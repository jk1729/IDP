#include "ble_callbacks.h"
#include "ble_manager.h"
ServerCallbacks::ServerCallbacks(BleManager& manager) : manager_(manager) {}
void ServerCallbacks::onConnect(BLEServer*) { manager_.setConnected(true); }
void ServerCallbacks::onDisconnect(BLEServer* server) {
  manager_.setConnected(false);
  server->getAdvertising()->start();
}
CommandCallbacks::CommandCallbacks(BleManager& manager) : manager_(manager) {}
void CommandCallbacks::onWrite(BLECharacteristic* characteristic) {
  manager_.handleCommand(characteristic->getValue().c_str());
}

