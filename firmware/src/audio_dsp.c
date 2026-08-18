/*
 * Copyright (c) 2026 5th Sense
 * Audio DSP Implementation
 */

#include "audio_dsp.h"
#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(audio_dsp, LOG_LEVEL_INF);

static uint8_t g_volume_percent = 100;
static int32_t g_volume_gain_q15 = 32768; /* 100% default unity gain */

void audio_dsp_init(void)
{
	audio_dsp_set_volume(100);
	LOG_INF("Audio DSP initialized. Default volume: %u%%", g_volume_percent);
}

void audio_dsp_set_volume(uint8_t volume_percent)
{
	if (volume_percent > 100) {
		volume_percent = 100;
	}
	g_volume_percent = volume_percent;

	/* Logarithmic perception curve mapping to Q15 gain */
	/* gain = (vol/100)^2 * 32768 */
	uint32_t vol_squared = (uint32_t)volume_percent * (uint32_t)volume_percent;
	g_volume_gain_q15 = (int32_t)((vol_squared * 32768U) / 10000U);
}

uint8_t audio_dsp_get_volume(void)
{
	return g_volume_percent;
}

void audio_dsp_process_pcm16(int16_t *samples, size_t num_samples)
{
	if (!samples || num_samples == 0) {
		return;
	}

	if (g_volume_percent == 100) {
		return;
	}

	if (g_volume_percent == 0) {
		memset(samples, 0, num_samples * sizeof(int16_t));
		return;
	}

	for (size_t i = 0; i < num_samples; i++) {
		/* Apply Q15 volume gain */
		int32_t sample = (int32_t)samples[i];
		int32_t scaled = (sample * g_volume_gain_q15) >> 15;

		/* Soft limiter / hard clamp to 16-bit range */
		if (scaled > 32767) {
			scaled = 32767;
		} else if (scaled < -32768) {
			scaled = -32768;
		}

		samples[i] = (int16_t)scaled;
	}
}

void audio_dsp_stereo_to_mono(const int16_t *stereo_in, int16_t *mono_out, size_t num_frames)
{
	if (!stereo_in || !mono_out || num_frames == 0) {
		return;
	}

	for (size_t i = 0; i < num_frames; i++) {
		int32_t left = stereo_in[i * 2];
		int32_t right = stereo_in[i * 2 + 1];
		int32_t mixed = (left + right) / 2;
		mono_out[i] = (int16_t)mixed;
	}
}
