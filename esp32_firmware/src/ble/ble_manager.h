#pragma once
#include <Arduino.h>
#include <BLECharacteristic.h>
#include <BLEServer.h>
class DeviceState;
class BleManager {
 public:
  explicit BleManager(DeviceState& state);
  void begin();
  void notifyLive(const String& packet);
  void notifyRisk(const String& packet);
  void notifyStatus(const String& packet);
  void handleCommand(const String& command);
  bool connected() const;
  void notifyAck(const String& packet);
 private:
  DeviceState& state_;
  BLEServer* server_;
  BLECharacteristic* live_;
  BLECharacteristic* risk_;
  BLECharacteristic* status_;
  BLECharacteristic* command_;
  BLECharacteristic* statusForAck_;
  bool connected_;
  void setConnected(bool connected);
  friend class ServerCallbacks;
};
