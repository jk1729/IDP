#pragma once
#include <BLEServer.h>
class BleManager;
class ServerCallbacks final : public BLEServerCallbacks {
 public:
  explicit ServerCallbacks(BleManager& manager);
  void onConnect(BLEServer* server) override;
  void onDisconnect(BLEServer* server) override;
 private:
  BleManager& manager_;
};
class CommandCallbacks final : public BLECharacteristicCallbacks {
 public:
  explicit CommandCallbacks(BleManager& manager);
  void onWrite(BLECharacteristic* characteristic) override;
 private:
  BleManager& manager_;
};

