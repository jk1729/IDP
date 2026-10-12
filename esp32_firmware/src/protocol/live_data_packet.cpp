#include "live_data_packet.h"

#include <ArduinoJson.h>

String serializeLiveData(const SensorReading& reading) {
  JsonDocument document;
  document["type"] = "live";
  document["ts"] = reading.timestamp;
  document["hr"] = reading.heartRate;
  document["temp"] = reading.temperature;
  document["heel"] = reading.heel;
  document["forefoot"] = reading.forefoot;
  document["acc"] = sqrtf(reading.accelX * reading.accelX + reading.accelY * reading.accelY +
                          reading.accelZ * reading.accelZ);
  document["cad"] = nullptr;  // Cadence is unavailable until the processing layer supplies it.
  document["mode"] = reading.mode;
  String output;
  serializeJson(document, output);
  return output;
}

