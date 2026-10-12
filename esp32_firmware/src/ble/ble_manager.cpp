#include "ble_manager.h"
#include <ArduinoJson.h>
#include <BLE2902.h>
#include <BLEDevice.h>
#include "ble_callbacks.h"
#include "ble_config.h"
#include "../device/device_state.h"

BleManager::BleManager(DeviceState& state)
    : state_(state), server_(nullptr), live_(nullptr), risk_(nullptr), status_(nullptr),
      command_(nullptr), statusForAck_(nullptr), connected_(false) {}
void BleManager::begin() {
  BLEDevice::init(BLE_DEVICE_NAME);
  server_ = BLEDevice::createServer();
  server_->setCallbacks(new ServerCallbacks(*this));
  BLEService* service = server_->createService(SMART_GAIT_SERVICE_UUID);
  live_ = service->createCharacteristic(LIVE_DATA_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  risk_ = service->createCharacteristic(RISK_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  status_ = service->createCharacteristic(DEVICE_STATUS_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  statusForAck_ = status_;
  command_ = service->createCharacteristic(COMMAND_CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  live_->addDescriptor(new BLE2902()); risk_->addDescriptor(new BLE2902()); status_->addDescriptor(new BLE2902());
  command_->setCallbacks(new CommandCallbacks(*this));
  service->start();
  server_->getAdvertising()->start();
}
void BleManager::setConnected(bool connected) { connected_ = connected; state_.setConnected(connected); }
void BleManager::notifyLive(const String& packet) { if (connected_) { live_->setValue(packet.c_str()); live_->notify(); } }
void BleManager::notifyRisk(const String& packet) { if (connected_) { risk_->setValue(packet.c_str()); risk_->notify(); } }
void BleManager::notifyStatus(const String& packet) { if (connected_) { status_->setValue(packet.c_str()); status_->notify(); } }
void BleManager::notifyAck(const String& packet) { if (connected_) { status_->setValue(packet.c_str()); status_->notify(); } }
bool BleManager::connected() const { return connected_; }
void BleManager::handleCommand(const String& command) {
  JsonDocument document;
  if (deserializeJson(document, command)) {
    notifyAck("{\"type\":\"error\",\"message\":\"Invalid JSON command\"}");
    return;
  }
  const char* value = document["command"];
  if (!value) {
    notifyAck("{\"type\":\"error\",\"message\":\"Missing command\"}");
    return;
  }
  String name(value);
  if (name == "PING" || name == "GET_STATUS") {
    notifyAck(String("{\"type\":\"ack\",\"command\":\"") + name + "\",\"success\":true}");
  } else if (name == "START_SESSION") {
    state_.startSession();
    notifyAck("{\"type\":\"ack\",\"command\":\"START_SESSION\",\"success\":true}");
  } else if (name == "STOP_SESSION") {
    state_.stopSession();
    notifyAck("{\"type\":\"ack\",\"command\":\"STOP_SESSION\",\"success\":true}");
  } else if (name == "START_CALIBRATION") {
    state_.startCalibration();
    notifyAck("{\"type\":\"ack\",\"command\":\"START_CALIBRATION\",\"success\":true}");
  } else if (name == "STOP_CALIBRATION") {
    state_.stopCalibration();
    notifyAck("{\"type\":\"ack\",\"command\":\"STOP_CALIBRATION\",\"success\":true}");
  }
  else if (name == "VIBRATE_TEST") notifyAck("{\"type\":\"ack\",\"command\":\"VIBRATE_TEST\",\"success\":true}");
  else notifyAck("{\"type\":\"error\",\"message\":\"Unknown command\"}");
}
