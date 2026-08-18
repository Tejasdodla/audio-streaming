/*
 * Copyright (c) 2026 5th Sense
 * On-device Tone & Melody Synthesizer Implementation
 */

#include "tone_synth.h"
#include "audio_i2s.h"
#include "jitter_buffer.h"
#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>
#include <math.h>

LOG_MODULE_REGISTER(tone_synth, LOG_LEVEL_INF);

#define SAMPLE_RATE 16000
#define PI 3.14159265358979323846f
#define CHUNK_SAMPLES 256
#define CHUNK_BYTES (CHUNK_SAMPLES * sizeof(int16_t))

static uint8_t g_tune_index = 0;
static uint8_t g_seq_num = 0;
static K_SEM_DEFINE(g_tune_sem, 0, 1);
static int g_requested_tune = -1;

#define TONE_THREAD_STACK_SIZE 2048
#define TONE_THREAD_PRIORITY   5

static K_THREAD_STACK_DEFINE(tone_thread_stack, TONE_THREAD_STACK_SIZE);
static struct k_thread tone_thread_data;

struct note_entry {
	uint16_t freq_hz;
	uint16_t duration_ms;
};

/* 1. Startup Melody: Rising 4-note Chord Arpeggio (C5 -> E5 -> G5 -> C6) */
static const struct note_entry startup_tune[] = {
	{ NOTE_C5, 90 },
	{ NOTE_E5, 90 },
	{ NOTE_G5, 90 },
	{ NOTE_C6, 250 },
	{ NOTE_REST, 30 }
};

/* 2. Button 2 Melody A: Mario Coin Chime (B5 -> E6) */
static const struct note_entry coin_tune[] = {
	{ NOTE_B5, 80 },
	{ NOTE_E6, 320 },
	{ NOTE_REST, 50 }
};

/* 3. Button 2 Melody B: Victory Fanfare (C5 -> G5 -> C6 -> E6 -> G6) */
static const struct note_entry victory_tune[] = {
	{ NOTE_C5, 80 },
	{ NOTE_G5, 80 },
	{ NOTE_C6, 80 },
	{ NOTE_E6, 100 },
	{ NOTE_G6, 300 },
	{ NOTE_REST, 50 }
};

/* 4. Button 2 Melody C: Sci-Fi Alert Chime */
static const struct note_entry alert_tune[] = {
	{ NOTE_A5, 90 },
	{ NOTE_D6, 90 },
	{ NOTE_A5, 90 },
	{ NOTE_D6, 250 },
	{ NOTE_REST, 50 }
};

/* 5. Button 2 Melody D: Pure 1000Hz Diagnostic Sine Tone (300ms) */
static const struct note_entry test_sine_tune[] = {
	{ 1000, 300 },
	{ NOTE_REST, 50 }
};

/* Synthesize a single note with smooth attack/decay envelope */
static void synthesize_and_push_note(uint16_t freq_hz, uint16_t duration_ms)
{
	size_t total_samples = (SAMPLE_RATE * duration_ms) / 1000;
	int16_t chunk_buf[CHUNK_SAMPLES];
	float phase = 0.0f;
	float phase_inc = (2.0f * PI * freq_hz) / SAMPLE_RATE;
	float max_amplitude = 18000.0f; /* ~55% full scale volume */

	size_t sample_idx = 0;
	while (sample_idx < total_samples) {
		size_t to_gen = (total_samples - sample_idx > CHUNK_SAMPLES) ? 
		                CHUNK_SAMPLES : (total_samples - sample_idx);

		for (size_t i = 0; i < to_gen; i++) {
			size_t cur = sample_idx + i;
			if (freq_hz == NOTE_REST) {
				chunk_buf[i] = 0;
			} else {
				/* Apply smooth trapezoidal / cosine envelope to prevent clicks */
				float env = 1.0f;
				size_t attack = (SAMPLE_RATE * 8) / 1000; /* 8ms attack */
				size_t decay = (SAMPLE_RATE * 15) / 1000; /* 15ms decay */

				if (cur < attack) {
					env = (float)cur / (float)attack;
				} else if (cur > total_samples - decay) {
					env = (float)(total_samples - cur) / (float)decay;
				}

				chunk_buf[i] = (int16_t)(sinf(phase) * max_amplitude * env);
				phase += phase_inc;
				if (phase >= 2.0f * PI) {
					phase -= 2.0f * PI;
				}
			}
		}

		/* Zero-pad remainder if last partial chunk */
		for (size_t i = to_gen; i < CHUNK_SAMPLES; i++) {
			chunk_buf[i] = 0;
		}

		/* Push to jitter buffer */
		jitter_buffer_push((const uint8_t *)chunk_buf, CHUNK_BYTES, g_seq_num++);
		sample_idx += to_gen;
	}
}

static void play_notes_sequence(const struct note_entry *notes, size_t count)
{
	/* Reset buffer and ensure I2S is active */
	jitter_buffer_reset();
	audio_i2s_start();

	for (size_t i = 0; i < count; i++) {
		synthesize_and_push_note(notes[i].freq_hz, notes[i].duration_ms);
	}
}

static void tone_synth_thread(void *arg1, void *arg2, void *arg3)
{
	ARG_UNUSED(arg1);
	ARG_UNUSED(arg2);
	ARG_UNUSED(arg3);

	LOG_INF("Tone synth worker thread ready");

	while (1) {
		k_sem_take(&g_tune_sem, K_FOREVER);

		int tune = g_requested_tune;
		if (tune == 0) {
			LOG_INF("Playing Startup Boot Tune on MAX98357A I2S...");
			play_notes_sequence(startup_tune, ARRAY_SIZE(startup_tune));
		} else if (tune == 1) {
			uint8_t mode = g_tune_index % 4;
			g_tune_index++;
			switch (mode) {
			case 0:
				LOG_INF("[Button 2] Playing Mario Coin Chime (B5->E6)...");
				play_notes_sequence(coin_tune, ARRAY_SIZE(coin_tune));
				break;
			case 1:
				LOG_INF("[Button 2] Playing Victory Fanfare (C5->G6)...");
				play_notes_sequence(victory_tune, ARRAY_SIZE(victory_tune));
				break;
			case 2:
				LOG_INF("[Button 2] Playing Sci-Fi Alert Chime...");
				play_notes_sequence(alert_tune, ARRAY_SIZE(alert_tune));
				break;
			case 3:
				LOG_INF("[Button 2] Playing 1000 Hz Pure Test Sine Tone...");
				play_notes_sequence(test_sine_tune, ARRAY_SIZE(test_sine_tune));
				break;
			}
		}
	}
}

void tone_synth_init(void)
{
	k_thread_create(&tone_thread_data, tone_thread_stack,
			K_THREAD_STACK_SIZEOF(tone_thread_stack),
			tone_synth_thread, NULL, NULL, NULL,
			TONE_THREAD_PRIORITY, 0, K_NO_WAIT);
	k_thread_name_set(&tone_thread_data, "tone_synth");
	LOG_INF("Tone synthesizer initialized");
}

void tone_synth_play_startup_tune(void)
{
	g_requested_tune = 0;
	k_sem_give(&g_tune_sem);
}

void tone_synth_play_button2_tune(void)
{
	g_requested_tune = 1;
	k_sem_give(&g_tune_sem);
}

void tone_synth_play_test_beep(uint16_t freq_hz, uint16_t duration_ms)
{
	struct note_entry single_note = { freq_hz, duration_ms };
	play_notes_sequence(&single_note, 1);
}
