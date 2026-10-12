#pragma once

#include <Arduino.h>

struct SensorReading {
  unsigned long timestamp;
  int heel;
  int forefoot;
  float accelX;
  float accelY;
  float accelZ;
  float gyroX;
  float gyroY;
  float gyroZ;
  float heartRate;
  float temperature;
  const char* mode;
};

class SensorInterface {
 public:
  virtual ~SensorInterface() = default;
  virtual bool begin() = 0;
  virtual SensorReading read() = 0;
  virtual bool isReady() const = 0;
};

