"""CSV-to-CSV Phase 3 processing pipeline."""

import csv
from dataclasses import asdict
from pathlib import Path
from typing import Dict, Sequence

from .feature_extractor import ProcessedFeatures, combine
from .filters import moving_average
from .fsr_processing import classify_loading_distribution, loading_summary, process_fsr
from .gait_processing import apply_step_features, cadence_from_steps, contact_features, detect_steps
from .imu_processing import motion_summary, process_imu
from .physiological_processing import process_physiology


def _value(row: Dict[str, str], *names: str) -> float:
    for name in names:
        if name in row and row[name] != "":
            return float(row[name])
    raise ValueError(f"Missing required field; expected one of {names}")


def _read_rows(path: str | Path) -> list[dict[str, str]]:
    with Path(path).open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        raise ValueError("Input CSV contains no data rows")
    return rows


def process_sensor_data(
    input_file: str | Path,
    output_file: str | Path,
    sample_interval: float = 0.1,
    fsr_filter_window: int = 3,
    imu_filter_window: int = 3,
    contact_threshold: float = 500.0,
    step_threshold: float = 1.0,
    min_step_interval: float = 0.3,
    distribution_tolerance: float = 10.0,
) -> list[ProcessedFeatures]:
    rows = _read_rows(input_file)
    heel = [_value(row, "heel_fsr_raw", "heel_fsr") for row in rows]
    forefoot = [_value(row, "forefoot_fsr_raw", "forefoot_fsr") for row in rows]
    imu = process_imu(
        [_value(row, "accel_x_mps2", "accel_x") for row in rows],
        [_value(row, "accel_y_mps2", "accel_y") for row in rows],
        [_value(row, "accel_z_mps2", "accel_z") for row in rows],
        [_value(row, "gyro_x_dps", "gyro_x") for row in rows],
        [_value(row, "gyro_y_dps", "gyro_y") for row in rows],
        [_value(row, "gyro_z_dps", "gyro_z") for row in rows],
        imu_filter_window,
    )
    fsr = process_fsr(heel, forefoot, fsr_filter_window)
    gait = contact_features([item.total_loading for item in fsr], contact_threshold, sample_interval)
    steps = detect_steps([item.dynamic_acceleration for item in imu], sample_interval, step_threshold, min_step_interval)
    gait = apply_step_features(gait, steps, sample_interval)
    physiology = process_physiology(
        [_value(row, "heart_rate_bpm", "heart_rate") for row in rows],
        [_value(row, "temperature_c", "temperature") for row in rows],
        fsr_filter_window,
    )
    impact = motion_summary(imu)
    summary = loading_summary(fsr)
    output = []
    for index, row in enumerate(rows):
        output.append(combine(
            row["timestamp"], row.get("activity_state", "UNKNOWN"), fsr[index], imu[index], gait[index],
            physiology[index], classify_loading_distribution(fsr[index].heel_percentage, distribution_tolerance),
            impact, {"angular_motion_mean": impact["angular_motion_mean"],
                     "angular_motion_peak": impact["angular_motion_peak"],
                     "angular_motion_std": impact["angular_motion_std"]},
        ))
    target = Path(output_file)
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(asdict(output[0])))
        writer.writeheader()
        writer.writerows(asdict(item) for item in output)
    return output

