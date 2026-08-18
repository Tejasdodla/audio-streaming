/*
 * Copyright (c) 2026 5th Sense
 * IMA-ADPCM Audio Codec for Ultra-Low Latency BLE Streaming
 */

#ifndef ADPCM_H_
#define ADPCM_H_

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Decode IMA-ADPCM buffer into 16-bit linear PCM
 *
 * @param adpcm_data: Pointer to packed 4-bit ADPCM nibbles
 * @param adpcm_len: Length of adpcm_data in bytes
 * @param init_predictor: Initial 16-bit predictor sample from packet header
 * @param init_index: Initial step index (0-88) from packet header
 * @param out_pcm: Output buffer for 16-bit PCM samples (must hold adpcm_len * 2 samples)
 * @return Number of 16-bit PCM samples generated (adpcm_len * 2)
 */
size_t adpcm_decode_frame(const uint8_t *adpcm_data, size_t adpcm_len,
                          int16_t init_predictor, uint8_t init_index,
                          int16_t *out_pcm);

#ifdef __cplusplus
}
#endif

#endif /* ADPCM_H_ */
