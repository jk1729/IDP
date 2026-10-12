#include "sensor_simulator.h"

#include <math.h>

SensorSimulator::SensorSimulator(unsigned long seed) : seed_(seed), ready_(false) {}

bool SensorSimulator::begin() {
  randomSeed(seed_);
  ready_ = true;
  return ready_;
}

SensorReading SensorSimulator::read() {
  const float phase = millis() / 550.0f;
  const float contact = max(0.0f, sinf(phase));
  return {
      millis(),
      static_cast<int>(180 + 2200 * contact),
      static_cast<int>(220 + 2700 * max(0.0f, sinf(phase + 0.85f))),
      1.8f * sinf(phase),
      0.7f * cosf(phase),
      9.81f + 1.3f * contact,
      18.0f * sinf(phase),
      12.0f * cosf(phase),
      25.0f * sinf(phase + 0.3f),
      110.0f + 3.0f * sinf(phase / 10.0f),
      30.5f + 0.1f * sinf(phase / 20.0f),
      PACKET_MODE_SIMULATION};
}

bool SensorSimulator::isReady() const { return ready_; }

