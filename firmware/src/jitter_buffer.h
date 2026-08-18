/*
 * Copyright (c) 2026 5th Sense
 * Audio Jitter Buffer & Flow Control
 */

#ifndef JITTER_BUFFER_H_
#define JITTER_BUFFER_H_

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define JITTER_BUFFER_CAPACITY_BYTES   (16 * 1024) /* 16 KB ring buffer */
#define JITTER_BUFFER_PREBUFFER_BYTES  (960)       /* 960 bytes (2 ADPCM frames = 30ms) */

struct jitter_buffer_stats {
	size_t capacity;
	size_t bytes_available;
	size_t bytes_free;
	uint8_t fill_percent;
	uint32_t packets_received;
	uint32_t packets_dropped;
	uint16_t underrun_count;
	uint32_t total_bytes_written;
	uint32_t total_bytes_read;
	bool is_prebuffered;
};

void jitter_buffer_init(void);
void jitter_buffer_reset(void);

/* Push audio chunk from BLE thread (Returns actual bytes written) */
size_t jitter_buffer_push(const uint8_t *data, size_t len, uint8_t seq_num);

/* Pop audio chunk for I2S DMA thread. Returns bytes read. If empty, pads with silence and increments underrun */
size_t jitter_buffer_pop(uint8_t *dest, size_t requested_len);

/* Check if playback can begin (prebuffer satisfied) */
bool jitter_buffer_is_ready_to_play(void);

/* Retrieve buffer health metrics */
void jitter_buffer_get_stats(struct jitter_buffer_stats *out_stats);

#ifdef __cplusplus
}
#endif

#endif /* JITTER_BUFFER_H_ */
