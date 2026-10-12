#include "vibration_controller.h"
VibrationController::VibrationController(int pin) : pin_(pin), stopAt_(0), enabled_(false) {}
void VibrationController::begin() { pinMode(pin_, OUTPUT); digitalWrite(pin_, LOW); }
void VibrationController::test() { requestWarning(150); }
void VibrationController::requestWarning(unsigned long durationMs) {
  // GPIO must drive a suitable transistor/MOSFET motor circuit, never the motor directly.
  enabled_ = true; digitalWrite(pin_, HIGH); stopAt_ = millis() + durationMs;
}
void VibrationController::stop() { enabled_ = false; digitalWrite(pin_, LOW); }
void VibrationController::update() {
  if (enabled_ && static_cast<long>(millis() - stopAt_) >= 0) stop();
}

