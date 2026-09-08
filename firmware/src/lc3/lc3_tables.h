/* LC3 Constants & Flash Tables */
#ifndef LC3_TABLES_H_
#define LC3_TABLES_H_

#include <stdint.h>

extern const uint16_t lc3_sns_band_offsets_16k[17];
extern const uint16_t lc3_sns_band_offsets_24k[17];
extern const uint16_t lc3_sns_band_offsets_48k[17];
extern const float lc3_sine_window_16k[320];
extern const float lc3_cos_lut_1280[1280];

#endif
