#include "real_fsr_sensor.h"

RealFSRSensor::RealFSRSensor(int heelPin, int forefootPin)
    : heelPin_(heelPin), forefootPin_(forefootPin), ready_(false) {}

bool RealFSRSensor::begin() {
  analogReadResolution(12);
  analogSetPinAttenuation(heelPin_, ADC_11db);
  analogSetPinAttenuation(forefootPin_, ADC_11db);
  ready_ = true;
  return ready_;
}

int RealFSRSensor::readAveragedADC(int pin) {
  (void)analogRead(pin);  // Discard first conversion after channel switching
  delay(3);
  uint32_t total = 0;
  const int samples = 12;
  for (int i = 0; i < samples; ++i) {
    total += analogRead(pin);
    delay(2);
  }
  return total / samples;
}

SensorReading RealFSRSensor::read() {
  const int heelRaw = readAveragedADC(heelPin_);
  const int forefootRaw = readAveragedADC(forefootPin_);

  return {
      millis(),
      heelRaw,
      forefootRaw,
      0.0f,
      0.0f,
      9.81f,
      0.0f,
      0.0f,
      0.0f,
      72.0f,
      36.6f,
      "HARDWARE"};
}

bool RealFSRSensor::isReady() const { return ready_; }

