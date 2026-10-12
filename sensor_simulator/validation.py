"""Basic Phase 2 data and CSV validation."""

import csv
import math
from datetime import datetime
from pathlib import Path
from typing import Sequence

from sensor_simulator.config import SimulationConfig
from sensor_simulator.models import SensorReading
from sensor_simulator.simulator import CSV_COLUMNS


def validate_readings(readings: Sequence[SensorReading], config: SimulationConfig) -> None:
    if not readings:
        raise ValueError("No sensor readings were generated")
    previous = None
    for reading in readings:
        if previous and datetime.fromisoformat(reading.timestamp.replace("Z", "+00:00")) <= datetime.fromisoformat(previous.timestamp.replace("Z", "+00:00")):
            raise ValueError("Timestamps must increase strictly")
        previous = reading
        for value in (reading.accel_x_mps2, reading.accel_y_mps2, reading.accel_z_mps2, reading.gyro_x_dps, reading.gyro_y_dps, reading.gyro_z_dps):
            if not math.isfinite(value):
                raise ValueError("IMU values must be finite numbers")
        if not config.fsr_min <= reading.heel_fsr_raw <= config.fsr_max or not config.fsr_min <= reading.forefoot_fsr_raw <= config.fsr_max:
            raise ValueError("FSR value is outside configured limits")
        if not config.heart_rate_min_bpm <= reading.heart_rate_bpm <= config.heart_rate_max_bpm:
            raise ValueError("Heart rate is outside configured simulation limits")
        if not config.temperature_min_c <= reading.temperature_c <= config.temperature_max_c:
            raise ValueError("Temperature is outside configured simulation limits")


def validate_csv(path: str | Path) -> None:
    with Path(path).open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        missing = [column for column in CSV_COLUMNS if column not in (reader.fieldnames or [])]
        if missing:
            raise ValueError(f"CSV is missing columns: {missing}")

