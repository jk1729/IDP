#pragma once

#include <Arduino.h>

enum class DeviceMode { SIMULATION, HARDWARE };
enum class DeviceStatusState { IDLE, CONNECTED, SESSION_ACTIVE, CALIBRATION, DISCONNECTED };

class DeviceState {
 public:
  DeviceState();
  void setConnected(bool connected);
  void startSession();
  void stopSession();
  void startCalibration();
  void stopCalibration();
  bool isConnected() const;
  DeviceStatusState status() const;
  DeviceMode mode() const;
  unsigned long uptimeMs() const;
  unsigned long lastPacketMs() const;
  void markPacket();

 private:
  bool connected_;
  DeviceStatusState status_;
  DeviceMode mode_;
  unsigned long startedAt_;
  unsigned long lastPacketAt_;
};

