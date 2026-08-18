/*
 * Copyright 2022-2026 Google LLC
 * Copyright 2026 5th Sense
 *
 * Low Complexity Communication Codec (LC3)
 * Bluetooth LE Audio Standard Codec Interface
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef LC3_H_
#define LC3_H_

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* PCM sample formats */
enum lc3_pcm_format {
	LC3_PCM_FORMAT_S16 = 0, /* 16-bit signed integer (int16_t) */
	LC3_PCM_FORMAT_S24 = 1, /* 24-bit signed integer in 32-bit (int32_t) */
	LC3_PCM_FORMAT_FLOAT = 2 /* 32-bit floating point (float) */
};

/* Opaque LC3 Decoder Handle */
typedef struct lc3_decoder *lc3_decoder_t;

/* Opaque LC3 Encoder Handle */
typedef struct lc3_encoder *lc3_encoder_t;

/**
 * @brief Get the number of samples in a frame for given frame duration and sample rate.
 *
 * @param dt_us Frame duration in microseconds (7500 or 10000)
 * @param sr_hz Sample rate in Hz (8000, 16000, 24000, 32000, 48000)
 * @return Number of samples per frame, or -1 on invalid parameter.
 */
int lc3_frame_samples(int dt_us, int sr_hz);

/**
 * @brief Get the required memory size in bytes for an LC3 decoder instance.
 *
 * @param dt_us Frame duration in microseconds (7500 or 10000)
 * @param sr_hz Sample rate in Hz (8000, 16000, 24000, 32000, 48000)
 * @return Decoder memory size in bytes, or 0 on error.
 */
unsigned lc3_decoder_size(int dt_us, int sr_hz);

/**
 * @brief Initialize and set up an LC3 decoder instance in caller-provided memory.
 *
 * @param dt_us Frame duration in microseconds (7500 or 10000)
 * @param sr_hz Sample rate in Hz (8000, 16000, 24000, 32000, 48000)
 * @param sr_pcm_hz Output PCM sample rate in Hz (0 = same as sr_hz)
 * @param mem Pointer to memory buffer of at least lc3_decoder_size() bytes
 * @return Decoder handle, or NULL on error.
 */
lc3_decoder_t lc3_setup_decoder(int dt_us, int sr_hz, int sr_pcm_hz, void *mem);

/**
 * @brief Decode an LC3 compressed frame to 16-bit linear PCM.
 *
 * If in == NULL or nbytes == 0, the decoder automatically executes Packet Loss
 * Concealment (PLC) to seamlessly extrapolate audio without pops, clicks, or dropouts.
 *
 * @param decoder Decoder handle
 * @param in Compressed input bytes (or NULL for PLC packet loss concealment)
 * @param nbytes Number of bytes in input frame (or 0 for PLC)
 * @param fmt Output PCM sample format (e.g. LC3_PCM_FORMAT_S16)
 * @param pcm Output buffer for decoded PCM samples
 * @param stride Sample stride (1 for contiguous mono)
 * @return 0 on success, or negative error code.
 */
int lc3_decode(lc3_decoder_t decoder, const void *in, int nbytes,
               enum lc3_pcm_format fmt, void *pcm, int stride);

/**
 * @brief Reset the internal state of an LC3 decoder instance.
 *
 * @param decoder Decoder handle
 */
void lc3_decoder_reset(lc3_decoder_t decoder);

#ifdef __cplusplus
}
#endif

#endif /* LC3_H_ */
