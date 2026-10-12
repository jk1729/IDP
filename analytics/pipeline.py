"""Filtering, baseline calibration, feature extraction, and prototype risk engines."""

from __future__ import annotations

import csv
import json
import math
import statistics
from dataclasses import asdict
from pathlib import Path
from typing import Iterable, List, Sequence

from .models import (
    InjuryRiskFeatures,
    PersonalBaseline,
    PhysiologicalRiskFeatures,
    ProcessedSensorData,
    RawSensorData,
    RiskResult,
    RiskState,
    PersistenceStatus,
    SCHEMA_VERSION,
    to_dict,
    utc_now,
)


def mean(values: Sequence[float]) -> float:
    return statistics.fmean(values) if values else 0.0


def stddev(values: Sequence[float]) -> float:
    return statistics.stdev(values) if len(values) > 1 else 0.0


def bounded_z(value: float, baseline_mean: float, baseline_stddev: float, limit: float = 4.0) -> float:
    z = abs(value - baseline_mean) / max(baseline_stddev, 0.001)
    return min(z / limit, 1.0)


def _raw_features(raw: RawSensorData, previous: RawSensorData | None, step_count: int) -> dict:
    acceleration_magnitude = math.sqrt(raw.accel_x_mps2**2 + raw.accel_y_mps2**2 + raw.accel_z_mps2**2)
    angular_motion = math.sqrt(raw.gyro_x_dps**2 + raw.gyro_y_dps**2 + raw.gyro_z_dps**2)
    total = max(float(raw.heel_fsr_raw + raw.forefoot_fsr_raw), 1.0)
    heel_percentage = raw.heel_fsr_raw / total * 100.0
    forefoot_percentage = raw.forefoot_fsr_raw / total * 100.0
    phase_step = 1 if previous and raw.accel_z_mps2 > 10.8 and previous.accel_z_mps2 <= 10.8 else 0
    cadence = 165.0 if phase_step else 0.0
    gait_cycle = 60.0 / cadence if cadence else 0.0
    return {
        "heel": float(raw.heel_fsr_raw),
        "forefoot": float(raw.forefoot_fsr_raw),
        "total": total,
        "heel_percentage": heel_percentage,
        "forefoot_percentage": forefoot_percentage,
        "imbalance": (raw.heel_fsr_raw - raw.forefoot_fsr_raw) / total,
        "acceleration_magnitude": acceleration_magnitude,
        "step_event": phase_step,
        "cadence": cadence,
        "gait_cycle": gait_cycle,
        "stance_time": 0.29 if phase_step else 0.0,
        "impact": max(0.0, acceleration_magnitude - 9.81),
        "angular_motion": angular_motion,
        "gait_angle": math.degrees(math.atan2(raw.accel_x_mps2, raw.accel_z_mps2)),
        "heart_rate": raw.heart_rate_bpm,
        "temperature": raw.temperature_c,
    }


def process_readings(raw_readings: Sequence[RawSensorData]) -> List[ProcessedSensorData]:
    """Apply a simple moving average and extract beginner-friendly prototype features."""
    output: List[ProcessedSensorData] = []
    heel_window: List[float] = []
    forefoot_window: List[float] = []
    acceleration_window: List[float] = []
    hr_window: List[float] = []
    temperature_window: List[float] = []
    steps = 0
    previous = None
    for raw in raw_readings:
        values = _raw_features(raw, previous, steps)
        steps += int(values["step_event"] > 0)
        for window, value in (
            (heel_window, values["heel"]),
            (forefoot_window, values["forefoot"]),
            (acceleration_window, values["acceleration_magnitude"]),
            (hr_window, values["heart_rate"]),
            (temperature_window, values["temperature"]),
        ):
            window.append(value)
            del window[:-5]
        total = mean(heel_window) + mean(forefoot_window)
        safe_total = max(total, 1.0)
        output.append(
            ProcessedSensorData(
                heel_loading_filtered=mean(heel_window),
                forefoot_loading_filtered=mean(forefoot_window),
                total_loading=total,
                heel_percentage=mean(heel_window) / safe_total * 100.0,
                forefoot_percentage=mean(forefoot_window) / safe_total * 100.0,
                heel_forefoot_imbalance=(mean(heel_window) - mean(forefoot_window)) / safe_total,
                acceleration_magnitude_mps2=values["acceleration_magnitude"],
                acceleration_filtered_mps2=mean(acceleration_window),
                step_count=steps,
                cadence_steps_per_min=165.0 if values["step_event"] else 0.0,
                gait_cycle_duration_s=0.3636 if values["step_event"] else 0.0,
                stance_time_s=values["stance_time"],
                impact_mps2=values["impact"],
                angular_motion_dps=values["angular_motion"],
                gait_angle_deg=values["gait_angle"],
                heart_rate_filtered_bpm=mean(hr_window),
                temperature_filtered_c=mean(temperature_window),
            )
        )
        previous = raw
    return output


def calibrate_baseline(processed: Sequence[ProcessedSensorData], baseline_id: str = "baseline-001") -> PersonalBaseline:
    """Create a personalized baseline from the first 2-5 minutes of valid data."""
    fields = {
        "hr": [item.heart_rate_filtered_bpm for item in processed],
        "temperature": [item.temperature_filtered_c for item in processed],
        "heel": [item.heel_loading_filtered for item in processed],
        "forefoot": [item.forefoot_loading_filtered for item in processed],
        "total": [item.total_loading for item in processed],
        "heel_percentage": [item.heel_percentage for item in processed],
        "forefoot_percentage": [item.forefoot_percentage for item in processed],
        "cadence": [item.cadence_steps_per_min for item in processed if item.cadence_steps_per_min],
        "gait_cycle": [item.gait_cycle_duration_s for item in processed if item.gait_cycle_duration_s],
        "stance": [item.stance_time_s for item in processed if item.stance_time_s],
        "impact": [item.impact_mps2 for item in processed],
        "gait_angle": [item.gait_angle_deg for item in processed],
    }
    return PersonalBaseline(
        baseline_id=baseline_id, created_at=utc_now(),
        duration_seconds=max(120, min(300, len(processed) // 10)), activity_type="GENERAL",
        sample_count=len(processed), hr_mean=mean(fields["hr"]), hr_stddev=stddev(fields["hr"]),
        temperature_mean=mean(fields["temperature"]), temperature_stddev=stddev(fields["temperature"]),
        heel_loading_mean=mean(fields["heel"]), heel_loading_stddev=stddev(fields["heel"]),
        forefoot_loading_mean=mean(fields["forefoot"]), forefoot_loading_stddev=stddev(fields["forefoot"]),
        total_loading_mean=mean(fields["total"]), total_loading_stddev=stddev(fields["total"]),
        heel_percentage_mean=mean(fields["heel_percentage"]), heel_percentage_stddev=stddev(fields["heel_percentage"]),
        forefoot_percentage_mean=mean(fields["forefoot_percentage"]), forefoot_percentage_stddev=stddev(fields["forefoot_percentage"]),
        cadence_mean=mean(fields["cadence"]), cadence_stddev=stddev(fields["cadence"]),
        gait_cycle_mean=mean(fields["gait_cycle"]), gait_cycle_stddev=stddev(fields["gait_cycle"]),
        stance_time_mean=mean(fields["stance"]), stance_time_stddev=stddev(fields["stance"]),
        impact_mean=mean(fields["impact"]), impact_stddev=stddev(fields["impact"]),
        gait_angle_mean=mean(fields["gait_angle"]), gait_angle_stddev=stddev(fields["gait_angle"]),
        quality_score=1.0 if len(processed) >= 1200 else 0.5, valid=len(processed) >= 1200,
    )


def compare_to_baseline(item: ProcessedSensorData, baseline: PersonalBaseline) -> InjuryRiskFeatures:
    loading = bounded_z(item.total_loading, baseline.total_loading_mean, baseline.total_loading_stddev)
    angle = bounded_z(item.gait_angle_deg, baseline.gait_angle_mean, baseline.gait_angle_stddev)
    impact = bounded_z(item.impact_mps2, baseline.impact_mean, baseline.impact_stddev)
    cycle = bounded_z(item.gait_cycle_duration_s, baseline.gait_cycle_mean, baseline.gait_cycle_stddev)
    return InjuryRiskFeatures(
        loading_deviation_raw=item.total_loading - baseline.total_loading_mean,
        loading_deviation_score=loading,
        gait_angle_deviation_raw=item.gait_angle_deg - baseline.gait_angle_mean,
        gait_angle_deviation_score=angle,
        impact_deviation_raw=item.impact_mps2 - baseline.impact_mean,
        impact_deviation_score=impact,
        gait_cycle_deviation_raw=item.gait_cycle_duration_s - baseline.gait_cycle_mean,
        gait_cycle_abnormality_score=cycle,
    )


def calculate_risk(
    timestamp: str,
    item: ProcessedSensorData,
    baseline: PersonalBaseline,
    persistence_duration_s: float = 0.0,
) -> tuple[RiskResult, PhysiologicalRiskFeatures, InjuryRiskFeatures]:
    injury_features = compare_to_baseline(item, baseline)
    injury_score = (
        0.35 * injury_features.loading_deviation_score
        + 0.25 * injury_features.gait_angle_deviation_score
        + 0.20 * injury_features.impact_deviation_score
        + 0.20 * injury_features.gait_cycle_abnormality_score
    )
    hr_delta = item.heart_rate_filtered_bpm - baseline.hr_mean
    temperature_delta = item.temperature_filtered_c - baseline.temperature_mean
    drs = (hr_delta * 2.0) + (temperature_delta * 20.0)
    physiology_score = min(max(drs / 100.0, 0.0), 1.0)
    abnormality_types = []
    if injury_score >= 0.4:
        abnormality_types.append("LOADING_OR_GAIT_DEVIATION")
    if abs(hr_delta) > max(2.0 * baseline.hr_stddev, 5.0):
        abnormality_types.append("HEART_RATE_DEVIATION")
    if abs(temperature_delta) > max(2.0 * baseline.temperature_stddev, 0.5):
        abnormality_types.append("TEMPERATURE_DEVIATION")
    abnormal = bool(abnormality_types)
    persistence = PersistenceStatus.PERSISTING if persistence_duration_s >= 3.0 else (
        PersistenceStatus.OBSERVED if abnormal else PersistenceStatus.NONE
    )
    vibration = persistence == PersistenceStatus.PERSISTING and max(injury_score, physiology_score) >= 0.7
    return (
        RiskResult(
            timestamp=timestamp,
            injury_risk_score=round(injury_score, 4),
            physiological_risk_score=round(physiology_score, 4),
            injury_risk_state=state_for(injury_score),
            physiological_risk_state=state_for(physiology_score),
            abnormality_detected=abnormal,
            abnormality_types=abnormality_types,
            persistence_status=persistence,
            persistence_duration_s=persistence_duration_s,
            vibration_alert=vibration,
            vibration_reason="Persistent high prototype risk" if vibration else "Persistence or score threshold not reached",
        ),
        PhysiologicalRiskFeatures(
            hr_base_bpm=baseline.hr_mean, temperature_base_c=baseline.temperature_mean,
            hr_current_bpm=item.heart_rate_filtered_bpm, temperature_current_c=item.temperature_filtered_c,
            hr_delta_bpm=hr_delta, temperature_delta_c=temperature_delta,
            dehydration_risk_score_raw=drs, dehydration_risk_score_normalized=physiology_score,
        ),
        injury_features,
    )


def state_for(score: float) -> RiskState:
    return RiskState.HIGH if score >= 0.7 else RiskState.MODERATE if score >= 0.4 else RiskState.LOW


def export_session(directory: Path, raw: Sequence[RawSensorData], processed: Sequence[ProcessedSensorData], baseline: PersonalBaseline) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    jsonl_path = directory / "session.jsonl"
    csv_path = directory / "session.csv"
    rows = []
    for index, (raw_item, item) in enumerate(zip(raw, processed)):
        risk, physiology, injury = calculate_risk(raw_item.timestamp, item, baseline)
        row = {
            "schema_version": SCHEMA_VERSION, "timestamp": raw_item.timestamp,
            "device_id": raw_item.device_id, "data_source": raw_item.data_source,
            "sensors": to_dict(raw_item), "features": to_dict(item),
            "baseline_comparison": to_dict(injury), "physiological": to_dict(physiology), "risk": to_dict(risk),
        }
        rows.append(row)
    with jsonl_path.open("w", encoding="utf-8", newline="") as handle:
        for row in rows:
            handle.write(json.dumps(row) + "\n")
    flat_rows = [_flatten(row) for row in rows]
    with csv_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(flat_rows[0]), extrasaction="ignore")
        writer.writeheader()
        writer.writerows(flat_rows)
    (directory / "example_reading.json").write_text(json.dumps(rows[-1], indent=2) + "\n", encoding="utf-8")
    (directory / "baseline.json").write_text(json.dumps(to_dict(baseline), indent=2) + "\n", encoding="utf-8")


def _flatten(row: dict) -> dict:
    flat = {"schema_version": row["schema_version"], "timestamp": row["timestamp"],
            "device_id": row["device_id"], "data_source": row["data_source"]}
    for group in ("sensors", "features", "baseline_comparison", "physiological", "risk"):
        for key, value in row[group].items():
            if isinstance(value, (dict, list)):
                continue
            flat[key] = value
    return flat
