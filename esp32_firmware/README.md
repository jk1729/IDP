# Phase 8 - ESP32 + BLE Integration

This separate PlatformIO project defines the ESP32 BLE peripheral architecture.
Hardware is not currently available, so the default firmware uses `SensorSimulator`
and marks readings as `SIMULATION`. Hardware validation is pending because the
physical sensors and ESP32 prototype are not currently available.

## GATT service

| Item | UUID | Properties |
|---|---|---|
| Service | `7e3a0001-5e8d-4f4a-9a9b-1a2b3c4d5e01` | Custom |
| Live data | `7e3a0002-5e8d-4f4a-9a9b-1a2b3c4d5e01` | Read, Notify |
| Risk | `7e3a0003-5e8d-4f4a-9a9b-1a2b3c4d5e01` | Read, Notify |
| Device status | `7e3a0004-5e8d-4f4a-9a9b-1a2b3c4d5e01` | Read, Notify |
| Command | `7e3a0005-5e8d-4f4a-9a9b-1a2b3c4d5e01` | Write, Write Without Response |

Device name: `SMART_GAIT_001`.

## Packets

Live packets use compact JSON for development:

```json
{"type":"live","ts":123456,"hr":110,"temp":30.5,"heel":420,"forefoot":310,"acc":9.81,"cad":null,"mode":"SIMULATION"}
```

`null` means unavailable; zero is not a missing-value marker. Risk packets are
transport-only serializations of Phase 7 results; this firmware does not calculate
injury or physiological risk.

Commands include `PING`, `GET_STATUS`, `START_SESSION`, `STOP_SESSION`,
`START_CALIBRATION`, `STOP_CALIBRATION`, and `VIBRATE_TEST`. State-changing commands
are parsed safely; unknown or malformed commands do not change state.

## Build

From `esp32_firmware/`:

```text
pio run
pio run -t upload
pio device monitor
```

The radio, board, and sensors have not been tested because hardware is unavailable.

## Manual BLE test when hardware exists

Flash the board, open the serial monitor, scan with nRF Connect, find
`SMART_GAIT_001`, discover the custom service, subscribe to live/risk notifications,
read status, and write JSON commands to the command characteristic. Verify
acknowledgements and state changes. This is a future procedure, not completed
hardware validation.

## Safety

Verify pins against the exact ESP32 DevKit/WROOM board. FSR1 is heel and FSR2 is
forefoot. Do not connect a Li-ion cell directly to 3.3 V; use a protected battery
and correct regulated input. Do not place a rigid battery under the athlete's foot.
The GPIO must drive a proper transistor/MOSFET motor circuit and must not power the
motor directly. Confirm voltage/current requirements before integration.

`SensorSimulator` can later be replaced by FSR, MPU6050, MAX30102, and temperature
drivers implementing `SensorInterface` without changing the BLE packet boundary.
JSON is used for development simplicity; binary packets can be considered later if
packet-size measurements require it.
