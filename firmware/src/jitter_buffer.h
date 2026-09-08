/*
 * Copyright (c) 2026 5th Sense
 * Audio Jitter Buffer Definitions
 */

#ifndef JITTER_BUFFER_H_
#define JITTER_BUFFER_H_

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Jitter Buffer Capacity: 16 KB (~500ms of 16kHz mono audio) */
#define JITTER_BUFFER_CAPACITY_BYTES    16384
/* Prebuffer threshold: 640 bytes (2 frames = 20ms of 16kHz audio) */
#define JITTER_BUFFER_PREBUFFER_BYTES   640

struct jitter_buffer_stats {
	uint32_t capacity;
	uint32_t bytes_available;
	uint32_t bytes_free;
	uint32_t total_bytes_written;
	uint32_t total_bytes_read;
	uint32_t underrun_count;
	uint32_t packets_received;
	uint32_t packets_dropped;
	uint8_t  fill_percent;
	bool     is_prebuffered;
};

void jitter_buffer_init(void);
void jitter_buffer_reset(void);
size_t jitter_buffer_push(const uint8_t *data, size_t len, uint8_t seq_num);
size_t jitter_buffer_pop(uint8_t *dest, size_t requested_len);
bool jitter_buffer_is_ready_to_play(void);
void jitter_buffer_get_stats(struct jitter_buffer_stats *out_stats);

#ifdef __cplusplus
}
#endif

#endif /* JITTER_BUFFER_H_ */
