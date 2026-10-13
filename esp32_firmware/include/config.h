#pragma once

#define DEBUG_LOGGING 1
#define SIMULATION_MODE 0

// Verify every pin against the exact ESP32 DevKit/WROOM board before hardware integration.
#define FSR_HEEL_PIN 34
#define FSR_FOREFOOT_PIN 35
#define I2C_SDA_PIN 21
#define I2C_SCL_PIN 22
#define VIBRATION_PIN 25

#define LIVE_DATA_INTERVAL_MS 500UL
#define RISK_UPDATE_INTERVAL_MS 1000UL
#define STATUS_UPDATE_INTERVAL_MS 5000UL
#define FIRMWARE_VERSION "0.8.0"

