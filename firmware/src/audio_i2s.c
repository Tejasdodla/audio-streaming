/*
 * Copyright (c) 2026 5th Sense
 * Zephyr I2S Driver Implementation for MAX98357A DAC
 */

#include "audio_i2s.h"
#include "jitter_buffer.h"
#include "audio_dsp.h"
#include <zephyr/kernel.h>
#include <zephyr/drivers/i2s.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(audio_i2s, LOG_LEVEL_INF);

/* Memory slab for I2S DMA TX buffers (640 bytes per block, 8 blocks) */
K_MEM_SLAB_DEFINE_STATIC(i2s_tx_slab, I2S_BLOCK_SIZE_BYTES, I2S_NUM_BLOCKS, 4);

/* I2S Thread definitions */
#define I2S_THREAD_STACK_SIZE 2048
#define I2S_THREAD_PRIORITY   1 /* High priority audio thread */

static K_THREAD_STACK_DEFINE(i2s_thread_stack, I2S_THREAD_STACK_SIZE);
static struct k_thread i2s_thread_data;

static const struct device *g_i2s_dev = NULL;
static struct i2s_config g_i2s_cfg;
static bool g_is_running = false;
static bool g_dma_running = false;
static bool g_i2s_configured = false;

static void i2s_playback_thread(void *arg1, void *arg2, void *arg3);

int audio_i2s_init(void)
{
#if DT_NODE_EXISTS(DT_ALIAS(i2s_audio))
	g_i2s_dev = DEVICE_DT_GET(DT_ALIAS(i2s_audio));
#elif DT_NODE_EXISTS(DT_NODELABEL(i2s20))
	g_i2s_dev = DEVICE_DT_GET(DT_NODELABEL(i2s20));
#elif DT_NODE_EXISTS(DT_NODELABEL(i2s0))
	g_i2s_dev = DEVICE_DT_GET(DT_NODELABEL(i2s0));
#else
	LOG_ERR("No I2S device found in DeviceTree!");
	return -ENODEV;
#endif

	if (!g_i2s_dev || !device_is_ready(g_i2s_dev)) {
		LOG_ERR("I2S device %s is not ready!", g_i2s_dev ? g_i2s_dev->name : "NULL");
		return -ENODEV;
	}

	LOG_INF("I2S device %s found and ready", g_i2s_dev->name);

	/* Configure standard Stereo Philips I2S format (16kHz, 16-bit, 2 Channels for MAX98357A) */
	int ret = audio_i2s_configure(16000, 2, 16);
	if (ret < 0) {
		LOG_ERR("Failed to configure I2S default format: %d", ret);
		return ret;
	}

	/* Start dedicated I2S playback feeder thread */
	k_thread_create(&i2s_thread_data, i2s_thread_stack,
			K_THREAD_STACK_SIZEOF(i2s_thread_stack),
			i2s_playback_thread, NULL, NULL, NULL,
			I2S_THREAD_PRIORITY, 0, K_NO_WAIT);
	k_thread_name_set(&i2s_thread_data, "i2s_feeder");

	return 0;
}

int audio_i2s_configure(uint32_t sample_rate, uint8_t channels, uint8_t bit_depth)
{
	(void)channels;
	if (!g_i2s_dev) {
		return -ENODEV;
	}

	/* Force 2 channels (Stereo) for MAX98357A standard Philips clocking */
	uint8_t ch = 2;
	uint8_t ws = (bit_depth == 24) ? 24 : 16;

	if (g_i2s_configured && g_i2s_cfg.frame_clk_freq == sample_rate &&
	    g_i2s_cfg.channels == ch && g_i2s_cfg.word_size == ws) {
		return 0; /* Already configured to matching parameters */
	}

	if (g_dma_running) {
		i2s_trigger(g_i2s_dev, I2S_DIR_TX, I2S_TRIGGER_DROP);
		g_dma_running = false;
	}

	memset(&g_i2s_cfg, 0, sizeof(g_i2s_cfg));

	/* Standard Philips I2S format, Master mode */
	g_i2s_cfg.word_size = ws;
	g_i2s_cfg.channels = ch;
	g_i2s_cfg.format = I2S_FMT_DATA_FORMAT_I2S | I2S_FMT_CLK_NF_NB;
	g_i2s_cfg.options = I2S_OPT_FRAME_CLK_MASTER | I2S_OPT_BIT_CLK_MASTER;
	g_i2s_cfg.frame_clk_freq = sample_rate;
	g_i2s_cfg.mem_slab = &i2s_tx_slab;
	g_i2s_cfg.block_size = I2S_BLOCK_SIZE_BYTES;
	g_i2s_cfg.timeout = 1000;

	int ret = i2s_configure(g_i2s_dev, I2S_DIR_TX, &g_i2s_cfg);
	if (ret < 0) {
		LOG_ERR("i2s_configure failed: %d", ret);
		return ret;
	}

	g_i2s_configured = true;
	LOG_INF("I2S Configured: Rate=%u Hz, Ch=%u (Stereo MAX98357A), Bits=%u",
		sample_rate, ch, bit_depth);
	return 0;
}

int audio_i2s_start(void)
{
	if (!g_i2s_configured) {
		return -EINVAL;
	}

	g_is_running = true;
	LOG_INF("Audio I2S output enabled");
	return 0;
}

int audio_i2s_stop(void)
{
	g_is_running = false;
	LOG_INF("Audio I2S output stopped (streaming digital silence)");
	return 0;
}

bool audio_i2s_is_active(void)
{
	return g_is_running;
}

static void i2s_playback_thread(void *arg1, void *arg2, void *arg3)
{
	ARG_UNUSED(arg1);
	ARG_UNUSED(arg2);
	ARG_UNUSED(arg3);

	static int16_t s_mono_buf[160]; /* 10ms mono frame @ 16kHz */

	LOG_INF("I2S Feeder thread active");

	while (1) {
		/* Allocate a buffer from memory slab for I2S DMA */
		void *tx_buf = NULL;
		int ret = k_mem_slab_alloc(&i2s_tx_slab, &tx_buf, K_MSEC(100));
		if (ret != 0) {
			k_msleep(2);
			continue;
		}

		int16_t *stereo_out = (int16_t *)tx_buf;

		/* Check if we have audio to play */
		if (g_is_running && jitter_buffer_is_ready_to_play()) {
			/* Pop 10ms mono PCM frame (320 bytes = 160 samples) from jitter buffer */
			size_t read_bytes = jitter_buffer_pop((uint8_t *)s_mono_buf, 320);
			if (read_bytes < 320) {
				memset((uint8_t *)s_mono_buf + read_bytes, 0, 320 - read_bytes);
			}

			/* Duplicate Mono to Stereo (Left = s, Right = s) for MAX98357A */
			for (int i = 0; i < 160; i++) {
				int16_t s = s_mono_buf[i];
				stereo_out[2 * i]     = s;
				stereo_out[2 * i + 1] = s;
			}

			/* Apply digital volume DSP & soft-clipping before DAC */
			audio_dsp_process_pcm16(stereo_out, 320);
		} else {
			/* Stream silence when idle or buffering */
			memset(tx_buf, 0, I2S_BLOCK_SIZE_BYTES);

			if (!g_dma_running) {
				k_mem_slab_free(&i2s_tx_slab, tx_buf);
				k_msleep(10);
				continue;
			}
		}

		/* Send to I2S controller for DMA transmission */
		ret = i2s_write(g_i2s_dev, tx_buf, I2S_BLOCK_SIZE_BYTES);
		if (ret < 0) {
			LOG_WRN("i2s_write failed (%d), resetting DMA state", ret);
			k_mem_slab_free(&i2s_tx_slab, tx_buf);
			if (g_dma_running) {
				i2s_trigger(g_i2s_dev, I2S_DIR_TX, I2S_TRIGGER_DROP);
				g_dma_running = false;
			}
			k_msleep(5);
			continue;
		}

		/* If I2S DMA is not running, trigger START to begin clocking & DMA */
		if (!g_dma_running) {
			int tr_ret = i2s_trigger(g_i2s_dev, I2S_DIR_TX, I2S_TRIGGER_START);
			if (tr_ret == 0) {
				g_dma_running = true;
				LOG_INF("I2S DMA stream started");
			} else {
				LOG_WRN("i2s_trigger START returned %d", tr_ret);
			}
		}
	}
}
