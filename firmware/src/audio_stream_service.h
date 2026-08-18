/*
 * Copyright (c) 2026 5th Sense
 * BLE Custom Audio GATT Streaming Service
 */

#ifndef AUDIO_STREAM_SERVICE_H_
#define AUDIO_STREAM_SERVICE_H_

#include <stdint.h>
#include <stdbool.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/gatt.h>
#include "audio_protocol.h"

#ifdef __cplusplus
extern "C" {
#endif

int audio_stream_service_init(void);

/* Called when BLE connection state changes */
void audio_stream_service_on_connected(struct bt_conn *conn);
void audio_stream_service_on_disconnected(struct bt_conn *conn);

/* Push periodic statistics to connected client */
int audio_stream_service_send_stats(void);

#ifdef __cplusplus
}
#endif

#endif /* AUDIO_STREAM_SERVICE_H_ */
