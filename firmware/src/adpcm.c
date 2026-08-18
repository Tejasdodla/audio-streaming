/*
 * Copyright (c) 2026 5th Sense
 * IMA-ADPCM Audio Decoder Implementation
 */

#include "adpcm.h"

/* Standard IMA-ADPCM Step Table (89 entries) */
static const int16_t step_table[89] = {
	7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
	19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
	50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
	130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
	337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
	876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
	2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
	5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
	15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
};

/* Standard IMA-ADPCM Index Table for 4-bit nibbles */
static const int8_t index_table[16] = {
	-1, -1, -1, -1, 2, 4, 6, 8,
	-1, -1, -1, -1, 2, 4, 6, 8
};

static inline int16_t decode_sample(uint8_t nibble, int16_t *predictor, int8_t *step_index)
{
	int16_t step = step_table[*step_index];
	int32_t diff = step >> 3;

	if (nibble & 4) diff += step;
	if (nibble & 2) diff += (step >> 1);
	if (nibble & 1) diff += (step >> 2);

	int32_t pred = *predictor;
	if (nibble & 8) {
		pred -= diff;
	} else {
		pred += diff;
	}

	/* Clamp to 16-bit signed range */
	if (pred > 32767) pred = 32767;
	else if (pred < -32768) pred = -32768;

	*predictor = (int16_t)pred;

	/* Update step index */
	int8_t idx = *step_index + index_table[nibble & 0x0F];
	if (idx < 0) idx = 0;
	else if (idx > 88) idx = 88;
	*step_index = idx;

	return *predictor;
}

size_t adpcm_decode_frame(const uint8_t *adpcm_data, size_t adpcm_len,
                          int16_t init_predictor, uint8_t init_index,
                          int16_t *out_pcm)
{
	if (!adpcm_data || !out_pcm || adpcm_len == 0) {
		return 0;
	}

	int16_t predictor = init_predictor;
	int8_t step_index = (int8_t)(init_index > 88 ? 88 : init_index);
	size_t out_idx = 0;

	for (size_t i = 0; i < adpcm_len; i++) {
		uint8_t byte = adpcm_data[i];

		/* Lower nibble first */
		out_pcm[out_idx++] = decode_sample(byte & 0x0F, &predictor, &step_index);

		/* Upper nibble second */
		out_pcm[out_idx++] = decode_sample((byte >> 4) & 0x0F, &predictor, &step_index);
	}

	return out_idx;
}
