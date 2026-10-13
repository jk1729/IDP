#pragma once

#include "sensor_interface.h"

class RealFSRSensor final : public SensorInterface {
 public:
  RealFSRSensor(int heelPin = 34, int forefootPin = 35);
  bool begin() override;
  SensorReading read() override;
  bool isReady() const override;

 private:
  int heelPin_;
  int forefootPin_;
  bool ready_;
  int readAveragedADC(int pin);
};

