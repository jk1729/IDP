#pragma once

#include "sensor_interface.h"

class SensorSimulator final : public SensorInterface {
 public:
  explicit SensorSimulator(unsigned long seed = 42);
  bool begin() override;
  SensorReading read() override;
  bool isReady() const override;

 private:
  unsigned long seed_;
  bool ready_;
};

