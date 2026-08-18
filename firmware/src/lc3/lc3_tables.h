/*
 * Copyright 2022-2026 Google LLC
 * Copyright 2026 5th Sense
 *
 * LC3 Tables & Lookup Constants
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef LC3_TABLES_H_
#define LC3_TABLES_H_

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* MDCT Window Tables */
extern const float lc3_w_10ms_16k[320];
extern const float lc3_w_10ms_24k[480];
extern const float lc3_w_10ms_48k[960];

/* Spectral Noise Shaping (SNS) Frequency Bands */
extern const uint8_t lc3_sns_band_offsets_16k[17];
extern const uint8_t lc3_sns_band_offsets_24k[17];
extern const uint8_t lc3_sns_band_offsets_48k[17];

/* DCT-IV Twiddle Factors & Sines */
extern const float lc3_dct4_twiddle_160[160];
extern const float lc3_dct4_twiddle_240[240];
extern const float lc3_dct4_twiddle_480[480];

#ifdef __cplusplus
}
#endif

#endif /* LC3_TABLES_H_ */
