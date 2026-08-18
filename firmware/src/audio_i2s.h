/*
 * Copyright (c) 2026 5th Sense
 * Zephyr I2S Driver wrapper for MAX98357A DAC
 */

#ifndef AUDIO_I2S_H_
#define AUDIO_I2S_H_

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/* 10.0ms @ 16kHz Stereo: 160 stereo pairs = 320 samples * 2 bytes = 640 bytes */
#define I2S_BLOCK_SIZE_BYTES    640
#define I2S_NUM_BLOCKS          8

int audio_i2s_init(void);
int audio_i2s_configure(uint32_t sample_rate, uint8_t channels, uint8_t bit_depth);
int audio_i2s_start(void);
int audio_i2s_stop(void);
bool audio_i2s_is_active(void);

#ifdef __cplusplus
}
#endif

#endif /* AUDIO_I2S_H_ */
