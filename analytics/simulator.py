"""Deterministic simulated sensor source. Replace this adapter with ESP32 input later."""

from __future__ import annotations

import math
import random
from datetime import datetime, timedelta, timezone
from typing import Iterator

from .models import RawSensorData


def simulate_readings(
    count: int = 1800,
    sample_rate_hz: float = 10.0,
    seed: int = 7,
) -> Iterator[RawSensorData]:
    """Yield deterministic raw readings for a 2-5 minute baseline/session."""
    rng = random.Random(seed)
    start = datetime.now(timezone.utc).replace(microsecond=0)
    for index in range(count):
        seconds = index / sample_rate_hz
        phase = 2.0 * math.pi * 2.75 * seconds
        contact = max(0.0, math.sin(phase))
        timestamp = (start + timedelta(seconds=seconds)).isoformat(timespec="milliseconds").replace("+00:00", "Z")
        yield RawSensorData(
            timestamp=timestamp,
            heel_fsr_raw=int(650 + 2050 * contact + rng.gauss(0, 35)),
            forefoot_fsr_raw=int(800 + 2450 * max(0.0, math.sin(phase + 0.45)) + rng.gauss(0, 40)),
            accel_x_mps2=0.35 * math.sin(phase) + rng.gauss(0, 0.08),
            accel_y_mps2=-0.8 * math.cos(phase) + rng.gauss(0, 0.08),
            accel_z_mps2=9.81 + 1.8 * contact + rng.gauss(0, 0.15),
            gyro_x_dps=12.0 * math.sin(phase) + rng.gauss(0, 1.5),
            gyro_y_dps=8.0 * math.cos(phase) + rng.gauss(0, 1.5),
            gyro_z_dps=20.0 * math.sin(phase + 0.2) + rng.gauss(0, 1.5),
            heart_rate_bpm=122.0 + 4.0 * math.sin(seconds / 20.0) + rng.gauss(0, 1.2),
            temperature_c=30.2 + 0.15 * math.sin(seconds / 30.0) + rng.gauss(0, 0.04),
        )

