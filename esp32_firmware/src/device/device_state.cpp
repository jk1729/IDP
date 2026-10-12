#include "device_state.h"

#include "config.h"

DeviceState::DeviceState()
    : connected_(false),
      status_(DeviceStatusState::DISCONNECTED),
      mode_(SIMULATION_MODE ? DeviceMode::SIMULATION : DeviceMode::HARDWARE),
      startedAt_(millis()),
      lastPacketAt_(0) {}

void DeviceState::setConnected(bool connected) {
  connected_ = connected;
  status_ = connected ? DeviceStatusState::CONNECTED : DeviceStatusState::DISCONNECTED;
}
void DeviceState::startSession() { status_ = DeviceStatusState::SESSION_ACTIVE; }
void DeviceState::stopSession() { status_ = connected_ ? DeviceStatusState::CONNECTED : DeviceStatusState::DISCONNECTED; }
void DeviceState::startCalibration() { status_ = DeviceStatusState::CALIBRATION; }
void DeviceState::stopCalibration() { status_ = connected_ ? DeviceStatusState::CONNECTED : DeviceStatusState::DISCONNECTED; }
bool DeviceState::isConnected() const { return connected_; }
DeviceStatusState DeviceState::status() const { return status_; }
DeviceMode DeviceState::mode() const { return mode_; }
unsigned long DeviceState::uptimeMs() const { return millis() - startedAt_; }
unsigned long DeviceState::lastPacketMs() const { return lastPacketAt_; }
void DeviceState::markPacket() { lastPacketAt_ = millis(); }

