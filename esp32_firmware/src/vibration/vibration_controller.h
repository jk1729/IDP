#pragma once
#include <Arduino.h>

class VibrationController {
 public:
  explicit VibrationController(int pin);
  void begin();
  void test();
  void requestWarning(unsigned long durationMs);
  void stop();
  void update();
 private:
  int pin_;
  unsigned long stopAt_;
  bool enabled_;
};

