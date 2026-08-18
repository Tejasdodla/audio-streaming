/*
 * Copyright (c) 2026 5th Sense
 * Onboard/External Flash Memory Storage Manager for nRF54L15
 */

#ifndef FLASH_STORAGE_H_
#define FLASH_STORAGE_H_

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define FLASH_AUDIO_SECTOR_SIZE   4096
#define FLASH_AUDIO_BASE_OFFSET   0x000000 /* Base offset in external flash */
#define FLASH_MAX_ASSET_SIZE      (256 * 1024) /* 256 KB max cached asset */

int flash_storage_init(void);
bool flash_storage_is_ready(void);

/* Write audio chunk to flash (offset relative to asset slot) */
int flash_storage_write_asset(uint8_t asset_id, uint32_t offset, const uint8_t *data, size_t len);

/* Read audio chunk from flash */
int flash_storage_read_asset(uint8_t asset_id, uint32_t offset, uint8_t *dest, size_t len);

/* Erase asset storage region */
int flash_storage_erase_asset(uint8_t asset_id, size_t total_size);

/* Stream an asset from flash directly into the jitter buffer for playback */
int flash_storage_play_asset(uint8_t asset_id);

#ifdef __cplusplus
}
#endif

#endif /* FLASH_STORAGE_H_ */
