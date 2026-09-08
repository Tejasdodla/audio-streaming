/*
 * Copyright (c) 2026 5th Sense
 * Audio Jitter Buffer Implementation
 */

#include "jitter_buffer.h"
#include <zephyr/kernel.h>
#include <zephyr/sys/ring_buffer.h>
#include <zephyr/logging/log.h>
#include <string.h>

LOG_MODULE_REGISTER(jitter_buf, LOG_LEVEL_INF);

static uint8_t g_ring_buf_raw[JITTER_BUFFER_CAPACITY_BYTES];
static struct ring_buf g_ring_buf;
static K_MUTEX_DEFINE(g_buf_mutex);

static struct jitter_buffer_stats g_stats;
static uint8_t g_last_seq_num = 0;
static bool g_has_received_first_packet = false;

void jitter_buffer_init(void)
{
	k_mutex_lock(&g_buf_mutex, K_FOREVER);
	ring_buf_init(&g_ring_buf, sizeof(g_ring_buf_raw), g_ring_buf_raw);
	memset(&g_stats, 0, sizeof(g_stats));
	g_stats.capacity = JITTER_BUFFER_CAPACITY_BYTES;
	g_has_received_first_packet = false;
	k_mutex_unlock(&g_buf_mutex);

	LOG_INF("Jitter buffer initialized (Capacity: %d bytes)", JITTER_BUFFER_CAPACITY_BYTES);
}

void jitter_buffer_reset(void)
{
	k_mutex_lock(&g_buf_mutex, K_FOREVER);
	ring_buf_reset(&g_ring_buf);
	g_stats.bytes_available = 0;
	g_stats.bytes_free = JITTER_BUFFER_CAPACITY_BYTES;
	g_stats.fill_percent = 0;
	g_stats.is_prebuffered = false;
	g_has_received_first_packet = false;
	k_mutex_unlock(&g_buf_mutex);

	LOG_INF("Jitter buffer reset");
}

size_t jitter_buffer_push(const uint8_t *data, size_t len, uint8_t seq_num)
{
	if (!data || len == 0) {
		return 0;
	}

	k_mutex_lock(&g_buf_mutex, K_FOREVER);

	/* Check sequence continuity for packet loss detection */
	if (g_has_received_first_packet) {
		uint8_t expected = (g_last_seq_num + 1) & 0xFF;
		if (seq_num != expected) {
			uint8_t lost = (seq_num >= expected) ? (seq_num - expected) : (256 + seq_num - expected);
			g_stats.packets_dropped += lost;
			LOG_WRN("Packet loss detected! Expected seq %u, got %u (Lost: %u)",
				expected, seq_num, lost);
		}
	} else {
		g_has_received_first_packet = true;
	}
	g_last_seq_num = seq_num;
	g_stats.packets_received++;

	/* Write to ring buffer */
	uint32_t written = ring_buf_put(&g_ring_buf, data, len);
	if (written < len) {
		/* Buffer overflow / overrun */
		LOG_WRN("Jitter buffer overflow! Dropped %u bytes", (uint32_t)(len - written));
	}

	g_stats.total_bytes_written += written;
	g_stats.bytes_available = ring_buf_size_get(&g_ring_buf);
	g_stats.bytes_free = ring_buf_space_get(&g_ring_buf);
	g_stats.fill_percent = (uint8_t)((g_stats.bytes_available * 100) / g_stats.capacity);

	if (!g_stats.is_prebuffered && g_stats.bytes_available >= JITTER_BUFFER_PREBUFFER_BYTES) {
		g_stats.is_prebuffered = true;
		LOG_INF("Prebuffer threshold reached (%u bytes). Playback ready!", (uint32_t)g_stats.bytes_available);
	}

	k_mutex_unlock(&g_buf_mutex);
	return written;
}

size_t jitter_buffer_pop(uint8_t *dest, size_t requested_len)
{
	if (!dest || requested_len == 0) {
		return 0;
	}

	k_mutex_lock(&g_buf_mutex, K_FOREVER);

	uint32_t read_bytes = ring_buf_get(&g_ring_buf, dest, requested_len);

	if (read_bytes < requested_len) {
		/* Underrun condition - fill remaining with silence (0) */
		memset(dest + read_bytes, 0, requested_len - read_bytes);
		g_stats.underrun_count++;
		g_stats.is_prebuffered = false; /* Force re-buffer */
	}

	g_stats.total_bytes_read += read_bytes;
	g_stats.bytes_available = ring_buf_size_get(&g_ring_buf);
	g_stats.bytes_free = ring_buf_space_get(&g_ring_buf);
	g_stats.fill_percent = (uint8_t)((g_stats.bytes_available * 100) / g_stats.capacity);

	k_mutex_unlock(&g_buf_mutex);
	return requested_len;
}

bool jitter_buffer_is_ready_to_play(void)
{
	k_mutex_lock(&g_buf_mutex, K_FOREVER);
	bool ready = (g_stats.bytes_available >= 320);
	k_mutex_unlock(&g_buf_mutex);
	return ready;
}

void jitter_buffer_get_stats(struct jitter_buffer_stats *out_stats)
{
	if (!out_stats) {
		return;
	}
	k_mutex_lock(&g_buf_mutex, K_FOREVER);
	memcpy(out_stats, &g_stats, sizeof(struct jitter_buffer_stats));
	k_mutex_unlock(&g_buf_mutex);
}
