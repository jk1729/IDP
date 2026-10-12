#include "risk_packet.h"

#include <ArduinoJson.h>

String serializeRiskPacket(const UnifiedRiskTransport& result) {
  JsonDocument document;
  document["type"] = "risk";
  document["ts"] = result.timestamp;
  document["injury"] = result.injuryScore;
  document["injury_level"] = result.injuryLevel;
  document["physio"] = result.physiologicalScore;
  document["physio_level"] = result.physiologicalLevel;
  document["overall"] = result.overallScore;
  document["overall_level"] = result.overallLevel;
  document["status"] = result.status;
  document["alert"] = result.alert;
  document["alert_type"] = result.alertType;
  document["priority"] = result.priority;
  document["message"] = result.message;
  String output;
  serializeJson(document, output);
  return output;
}
