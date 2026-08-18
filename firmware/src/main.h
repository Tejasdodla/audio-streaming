/*
 * Copyright (c) 2026 5th Sense
 * Main Board LED Indicators Interface
 */

#ifndef MAIN_H_
#define MAIN_H_

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

void main_set_conn_led(bool active);
void main_set_playback_led(bool active);

#ifdef __cplusplus
}
#endif

#endif /* MAIN_H_ */
