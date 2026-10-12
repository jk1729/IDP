#pragma once

#include <Arduino.h>

#include "../sensors/sensor_interface.h"

String serializeLiveData(const SensorReading& reading);

