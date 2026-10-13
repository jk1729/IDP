#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

// ESP32-WROOM (original ESP32) ADC1 pins
const int FSR1_PIN = 34;
const int FSR2_PIN = 35;

// Match these to the actual fixed resistors in your voltage dividers.
const float VCC = 3.3f;
const float R_FIXED_OHMS = 10000.0f;

int readAveragedADC(int pin) {
  (void)analogRead(pin);  // discard first conversion after channel switching
  delay(3);
  uint32_t total = 0;
  const int samples = 12;
  for (int i = 0; i < samples; ++i) {
    total += analogRead(pin);
    delay(2);
  }
  return total / samples;
}

float calculateVoltage(int raw) {
  return (raw / 4095.0f) * VCC;
}

// Wiring assumed: 3V3 -> FSR -> ADC junction -> fixed resistor -> GND.
// Formula: R_FSR = R_FIXED * (VCC / Vout - 1).
// Near raw=0, the FSR resistance is too large to estimate accurately.
// Near raw=4095, the ADC is saturated and resistance is unreliable.
float calculateFSRResistance(int raw) {
  if (raw <= 1 || raw >= 4090) return -1.0f;
  float voltage = calculateVoltage(raw);
  if (voltage <= 0.001f) return -1.0f;
  return R_FIXED_OHMS * ((VCC / voltage) - 1.0f);
}

void setup() {
  Serial.begin(115200);
  analogReadResolution(12);
  analogSetPinAttenuation(FSR1_PIN, ADC_11db);
  analogSetPinAttenuation(FSR2_PIN, ADC_11db);

  // Bluetooth Classic SPP. Requires an original ESP32-WROOM, not ESP32-C3/S3.
  SerialBT.begin("ESP32_FSR");

  // CSV header/data format:
  // FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2
  Serial.println("FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2");
  SerialBT.println("FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2");
}

void loop() {
  int raw1 = readAveragedADC(FSR1_PIN);
  int raw2 = readAveragedADC(FSR2_PIN);

  float v1 = calculateVoltage(raw1);
  float v2 = calculateVoltage(raw2);
  float r1 = calculateFSRResistance(raw1);
  float r2 = calculateFSRResistance(raw2);

  // A single CSV line makes parsing in the PC bridge reliable.
  char line[160];
  snprintf(line, sizeof(line), "FSR,%d,%.4f,%.1f,%d,%.4f,%.1f",
           raw1, v1, r1, raw2, v2, r2);

  Serial.println(line);    // USB serial monitor
  SerialBT.println(line);  // Bluetooth Classic SPP terminal / PC bridge

  delay(100);
}
