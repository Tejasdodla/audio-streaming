/*
 * Copyright (c) 2026 5th Sense
 * Audio Streaming Protocol Definitions
 */

#ifndef AUDIO_PROTOCOL_H_
#define AUDIO_PROTOCOL_H_

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* 128-bit UUIDs for 5th Sense Audio Service */
#define BT_UUID_AUDIO_SERVICE_VAL \
	BT_UUID_128_ENCODE(0x5f550001, 0x8b43, 0x4f1e, 0x9827, 0x00554c150000)
#define BT_UUID_AUDIO_DATA_CHAR_VAL \
	BT_UUID_128_ENCODE(0x5f550002, 0x8b43, 0x4f1e, 0x9827, 0x00554c150000)
#define BT_UUID_AUDIO_CTRL_CHAR_VAL \
	BT_UUID_128_ENCODE(0x5f550003, 0x8b43, 0x4f1e, 0x9827, 0x00554c150000)
#define BT_UUID_AUDIO_STATS_CHAR_VAL \
	BT_UUID_128_ENCODE(0x5f550004, 0x8b43, 0x4f1e, 0x9827, 0x00554c150000)

/* Protocol Opcodes */
#define AUDIO_OP_DATA               0x01
#define AUDIO_OP_CONTROL            0x02
#define AUDIO_OP_STATS_REQ          0x03

/* Control Commands */
#define AUDIO_CMD_START             0x10
#define AUDIO_CMD_STOP              0x11
#define AUDIO_CMD_PAUSE             0x12
#define AUDIO_CMD_RESUME            0x13
#define AUDIO_CMD_CONFIG            0x14
#define AUDIO_CMD_SET_VOLUME        0x15
#define AUDIO_CMD_PLAY_FLASH_ASSET  0x16
#define AUDIO_CMD_FLASH_WRITE_START 0x17
#define AUDIO_CMD_FLASH_WRITE_CHUNK 0x18
#define AUDIO_CMD_FLASH_WRITE_END   0x19

/* Audio Codec Types */
#define AUDIO_CODEC_PCM_16BIT       0x00
#define AUDIO_CODEC_ADPCM           0x01
#define AUDIO_CODEC_OPUS            0x02
#define AUDIO_CODEC_SBC             0x03
#define AUDIO_CODEC_LC3             0x04

/* Audio Stream Modes */
#define AUDIO_MODE_FILE             0x00
#define AUDIO_MODE_SYSTEM_SPEAKER   0x01
#define AUDIO_MODE_TTS              0x02

/* Default Audio Configuration */
#define AUDIO_DEFAULT_SAMPLE_RATE   16000
#define AUDIO_DEFAULT_CHANNELS      1
#define AUDIO_DEFAULT_BIT_DEPTH     16

#pragma pack(push, 1)

/* Audio Data Packet Header (6 bytes) */
struct audio_data_header {
	uint8_t opcode;       /* AUDIO_OP_DATA (0x01) */
	uint8_t seq_num;      /* Sequence number 0-255 */
	uint8_t codec;        /* AUDIO_CODEC_LC3 (0x04), ADPCM (0x01), PCM (0x00) */
	uint8_t init_index;   /* ADPCM step index (0-88) or LC3 frame length / flags */
	int16_t init_pred;    /* ADPCM initial predictor or LC3 reserved */
};

/* Audio Config Payload */
struct audio_config_payload {
	uint32_t sample_rate; /* e.g. 16000, 24000, 44100, 48000 */
	uint8_t channels;     /* 1 = Mono, 2 = Stereo */
	uint8_t bit_depth;    /* 16 bits */
	uint8_t codec;        /* AUDIO_CODEC_* */
	uint8_t stream_mode;  /* AUDIO_MODE_* */
};

/* Audio Stats Payload (Firmware -> App Notify) */
struct audio_stats_payload {
	uint8_t buffer_percent;  /* 0 - 100% */
	uint8_t volume;          /* 0 - 100% */
	uint16_t underrun_count; /* Number of buffer underruns */
	uint32_t packets_received;
	uint32_t bytes_played;
	uint8_t is_playing;
	uint8_t reserved[3];
};

#pragma pack(pop)

#ifdef __cplusplus
}
#endif

#endif /* AUDIO_PROTOCOL_H_ */
