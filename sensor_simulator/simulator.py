"""Common sensor-source interface and scenario runner."""

import csv
import json
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Iterable, Iterator, Sequence

from sensor_simulator.config import SimulationConfig
from sensor_simulator.models import ActivityState, SensorReading
from sensor_simulator.sensors import FSRSensorSimulator, HeartRateSimulator, IMUSensorSimulator, TemperatureSimulator


SCENARIOS = {
    "normal_rest": ActivityState.REST,
    "normal_walking": ActivityState.WALKING,
    "exercise": ActivityState.EXERCISE,
    "abnormal_loading": ActivityState.ABNORMAL_WALKING,
    "abnormal_gait": ActivityState.ABNORMAL_WALKING,
}


class SimulatedSensorSource:
    """Produces the same raw record shape a future ESP32 adapter must produce."""

    def __init__(self, config: SimulationConfig, scenario: str = "normal_walking"):
        if scenario not in SCENARIOS:
            raise ValueError(f"Unknown scenario: {scenario}. Choose from {sorted(SCENARIOS)}")
        self.config, self.scenario = config, scenario
        self.rng = random.Random(config.seed)
        self.fsr = FSRSensorSimulator(self.rng, config.fsr_min, config.fsr_max)
        self.imu = IMUSensorSimulator(self.rng)
        self.hr = HeartRateSimulator(self.rng)
        self.temperature = TemperatureSimulator(self.rng)

    def readings(self) -> Iterator[SensorReading]:
        start = datetime(2026, 1, 1, tzinfo=timezone.utc)
        state = SCENARIOS[self.scenario]
        for index in range(self.config.sample_count):
            seconds = index * self.config.sample_interval_seconds
            timestamp = (start + timedelta(seconds=seconds)).isoformat(timespec="milliseconds").replace("+00:00", "Z")
            heel, forefoot = self.fsr.read(seconds, state)
            ax, ay, az, gx, gy, gz = self.imu.read(seconds, state)
            yield SensorReading(
                timestamp=timestamp, heel_fsr_raw=heel, forefoot_fsr_raw=forefoot,
                accel_x_mps2=ax, accel_y_mps2=ay, accel_z_mps2=az,
                gyro_x_dps=gx, gyro_y_dps=gy, gyro_z_dps=gz,
                heart_rate_bpm=self.hr.read(state), temperature_c=self.temperature.read(state),
                activity_state=state,
            )


CSV_COLUMNS = [
    "timestamp", "device_id", "data_source", "activity_state",
    "heel_fsr_raw", "forefoot_fsr_raw", "accel_x_mps2", "accel_y_mps2", "accel_z_mps2",
    "gyro_x_dps", "gyro_y_dps", "gyro_z_dps", "heart_rate_bpm", "temperature_c",
]


def write_csv(readings: Sequence[SensorReading], path: str | Path) -> Path:
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_COLUMNS)
        writer.writeheader()
        for reading in readings:
            row = reading.as_dict()
            writer.writerow({column: row[column] for column in CSV_COLUMNS})
    return output


def write_json(reading: SensorReading, path: str | Path | None = None) -> str:
    payload = {
        "data_source": "SIMULATED_DATA",
        "timestamp": reading.timestamp,
        "activity_state": reading.activity_state.value,
        "sensors": {
            "heel_fsr_raw": reading.heel_fsr_raw,
            "forefoot_fsr_raw": reading.forefoot_fsr_raw,
            "accel_x_mps2": reading.accel_x_mps2,
            "accel_y_mps2": reading.accel_y_mps2,
            "accel_z_mps2": reading.accel_z_mps2,
            "gyro_x_dps": reading.gyro_x_dps,
            "gyro_y_dps": reading.gyro_y_dps,
            "gyro_z_dps": reading.gyro_z_dps,
            "heart_rate_bpm": reading.heart_rate_bpm,
            "temperature_c": reading.temperature_c,
        },
    }
    text = json.dumps(payload, indent=2)
    if path:
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text + "\n", encoding="utf-8")
    return text

