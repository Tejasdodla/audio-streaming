#
# Copyright (c) 2026 5th Sense
# SPDX-License-Identifier: Apache-2.0
#

cmake_minimum_required(VERSION 3.20.0)

find_package(Zephyr REQUIRED HINTS $ENV{ZEPHYR_BASE})
project(nrf54l15_audio_receiver)

target_sources(app PRIVATE
    src/main.c
    src/audio_i2s.c
    src/audio_stream_service.c
    src/jitter_buffer.c
    src/flash_storage.c
    src/audio_dsp.c
    src/tone_synth.c
    src/adpcm.c
    src/lc3/lc3_dec.c
    src/lc3/lc3_tables.c
)

target_include_directories(app PRIVATE src src/lc3)
