#pragma once
#include <Arduino.h>
#include "../device/device_state.h"
String serializeDeviceStatus(const DeviceState& state, bool sensorReady, int batteryPercent);

