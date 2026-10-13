# ESP32-WROOM Dual FSR Bluetooth & USB Serial + Web Dashboard

## What is included
- `esp32_dual_fsr_bluetooth.ino`: Physical ESP32 firmware. Reads GPIO34 (FSR1 Heel) and GPIO35 (FSR2 Forefoot) and sends CSV over USB serial and Bluetooth Classic SPP at 115200 baud every ~100ms.
- `bridge.py`: Python bridge. Auto-detects ESP32 Bluetooth Classic SPP or USB Serial COM port, parses live CSV readings into validated fields, handles -1 resistance as unavailable, tracks honest connection states, and exposes readings on `http://127.0.0.1:8765/data` and via SSE stream. Also directly serves `dashboard.html` at `http://127.0.0.1:8765/`.
- `dashboard.html`: Real-time browser dashboard with live raw ADC counts (0-4095), voltage (0-3.3V), estimated resistance in Ohms, real-time rolling graphs, loading balance bar, honest connection status indicators, stale warnings, and CSV session export.
- `IDP/processing/esp32_adapter.py`: Physical hardware adapter that streams real ESP32 readings into the canonical RawSensorData processing pipeline.

## Wiring (per sensor)
For each sensor:
- ESP32 3V3 -> one FSR terminal
- Other FSR terminal -> its own signal junction
- Signal junction -> GPIO34 (FSR1 Heel) or GPIO35 (FSR2 Forefoot)
- Same signal junction -> one 10 kΩ fixed resistor
- Other end of fixed resistor -> ESP32 GND

Both dividers may share 3V3 and GND. Do not join the GPIO34 and GPIO35 signal junctions. Do not feed 5 V to GPIO34/GPIO35.

The firmware assumes the FSR is between 3V3 and the ADC junction, with the fixed resistor from the junction to GND. If your resistor value differs from 10 kΩ, change `R_FIXED_OHMS` in the `.ino` file.

## 1. Upload firmware
1. Open `esp32_dual_fsr_bluetooth.ino` in Arduino IDE.
2. Select an original ESP32-WROOM board (Bluetooth Classic SPP is not available on ESP32-C3/S3 in the same way).
3. Select the USB COM port and upload.
4. Open Serial Monitor at 115200 baud if you want to verify USB output, then close Serial Monitor before starting the bridge.

## 2. Connect via Bluetooth Classic or USB Serial
### Option A: Bluetooth Classic SPP (Wireless)
1. Keep USB connected to power the ESP32 (or use battery).
2. In Windows Settings -> Bluetooth & devices -> Add device, pair with `ESP32_FSR`.
3. In Device Manager -> Ports (COM & LPT), identify the outgoing Bluetooth serial port (e.g. COM4, COM33).

### Option B: USB Serial (Direct Cable)
1. Connect ESP32 directly via USB cable to PC.
2. In Device Manager -> Ports (COM & LPT), note the CP210x or CH340 COM port (e.g. COM3).

## 3. Install dependencies and run the Python bridge
Install pyserial if needed, then run:
```cmd
python -m pip install pyserial
cd path\to\this\folder
python bridge.py
```
If you know your COM port, you can specify it explicitly:
```cmd
python bridge.py --port COM4
```
Otherwise, `bridge.py` will automatically scan and select the best available port. You can also switch ports on the fly using the dropdown in the dashboard!

## 4. Open the dashboard
Open `http://localhost:8765/` or double-click `dashboard.html` in Chrome or Edge.
The dashboard automatically connects to the bridge, displaying:
- Live Raw ADC (0 - 4095) for FSR1 & FSR2
- Live Voltage in Volts (0.0000 - 3.3000 V)
- Live Resistance in Ohms (Ω) with "-1" displayed as "Unavailable"
- Live Heel vs Forefoot Loading Balance
- Real-time rolling graphs updating at 10 Hz (~100ms)
- Honest Connection States: Connected, Connecting, Disconnected, Stale, Error

## Force / pressure accuracy warning
FSRs are nonlinear and differ from unit to unit. ADC readings close to 4095 are saturated; values beyond saturation cannot be recovered in software. Values are displayed in physical SI units (counts, V, Ω). Pressure in pascals additionally requires calibrated mechanical contact area.
