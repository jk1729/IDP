# Phase 2 Sensor Simulator

This package generates reproducible **SIMULATED DATA** for the two-FSR smart insole.
It does not implement hardware access, BLE, Android UI, machine learning, injury
diagnosis, or medical diagnosis.

## Run

From the repository root:

```text
python -m sensor_simulator.main --scenario normal_walking --duration 60 --seed 42
```

Available scenarios:

- `normal_rest`
- `normal_walking`
- `exercise`
- `abnormal_loading`
- `abnormal_gait`

Useful options:

```text
--duration 300
--interval 0.1
--seed 123
--output sensor_simulator/output/walking.csv
--json-output sensor_simulator/output/walking.json
```

The default sampling interval is 100 ms (10 Hz). FSR values are raw/relative ADC-like
values and are not converted to Newtons. Each CSV row contains the Phase 1 raw sensor
fields plus `activity_state`.

The source interface is `SimulatedSensorSource.readings()`. A future ESP32 adapter
should implement the same conceptual interface and produce `SensorReading` records.
The processing pipeline can then consume simulator data, ESP32 data, or CSV data
without knowing where the record originated.

The optional visualization requires matplotlib:

```python
from sensor_simulator.visualize import plot_csv
plot_csv("sensor_simulator/output/simulated_data.csv")
```

Every plot is labelled `SIMULATED DATA - NOT EXPERIMENTAL RESULTS`.

