/*
 * Copyright (c) 2026 5th Sense
 * nRF54L15 Audio Receiver Main Application
 */

#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/bluetooth/gatt.h>
#include <zephyr/drivers/gpio.h>

#include "audio_protocol.h"
#include "audio_dsp.h"
#include "jitter_buffer.h"
#include "audio_i2s.h"
#include "flash_storage.h"
#include "audio_stream_service.h"
#include "tone_synth.h"

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);

/* LED Status Indicator aliases if present on board */
#define LED0_NODE DT_ALIAS(led0)
#define LED1_NODE DT_ALIAS(led1)

#if DT_NODE_HAS_STATUS(LED0_NODE, okay)
static const struct gpio_dt_spec led_conn = GPIO_DT_SPEC_GET(LED0_NODE, gpios);
#endif
#if DT_NODE_HAS_STATUS(LED1_NODE, okay)
static const struct gpio_dt_spec led_play = GPIO_DT_SPEC_GET(LED1_NODE, gpios);
#endif

void main_set_conn_led(bool active)
{
#if DT_NODE_HAS_STATUS(LED0_NODE, okay)
	if (device_is_ready(led_conn.port)) {
		gpio_pin_set_dt(&led_conn, active ? 1 : 0);
	}
#endif
}

void main_set_playback_led(bool active)
{
#if DT_NODE_HAS_STATUS(LED1_NODE, okay)
	if (device_is_ready(led_play.port)) {
		gpio_pin_set_dt(&led_play, active ? 1 : 0);
	}
#endif
}

/* Physical Hardware Buttons from Schematic:
 * SW2 (Button0) = P1.13 (Connected to MAX98357A I2S DIN)
 * SW3 (Button1) = P1.09
 * SW5 (Button2) = P1.08
 * SW6 (Button3) = P0.04
 */
static const struct device *g_gpio0_dev = NULL;
static const struct device *g_gpio1_dev = NULL;
static struct gpio_callback g_btn_cb_gpio1;
static struct gpio_callback g_btn_cb_gpio0;

static int64_t last_button_time = 0;

static void trigger_diagnostic_tune(const char *btn_name)
{
	int64_t now = k_uptime_get();
	if (now - last_button_time < 400) {
		return; /* Debounce 400ms */
	}
	last_button_time = now;

	LOG_INF("[%s Pressed] Playing Diagnostic Melody...", btn_name);
	tone_synth_play_button2_tune();
}

static void button_pressed_cb(const struct device *dev, struct gpio_callback *cb, uint32_t pins)
{
	ARG_UNUSED(cb);

	if (dev == g_gpio1_dev) {
		if (pins & BIT(9)) {
			trigger_diagnostic_tune("SW3 (Button1 / P1.09)");
		} else if (pins & BIT(8)) {
			trigger_diagnostic_tune("SW5 (Button2 / P1.08)");
		}
	} else if (dev == g_gpio0_dev) {
		if (pins & BIT(4)) {
			trigger_diagnostic_tune("SW6 (Button3 / P0.04)");
		}
	}
}

/* BLE Advertising Data */
static const struct bt_data ad[] = {
	BT_DATA_BYTES(BT_DATA_FLAGS, (BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR)),
	BT_DATA(BT_DATA_NAME_COMPLETE, CONFIG_BT_DEVICE_NAME, sizeof(CONFIG_BT_DEVICE_NAME) - 1),
};

static const struct bt_data sd[] = {
	BT_DATA_BYTES(BT_DATA_UUID128_ALL, BT_UUID_AUDIO_SERVICE_VAL),
	BT_DATA_BYTES(BT_DATA_GAP_APPEARANCE, 0x10, 0x08), /* Generic Media Player / Audio Sink */
};

/* Fast connection parameters: 15ms interval, 0 latency, 4000ms supervision timeout */
static const struct bt_le_conn_param conn_param_fast = {
	.interval_min = BT_GAP_MS_TO_CONN_INTERVAL(15),
	.interval_max = BT_GAP_MS_TO_CONN_INTERVAL(20),
	.latency = 0,
	.timeout = 400,
};

static bool g_is_connected = false;

static void start_advertising(void)
{
	int ret = bt_le_adv_start(BT_LE_ADV_CONN, ad, ARRAY_SIZE(ad), sd, ARRAY_SIZE(sd));
	if (ret && ret != -EALREADY) {
		bt_le_adv_stop();
		ret = bt_le_adv_start(BT_LE_ADV_CONN, ad, ARRAY_SIZE(ad), sd, ARRAY_SIZE(sd));
	}
	if (!ret) {
		LOG_INF("BLE Advertising active as '%s'", CONFIG_BT_DEVICE_NAME);
	}
}

static void connected_cb(struct bt_conn *conn, uint8_t err)
{
	char addr[BT_ADDR_LE_STR_LEN];
	bt_addr_le_to_str(bt_conn_get_dst(conn), addr, sizeof(addr));

	if (err) {
		LOG_ERR("Connection failed (err 0x%02x): %s", err, addr);
		return;
	}

	g_is_connected = true;
	LOG_INF("Connected to peer: %s", addr);

#if DT_NODE_HAS_STATUS(LED0_NODE, okay)
	if (device_is_ready(led_conn.port)) {
		gpio_pin_set_dt(&led_conn, 1);
	}
#endif

	audio_stream_service_on_connected(conn);
}

static void disconnected_cb(struct bt_conn *conn, uint8_t reason)
{
	char addr[BT_ADDR_LE_STR_LEN];
	bt_addr_le_to_str(bt_conn_get_dst(conn), addr, sizeof(addr));

	g_is_connected = false;
	LOG_INF("Disconnected from %s (reason 0x%02x)", addr, reason);

#if DT_NODE_HAS_STATUS(LED0_NODE, okay)
	if (device_is_ready(led_conn.port)) {
		gpio_pin_set_dt(&led_conn, 0);
	}
#endif
#if DT_NODE_HAS_STATUS(LED1_NODE, okay)
	if (device_is_ready(led_play.port)) {
		gpio_pin_set_dt(&led_play, 0);
	}
#endif

	audio_stream_service_on_disconnected(conn);

	/* Restart advertising */
	start_advertising();
}

static void le_phy_updated_cb(struct bt_conn *conn, struct bt_conn_le_phy_info *param)
{
	LOG_INF("LE PHY Updated: TX=%u, RX=%u", param->tx_phy, param->rx_phy);
}

static void le_data_len_updated_cb(struct bt_conn *conn, struct bt_conn_le_data_len_info *info)
{
	LOG_INF("LE Data Length Updated: TX max=%u bytes, RX max=%u bytes",
		info->tx_max_len, info->rx_max_len);
}

BT_CONN_CB_DEFINE(conn_callbacks) = {
	.connected = connected_cb,
	.disconnected = disconnected_cb,
	.le_phy_updated = le_phy_updated_cb,
	.le_data_len_updated = le_data_len_updated_cb,
};

int main(void)
{
	LOG_INF("==================================================");
	LOG_INF("  5th Sense nRF54L15 Audio Receiver & Streamer    ");
	LOG_INF("  Target: nRF54L15 Dev Module + MAX98357A I2S     ");
	LOG_INF("==================================================");

	/* 1. Init LEDs */
#if DT_NODE_HAS_STATUS(LED0_NODE, okay)
	if (device_is_ready(led_conn.port)) {
		gpio_pin_configure_dt(&led_conn, GPIO_OUTPUT_INACTIVE);
	}
#endif
#if DT_NODE_HAS_STATUS(LED1_NODE, okay)
	if (device_is_ready(led_play.port)) {
		gpio_pin_configure_dt(&led_play, GPIO_OUTPUT_INACTIVE);
	}
#endif

	/* 2. Init Buttons: SW3 (P1.09), SW5 (P1.08), SW6 (P0.04) */
	g_gpio1_dev = DEVICE_DT_GET(DT_NODELABEL(gpio1));
	g_gpio0_dev = DEVICE_DT_GET(DT_NODELABEL(gpio0));

	if (g_gpio1_dev && device_is_ready(g_gpio1_dev)) {
		/* SW3 (Button1 / P1.09) */
		gpio_pin_configure(g_gpio1_dev, 9, GPIO_INPUT | GPIO_PULL_UP);
		gpio_pin_interrupt_configure(g_gpio1_dev, 9, GPIO_INT_EDGE_FALLING);

		/* SW5 (Button2 / P1.08) */
		gpio_pin_configure(g_gpio1_dev, 8, GPIO_INPUT | GPIO_PULL_UP);
		gpio_pin_interrupt_configure(g_gpio1_dev, 8, GPIO_INT_EDGE_FALLING);

		gpio_init_callback(&g_btn_cb_gpio1, button_pressed_cb, BIT(9) | BIT(8));
		gpio_add_callback(g_gpio1_dev, &g_btn_cb_gpio1);
		LOG_INF("Buttons SW3 (P1.09) and SW5 (P1.08) configured");
	}

	if (g_gpio0_dev && device_is_ready(g_gpio0_dev)) {
		/* SW6 (Button3 / P0.04) */
		gpio_pin_configure(g_gpio0_dev, 4, GPIO_INPUT | GPIO_PULL_UP);
		gpio_pin_interrupt_configure(g_gpio0_dev, 4, GPIO_INT_EDGE_FALLING);

		gpio_init_callback(&g_btn_cb_gpio0, button_pressed_cb, BIT(4));
		gpio_add_callback(g_gpio0_dev, &g_btn_cb_gpio0);
		LOG_INF("Button SW6 (P0.04) configured");
	}

	/* 3. Init Audio DSP & Jitter Buffer */
	audio_dsp_init();
	jitter_buffer_init();

	/* 4. Init Flash Storage */
	int ret = flash_storage_init();
	if (ret == 0) {
		LOG_INF("Flash storage ready for audio asset caching");
	}

	/* 5. Init I2S Driver for MAX98357A */
	ret = audio_i2s_init();
	if (ret != 0) {
		LOG_ERR("Failed to initialize I2S interface: %d", ret);
	}

	/* 6. Init Tone & Melody Synthesizer */
	tone_synth_init();

	/* 7. Init Audio GATT Service */
	audio_stream_service_init();

	/* 8. Enable Bluetooth */
	ret = bt_enable(NULL);
	if (ret) {
		LOG_ERR("Bluetooth init failed (err %d)", ret);
		return ret;
	}
	LOG_INF("Bluetooth initialized successfully");

	/* 9. Start Advertising */
	start_advertising();

	/* 10. Play Startup Boot Tune on MAX98357A I2S DAC */
	k_msleep(150);
	tone_synth_play_startup_tune();

	/* 11. Housekeeping, Polling & Stats Loop */
	int stats_counter = 0;
	while (1) {
		k_msleep(50);

		/* Button Polling backup (Grounded when pressed -> raw value 0) */
		if (g_gpio1_dev && device_is_ready(g_gpio1_dev)) {
			if (gpio_pin_get_raw(g_gpio1_dev, 9) == 0) {
				trigger_diagnostic_tune("SW3 (Button1 / P1.09)");
			} else if (gpio_pin_get_raw(g_gpio1_dev, 8) == 0) {
				trigger_diagnostic_tune("SW5 (Button2 / P1.08)");
			}
		}
		if (g_gpio0_dev && device_is_ready(g_gpio0_dev)) {
			if (gpio_pin_get_raw(g_gpio0_dev, 4) == 0) {
				trigger_diagnostic_tune("SW6 (Button3 / P0.04)");
			}
		}

		stats_counter++;
		if (stats_counter >= 10) { /* Every 500ms */
			stats_counter = 0;

			/* Watchdog: If disconnected, ensure advertising is continuously running */
			if (!g_is_connected) {
				start_advertising();
			}

			/* Update Play LED */
#if DT_NODE_HAS_STATUS(LED1_NODE, okay)
			if (device_is_ready(led_play.port)) {
				gpio_pin_set_dt(&led_play, audio_i2s_is_active() ? 1 : 0);
			}
#endif
			/* Send periodic stats to connected phone */
			audio_stream_service_send_stats();
		}
	}

	return 0;
}
