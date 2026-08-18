/*
 * Copyright (c) 2026 5th Sense
 * External NOR Flash (QSPI/SPI) Storage Driver for nRF54L15
 */

#include "flash_storage.h"
#include "jitter_buffer.h"
#include <zephyr/kernel.h>
#include <zephyr/drivers/flash.h>
#include <zephyr/storage/flash_map.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(flash_storage, LOG_LEVEL_INF);

static const struct device *g_flash_dev = NULL;
static bool g_flash_ready = false;

/* Structure stored at beginning of an asset region */
struct asset_header {
	uint32_t magic;       /* 0x534E5335 "5SNS" */
	uint32_t sample_rate;
	uint32_t size_bytes;
	uint8_t channels;
	uint8_t bit_depth;
	uint8_t reserved[6];
};

#define ASSET_MAGIC 0x534E5335

static uint32_t get_asset_base_addr(uint8_t asset_id)
{
	/* Divide flash area into 256KB slots */
	return FLASH_AUDIO_BASE_OFFSET + ((uint32_t)asset_id * FLASH_MAX_ASSET_SIZE);
}

int flash_storage_init(void)
{
#if DT_NODE_EXISTS(DT_ALIAS(audio_flash))
	g_flash_dev = DEVICE_DT_GET(DT_ALIAS(audio_flash));
#elif DT_NODE_EXISTS(DT_NODELABEL(mx25r6435f))
	g_flash_dev = DEVICE_DT_GET(DT_NODELABEL(mx25r6435f));
#elif DT_NODE_EXISTS(DT_NODELABEL(rram_controller))
	g_flash_dev = DEVICE_DT_GET(DT_NODELABEL(rram_controller));
#elif DT_NODE_EXISTS(DT_CHOSEN(zephyr_flash))
	g_flash_dev = DEVICE_DT_GET_OR_NULL(DT_CHOSEN(zephyr_flash));
#else
	g_flash_dev = NULL;
#endif

	if (!g_flash_dev || !device_is_ready(g_flash_dev)) {
		LOG_WRN("External/Internal flash storage not ready or not found. Flash caching disabled.");
		g_flash_ready = false;
		return -ENODEV;
	}

	g_flash_ready = true;
	LOG_INF("Flash storage initialized: %s", g_flash_dev->name);
	return 0;
}

bool flash_storage_is_ready(void)
{
	return g_flash_ready;
}

int flash_storage_erase_asset(uint8_t asset_id, size_t total_size)
{
	if (!g_flash_ready) {
		return -ENODEV;
	}

	uint32_t addr = get_asset_base_addr(asset_id);
	size_t erase_size = (total_size + FLASH_AUDIO_SECTOR_SIZE - 1) & ~(FLASH_AUDIO_SECTOR_SIZE - 1);
	if (erase_size > FLASH_MAX_ASSET_SIZE) {
		erase_size = FLASH_MAX_ASSET_SIZE;
	}

	LOG_INF("Erasing flash asset %u at 0x%08X (size %u bytes)", asset_id, addr, (uint32_t)erase_size);
	int ret = flash_erase(g_flash_dev, addr, erase_size);
	if (ret != 0) {
		LOG_ERR("flash_erase failed: %d", ret);
	}
	return ret;
}

int flash_storage_write_asset(uint8_t asset_id, uint32_t offset, const uint8_t *data, size_t len)
{
	if (!g_flash_ready) {
		return -ENODEV;
	}

	uint32_t addr = get_asset_base_addr(asset_id) + sizeof(struct asset_header) + offset;
	int ret = flash_write(g_flash_dev, addr, data, len);
	if (ret != 0) {
		LOG_ERR("flash_write failed at 0x%08X: %d", addr, ret);
	}
	return ret;
}

int flash_storage_read_asset(uint8_t asset_id, uint32_t offset, uint8_t *dest, size_t len)
{
	if (!g_flash_ready) {
		return -ENODEV;
	}

	uint32_t addr = get_asset_base_addr(asset_id) + sizeof(struct asset_header) + offset;
	return flash_read(g_flash_dev, addr, dest, len);
}

int flash_storage_play_asset(uint8_t asset_id)
{
	if (!g_flash_ready) {
		return -ENODEV;
	}

	uint32_t base_addr = get_asset_base_addr(asset_id);
	struct asset_header hdr;
	int ret = flash_read(g_flash_dev, base_addr, &hdr, sizeof(hdr));
	if (ret != 0 || hdr.magic != ASSET_MAGIC) {
		LOG_WRN("Asset %u not valid or empty (magic=0x%08X)", asset_id, hdr.magic);
		return -ENOENT;
	}

	LOG_INF("Playing flash asset %u (size=%u bytes, rate=%u Hz)",
		asset_id, hdr.size_bytes, hdr.sample_rate);

	jitter_buffer_reset();

	/* Read in chunks of 512 bytes and push to jitter buffer */
	uint8_t chunk_buf[512];
	uint32_t bytes_left = hdr.size_bytes;
	uint32_t offset = 0;
	uint8_t seq = 0;

	while (bytes_left > 0) {
		size_t to_read = (bytes_left > sizeof(chunk_buf)) ? sizeof(chunk_buf) : bytes_left;
		ret = flash_storage_read_asset(asset_id, offset, chunk_buf, to_read);
		if (ret != 0) {
			break;
		}

		jitter_buffer_push(chunk_buf, to_read, seq++);
		offset += to_read;
		bytes_left -= to_read;
	}

	return 0;
}
