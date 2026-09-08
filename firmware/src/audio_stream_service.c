/*
 * Copyright (c) 2026 5th Sense
 * Bluetooth LE Audio Streaming GATT Service
 */

#include "audio_stream_service.h"
#include "audio_i2s.h"
#include "jitter_buffer.h"
#include "audio_dsp.h"
#include "flash_storage.h"
#include "main.h"
#include <zephyr/kernel.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/gatt.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/logging/log.h>
#include <string.h>

LOG_MODULE_REGISTER(audio_svc, LOG_LEVEL_INF);

/* Custom 128-bit UUIDs */
static struct bt_uuid_128 audio_svc_uuid = BT_UUID_INIT_128(BT_UUID_AUDIO_SERVICE_VAL);
static struct bt_uuid_128 audio_data_uuid = BT_UUID_INIT_128(BT_UUID_AUDIO_DATA_CHAR_VAL);
static struct bt_uuid_128 audio_ctrl_uuid = BT_UUID_INIT_128(BT_UUID_AUDIO_CTRL_CHAR_VAL);
static struct bt_uuid_128 audio_stats_uuid = BT_UUID_INIT_128(BT_UUID_AUDIO_STATS_CHAR_VAL);

static struct bt_conn *g_current_conn = NULL;
static bool g_stats_notify_enabled = false;

/* Forward declarations */
static ssize_t write_audio_data(struct bt_conn *conn, const struct bt_gatt_attr *attr,
				const void *buf, uint16_t len, uint16_t offset, uint8_t flags);
static ssize_t write_audio_ctrl(struct bt_conn *conn, const struct bt_gatt_attr *attr,
				const void *buf, uint16_t len, uint16_t offset, uint8_t flags);
static ssize_t read_audio_stats(struct bt_conn *conn, const struct bt_gatt_attr *attr,
				void *buf, uint16_t len, uint16_t offset);
static void stats_ccc_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value);

/* GATT Service Definition */
BT_GATT_SERVICE_DEFINE(audio_svc,
	BT_GATT_PRIMARY_SERVICE(&audio_svc_uuid),

	/* Audio Data Characteristic: Write Without Response */
	BT_GATT_CHARACTERISTIC(&audio_data_uuid.uuid,
			       BT_GATT_CHRC_WRITE_WITHOUT_RESP | BT_GATT_CHRC_WRITE,
			       BT_GATT_PERM_WRITE,
			       NULL, write_audio_data, NULL),

	/* Audio Control Characteristic: Write & Write Without Response */
	BT_GATT_CHARACTERISTIC(&audio_ctrl_uuid.uuid,
			       BT_GATT_CHRC_WRITE | BT_GATT_CHRC_WRITE_WITHOUT_RESP,
			       BT_GATT_PERM_WRITE,
			       NULL, write_audio_ctrl, NULL),

	/* Audio Stats Characteristic: Read & Notify */
	BT_GATT_CHARACTERISTIC(&audio_stats_uuid.uuid,
			       BT_GATT_CHRC_READ | BT_GATT_CHRC_NOTIFY,
			       BT_GATT_PERM_READ,
			       read_audio_stats, NULL, NULL),
	BT_GATT_CCC(stats_ccc_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),
);

#include "adpcm.h"
#include "lc3/lc3.h"

/* Static memory allocation for LC3 decoder instance (8KB aligned) */
static uint8_t g_lc3_dec_mem[8192] __aligned(4);
static lc3_decoder_t g_lc3_decoder = NULL;
static uint32_t g_current_sample_rate = 16000;
static uint8_t g_expected_seq_num = 0;
static bool g_seq_initialized = false;

static void ensure_lc3_decoder(uint32_t sample_rate)
{
	if (!g_lc3_decoder || g_current_sample_rate != sample_rate) {
		g_current_sample_rate = sample_rate;
		g_lc3_decoder = lc3_setup_decoder(10000, sample_rate, 0, g_lc3_dec_mem);
		LOG_INF("LC3 Decoder initialized: %u Hz (10.0ms frames, %d samples)",
			sample_rate, lc3_frame_samples(10000, sample_rate));
	}
}

/* Callback when client writes audio PCM/LC3 data chunks */
static ssize_t write_audio_data(struct bt_conn *conn, const struct bt_gatt_attr *attr,
				const void *buf, uint16_t len, uint16_t offset, uint8_t flags)
{
	ARG_UNUSED(conn);
	ARG_UNUSED(attr);
	ARG_UNUSED(offset);
	ARG_UNUSED(flags);

	if (len < sizeof(struct audio_data_header)) {
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	struct audio_data_header hdr;
	memcpy(&hdr, buf, sizeof(hdr));

	if (hdr.opcode != AUDIO_OP_DATA) {
		return BT_GATT_ERR(BT_ATT_ERR_VALUE_NOT_ALLOWED);
	}

	const uint8_t *payload = (const uint8_t *)buf + sizeof(struct audio_data_header);
	size_t payload_len = len - sizeof(struct audio_data_header);

	if (payload_len > 0) {
		if (hdr.codec == AUDIO_CODEC_LC3) {
			ensure_lc3_decoder(g_current_sample_rate);

			int samples_per_frame = lc3_frame_samples(10000, g_current_sample_rate);
			if (samples_per_frame > 0 && samples_per_frame <= 480 && g_lc3_decoder) {
				static int16_t s_decoded_pcm[480];

				/* 1. Packet Loss Concealment (PLC) for missed frames */
				if (g_seq_initialized) {
					uint8_t lost = (hdr.seq_num >= g_expected_seq_num) ?
						(hdr.seq_num - g_expected_seq_num) :
						(256 + hdr.seq_num - g_expected_seq_num);

					if (lost > 0 && lost <= 4) {
						LOG_WRN("LC3 Packet loss (%u frames)! Running PLC...", lost);
						for (uint8_t p = 0; p < lost; p++) {
							lc3_decode(g_lc3_decoder, NULL, 0,
								   LC3_PCM_FORMAT_S16, s_decoded_pcm, 1);
							jitter_buffer_push((const uint8_t *)s_decoded_pcm,
									   samples_per_frame * sizeof(int16_t),
									   (g_expected_seq_num + p) & 0xFF);
						}
					}
				}

				/* 2. Decode Current LC3 Frame */
				int ret = lc3_decode(g_lc3_decoder, payload, payload_len,
						     LC3_PCM_FORMAT_S16, s_decoded_pcm, 1);
				if (ret == 0) {
					jitter_buffer_push((const uint8_t *)s_decoded_pcm,
							   samples_per_frame * sizeof(int16_t),
							   hdr.seq_num);
				} else {
					LOG_WRN("lc3_decode failed: %d", ret);
				}

				g_expected_seq_num = (hdr.seq_num + 1) & 0xFF;
				g_seq_initialized = true;
			}
		} else if (hdr.codec == AUDIO_CODEC_ADPCM) {
			if (payload_len >= 3) {
				int16_t init_pred = (int16_t)((uint16_t)payload[0] | ((uint16_t)payload[1] << 8));
				int8_t init_idx = (int8_t)payload[2];
				const uint8_t *adpcm_stream = payload + 3;
				size_t adpcm_len = payload_len - 3;

				static struct ima_adpcm_state s_adpcm_state;
				ima_adpcm_init_state(&s_adpcm_state, init_pred, init_idx);

				static int16_t s_pcm_buf[512];
				size_t num_samples = adpcm_len * 2;
				if (num_samples > 512) num_samples = 512;

				ima_adpcm_decode_block(&s_adpcm_state, adpcm_stream, adpcm_len, s_pcm_buf);
				jitter_buffer_push((const uint8_t *)s_pcm_buf, num_samples * sizeof(int16_t), hdr.seq_num);
			}
		} else {
			/* Linear PCM */
			jitter_buffer_push(payload, payload_len, hdr.seq_num);
		}
	}

	return len;
}

/* Callback when client writes to control characteristic */
static ssize_t write_audio_ctrl(struct bt_conn *conn, const struct bt_gatt_attr *attr,
				const void *buf, uint16_t len, uint16_t offset, uint8_t flags)
{
	ARG_UNUSED(conn);
	ARG_UNUSED(attr);
	ARG_UNUSED(offset);
	ARG_UNUSED(flags);

	if (len < 1) {
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	const uint8_t *data = (const uint8_t *)buf;
	uint8_t cmd = data[0];

	LOG_INF("Audio Control Command received: 0x%02X (len=%u)", cmd, len);

	switch (cmd) {
	case AUDIO_CMD_START:
		jitter_buffer_reset();
		g_seq_initialized = false;
		g_expected_seq_num = 0;
		if (g_lc3_decoder) lc3_decoder_reset(g_lc3_decoder);
		audio_i2s_start();
		main_set_playback_led(true);
		LOG_INF("Playback started by client");
		break;

	case AUDIO_CMD_STOP:
		audio_i2s_stop();
		jitter_buffer_reset();
		g_seq_initialized = false;
		g_expected_seq_num = 0;
		if (g_lc3_decoder) lc3_decoder_reset(g_lc3_decoder);
		main_set_playback_led(false);
		LOG_INF("Playback stopped by client");
		break;

	case AUDIO_CMD_PAUSE:
		audio_i2s_stop();
		main_set_playback_led(false);
		LOG_INF("Playback paused by client");
		break;

	case AUDIO_CMD_RESUME:
		audio_i2s_start();
		main_set_playback_led(true);
		LOG_INF("Playback resumed by client");
		break;

	case AUDIO_CMD_CONFIG:
		if (len >= 1 + sizeof(struct audio_config_payload)) {
			struct audio_config_payload cfg;
			memcpy(&cfg, data + 1, sizeof(cfg));
			LOG_INF("Config received: Rate=%u, Ch=%u, Bits=%u, Codec=%u, Mode=%u",
				cfg.sample_rate, cfg.channels, cfg.bit_depth, cfg.codec, cfg.stream_mode);
			g_current_sample_rate = cfg.sample_rate;
			ensure_lc3_decoder(cfg.sample_rate);
			audio_i2s_configure(cfg.sample_rate, cfg.channels, cfg.bit_depth);
			jitter_buffer_reset();
			g_seq_initialized = false;
			g_expected_seq_num = 0;
			audio_i2s_start();
		}
		break;

	case AUDIO_CMD_SET_VOLUME:
		if (len >= 2) {
			uint8_t vol = data[1];
			audio_dsp_set_volume(vol);
			LOG_INF("Volume set to %u%%", vol);
		}
		break;

	case AUDIO_CMD_PLAY_FLASH_ASSET:
		if (len >= 2) {
			uint8_t asset_id = data[1];
			flash_storage_play_asset(asset_id);
			audio_i2s_start();
		}
		break;

	default:
		LOG_WRN("Unknown control command: 0x%02X", cmd);
		break;
	}

	return len;
}

/* Callback when client reads stats characteristic */
static ssize_t read_audio_stats(struct bt_conn *conn, const struct bt_gatt_attr *attr,
				void *buf, uint16_t len, uint16_t offset)
{
	ARG_UNUSED(conn);

	struct jitter_buffer_stats j_stats;
	jitter_buffer_get_stats(&j_stats);

	struct audio_stats_payload payload = {
		.buffer_percent = j_stats.fill_percent,
		.volume = audio_dsp_get_volume(),
		.underrun_count = j_stats.underrun_count,
		.packets_received = j_stats.packets_received,
		.bytes_played = j_stats.total_bytes_read,
		.is_playing = audio_i2s_is_active() ? 1 : 0
	};

	return bt_gatt_attr_read(conn, attr, buf, len, offset, &payload, sizeof(payload));
}

static void stats_ccc_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value)
{
	ARG_UNUSED(attr);
	g_stats_notify_enabled = (value == BT_GATT_CCC_NOTIFY);
	LOG_INF("Stats notification %s", g_stats_notify_enabled ? "enabled" : "disabled");
}

int audio_stream_service_init(void)
{
	ensure_lc3_decoder(16000);
	LOG_INF("Audio Stream GATT Service registered");
	return 0;
}

void audio_stream_service_on_connected(struct bt_conn *conn)
{
	if (g_current_conn) {
		bt_conn_unref(g_current_conn);
		g_current_conn = NULL;
	}
	g_current_conn = bt_conn_ref(conn);
	g_stats_notify_enabled = false;
	g_seq_initialized = false;
	g_expected_seq_num = 0;
	jitter_buffer_reset();
	if (g_lc3_decoder) {
		lc3_decoder_reset(g_lc3_decoder);
	}
	main_set_playback_led(false);
	LOG_INF("Client connected to Audio Service (Session Cleaned)");
}

void audio_stream_service_on_disconnected(struct bt_conn *conn)
{
	ARG_UNUSED(conn);
	if (g_current_conn) {
		bt_conn_unref(g_current_conn);
		g_current_conn = NULL;
	}
	g_stats_notify_enabled = false;
	g_seq_initialized = false;
	g_expected_seq_num = 0;
	audio_i2s_stop();
	jitter_buffer_reset();
	if (g_lc3_decoder) {
		lc3_decoder_reset(g_lc3_decoder);
	}
	main_set_playback_led(false);
	LOG_INF("Client disconnected from Audio Service (Session Reset)");
}

int audio_stream_service_send_stats(void)
{
	if (!g_current_conn || !g_stats_notify_enabled) {
		return 0;
	}

	struct jitter_buffer_stats j_stats;
	jitter_buffer_get_stats(&j_stats);

	struct audio_stats_payload payload = {
		.buffer_percent = j_stats.fill_percent,
		.volume = audio_dsp_get_volume(),
		.underrun_count = j_stats.underrun_count,
		.packets_received = j_stats.packets_received,
		.bytes_played = j_stats.total_bytes_read,
		.is_playing = audio_i2s_is_active() ? 1 : 0
	};

	return bt_gatt_notify_uuid(g_current_conn, &audio_stats_uuid.uuid, audio_svc.attrs, &payload, sizeof(payload));
}
