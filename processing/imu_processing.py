"""Magnitude and simple smoothing features for raw IMU values."""

from dataclasses import dataclass
from math import sqrt
from statistics import fmean, pstdev
from typing import Sequence

from .filters import moving_average, validate_numeric


@dataclass
class IMUFeatures:
    accel_magnitude: float
    accel_magnitude_filtered: float
    gyro_magnitude: float
    gyro_magnitude_filtered: float
    dynamic_acceleration: float


def process_imu(
    accel_x: Sequence[float], accel_y: Sequence[float], accel_z: Sequence[float],
    gyro_x: Sequence[float], gyro_y: Sequence[float], gyro_z: Sequence[float],
    window_size: int, gravity: float = 9.81,
) -> list[IMUFeatures]:
    lengths = {len(accel_x), len(accel_y), len(accel_z), len(gyro_x), len(gyro_y), len(gyro_z)}
    if len(lengths) != 1:
        raise ValueError("All IMU sequences must have equal length")
    for name, values in (("accel_x", accel_x), ("accel_y", accel_y), ("accel_z", accel_z),
                         ("gyro_x", gyro_x), ("gyro_y", gyro_y), ("gyro_z", gyro_z)):
        validate_numeric(values, name)
    accel = [sqrt(x * x + y * y + z * z) for x, y, z in zip(accel_x, accel_y, accel_z)]
    gyro = [sqrt(x * x + y * y + z * z) for x, y, z in zip(gyro_x, gyro_y, gyro_z)]
    accel_filtered = moving_average(accel, window_size)
    gyro_filtered = moving_average(gyro, window_size)
    return [
        IMUFeatures(a, af, g, gf, abs(af - gravity))
        for a, af, g, gf in zip(accel, accel_filtered, gyro, gyro_filtered)
    ]


def motion_summary(features: Sequence[IMUFeatures]) -> dict[str, float]:
    impacts = [item.dynamic_acceleration for item in features]
    angular = [item.gyro_magnitude_filtered for item in features]
    return {
        "impact_peak": max(impacts, default=0.0),
        "impact_mean": fmean(impacts) if impacts else 0.0,
        "impact_std": pstdev(impacts) if len(impacts) > 1 else 0.0,
        "angular_motion_mean": fmean(angular) if angular else 0.0,
        "angular_motion_peak": max(angular, default=0.0),
        "angular_motion_std": pstdev(angular) if len(angular) > 1 else 0.0,
    }

