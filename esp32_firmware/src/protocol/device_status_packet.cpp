#include "device_status_packet.h"
#include <ArduinoJson.h>
#include "ble_config.h"
#include "config.h"

String serializeDeviceStatus(const DeviceState& state, bool sensorReady, int batteryPercent) {
  JsonDocument document;
  document["type"] = "status";
  document["device"] = BLE_DEVICE_NAME;
  document["firmware"] = FIRMWARE_VERSION;
  document["battery"] = batteryPercent;
  document["connected"] = state.isConnected();
  document["uptime"] = state.uptimeMs() / 1000UL;
  document["sensor_ready"] = sensorReady;
  document["mode"] = state.mode() == DeviceMode::SIMULATION ? "SIMULATION" : "HARDWARE";
  String output;
  serializeJson(document, output);
  return output;
}

