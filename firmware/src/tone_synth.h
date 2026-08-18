/*
 * Copyright (c) 2026 5th Sense
 * On-device Tone & Melody Synthesizer for I2S Diagnostic Testing
 */

#ifndef TONE_SYNTH_H_
#define TONE_SYNTH_H_

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Standard musical note frequencies in Hz */
#define NOTE_C4   262
#define NOTE_D4   294
#define NOTE_E4   330
#define NOTE_F4   349
#define NOTE_G4   392
#define NOTE_A4   440
#define NOTE_B4   494
#define NOTE_C5   523
#define NOTE_D5   587
#define NOTE_E5   659
#define NOTE_F5   698
#define NOTE_G5   784
#define NOTE_A5   880
#define NOTE_B5   988
#define NOTE_C6   1046
#define NOTE_D6   1175
#define NOTE_E6   1319
#define NOTE_G6   1568
#define NOTE_C7   2093
#define NOTE_REST 0

/* Play predefined diagnostic tunes */
void tone_synth_init(void);
void tone_synth_play_startup_tune(void);
void tone_synth_play_button2_tune(void);
void tone_synth_play_test_beep(uint16_t freq_hz, uint16_t duration_ms);

#ifdef __cplusplus
}
#endif

#endif /* TONE_SYNTH_H_ */
