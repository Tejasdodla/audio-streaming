/*
 * Copyright (c) 2026 5th Sense
 * Audio DSP: Volume Scaling, Soft Limiter, Mixing
 */

#ifndef AUDIO_DSP_H_
#define AUDIO_DSP_H_

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

void audio_dsp_init(void);
void audio_dsp_set_volume(uint8_t volume_percent);
uint8_t audio_dsp_get_volume(void);

/* Process 16-bit PCM samples in place: apply volume scaling and soft-clipping */
void audio_dsp_process_pcm16(int16_t *samples, size_t num_samples);

/* Downmix 16-bit stereo interleaved samples to mono in-place or out */
void audio_dsp_stereo_to_mono(const int16_t *stereo_in, int16_t *mono_out, size_t num_frames);

#ifdef __cplusplus
}
#endif

#endif /* AUDIO_DSP_H_ */
