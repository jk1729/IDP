#include <Arduino.h>
#include "ble/ble_manager.h"
#include "config.h"
#include "device/device_state.h"
#include "protocol/device_status_packet.h"
#include "protocol/live_data_packet.h"
#include "protocol/risk_packet.h"
#if SIMULATION_MODE
#include "sensors/sensor_simulator.h"
#else
#include "sensors/real_fsr_sensor.h"
#endif
#include "vibration/vibration_controller.h"
DeviceState deviceState;
#if SIMULATION_MODE
SensorSimulator sensors(42);
#else
RealFSRSensor sensors(FSR_HEEL_PIN, FSR_FOREFOOT_PIN);
#endif
BleManager ble(deviceState);
VibrationController vibration(VIBRATION_PIN);
unsigned long lastLive = 0;
unsigned long lastStatus = 0;
unsigned long lastRisk = 0;
void setup() {
  Serial.begin(115200);
  Serial.println("[BLE] Initializing...");
  sensors.begin(); vibration.begin(); ble.begin();
#if SIMULATION_MODE
  Serial.println("[SENSOR] Simulation mode");
#else
  Serial.println("[SENSOR] Physical ESP32 FSR hardware mode (GPIO34, GPIO35)");
#endif
  Serial.println("[BLE] Advertising started");
}
void loop() {
  const unsigned long now = millis();
  vibration.update();
  if (now - lastLive >= LIVE_DATA_INTERVAL_MS) {
    lastLive = now; deviceState.markPacket();
    ble.notifyLive(serializeLiveData(sensors.read()));
  }
  if (now - lastStatus >= STATUS_UPDATE_INTERVAL_MS) {
    lastStatus = now;
    ble.notifyStatus(serializeDeviceStatus(deviceState, sensors.isReady(), 100));
  }
  if (now - lastRisk >= RISK_UPDATE_INTERVAL_MS) {
    lastRisk = now;
    UnifiedRiskTransport demoRisk{
        now, 0.0f, "LOW", 0.0f, "LOW", 0.0f, "LOW", "NORMAL",
        false, "NONE", "NONE", "Simulation risk packet; not a diagnosis."};
    ble.notifyRisk(serializeRiskPacket(demoRisk));
  }
  delay(5);
}
