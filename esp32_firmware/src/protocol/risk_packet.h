#pragma once

#include <Arduino.h>

struct UnifiedRiskTransport {
  unsigned long timestamp;
  float injuryScore;
  const char* injuryLevel;
  float physiologicalScore;
  const char* physiologicalLevel;
  float overallScore;
  const char* overallLevel;
  const char* status;
  bool alert;
  const char* alertType;
  const char* priority;
  const char* message;
};

String serializeRiskPacket(const UnifiedRiskTransport& result);
