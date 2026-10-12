"""Build personal statistics from Phase 3 processed CSV data."""

from __future__ import annotations

import csv
import math
from datetime import datetime, timezone
from pathlib import Path
from statistics import fmean, pstdev
from typing import Any, Iterable, Mapping, Sequence

from sensor_simulator.config import SimulationConfig

from .baseline_model import FeatureStatistics, PersonalBaseline


FEATURE_COLUMNS = {
    "heel_filtered": "heel_filtered",
    "forefoot_filtered": "forefoot_filtered",
    "total_loading": "total_loading",
    "heel_percentage": "heel_percentage",
    "forefoot_percentage": "forefoot_percentage",
    "loading_imbalance": "loading_imbalance",
    "cadence": "cadence",
    "step_interval": "step_interval",
    "gait_cycle_duration": "gait_cycle_duration",
    "contact_duration": "contact_duration",
    "contact_ratio": "contact_ratio",
    "impact_peak": "impact_peak",
    "impact_mean": "impact_mean",
    "angular_motion_mean": "angular_motion_mean",
    "angular_motion_peak": "angular_motion_peak",
    "heart_rate": "hr_filtered",
    "temperature": "temperature_filtered",
}

GAIT_FEATURES = {
    "cadence", "step_interval", "gait_cycle_duration", "contact_duration",
    "contact_ratio", "impact_peak", "impact_mean", "angular_motion_mean",
    "angular_motion_peak", "heel_filtered", "forefoot_filtered", "total_loading",
    "heel_percentage", "forefoot_percentage", "loading_imbalance",
}


def _read_rows(data: str | Path | Sequence[Mapping[str, Any]]) -> list[dict[str, Any]]:
    if isinstance(data, (str, Path)):
        with Path(data).open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle))
    else:
        rows = [dict(row) for row in data]
    if not rows:
        raise ValueError("Processed dataset is empty")
    return rows


def _numeric_values(rows: Sequence[Mapping[str, Any]], feature: str, column: str) -> list[float]:
    values = []
    for row in rows:
        value = row.get(column)
        if value is None or value == "":
            continue
        try:
            numeric = float(value)
        except (TypeError, ValueError):
            continue
        if math.isfinite(numeric):
            values.append(numeric)
    return values


def _select_values(rows: Sequence[Mapping[str, Any]], feature: str, column: str) -> list[float]:
    if feature in GAIT_FEATURES:
        walking = [row for row in rows if row.get("activity_state") == "WALKING"]
        selected = walking or list(rows)
    elif feature == "heart_rate" or feature == "temperature":
        resting = [row for row in rows if row.get("activity_state") == "REST"]
        selected = resting or list(rows)
    else:
        selected = list(rows)
    values = _numeric_values(selected, feature, column)
    # Event-derived zeroes mean no event occurred in that row, not a normal
    # cadence/interval/contact duration measurement.
    if feature in {"cadence", "step_interval", "gait_cycle_duration"}:
        values = [value for value in values if value > 0]
    return values


def _remove_outliers(values: Sequence[float], rule: float | None) -> list[float]:
    if rule is None or len(values) < 3:
        return list(values)
    center = fmean(values)
    spread = pstdev(values)
    if spread == 0:
        return list(values)
    return [value for value in values if abs(value - center) <= rule * spread]


def _statistics(values: Sequence[float], outlier_rule: float | None) -> FeatureStatistics | None:
    cleaned = _remove_outliers(values, outlier_rule)
    if not cleaned:
        return None
    return FeatureStatistics(
        mean=fmean(cleaned),
        std=pstdev(cleaned),
        minimum=min(cleaned),
        maximum=max(cleaned),
        count=len(cleaned),
    )


def build_baseline(
    processed_data: str | Path | Sequence[Mapping[str, Any]],
    athlete_id: str,
    *,
    duration_seconds: float | None = None,
    minimum_samples: int = SimulationConfig.min_baseline_samples,
    baseline_version: str = SimulationConfig.baseline_version,
    outlier_rule: float | None = SimulationConfig.outlier_rule,
) -> PersonalBaseline:
    """Build a baseline from processed rows without assigning risk levels."""
    if not athlete_id.strip():
        raise ValueError("athlete_id must not be empty")
    rows = _read_rows(processed_data)
    if len(rows) < minimum_samples:
        raise ValueError(
            f"Insufficient baseline data: {len(rows)} samples; "
            f"minimum is {minimum_samples}"
        )
    if "timestamp" not in rows[0]:
        raise ValueError("Processed dataset is missing required column: timestamp")
    features: dict[str, FeatureStatistics] = {}
    for feature, column in FEATURE_COLUMNS.items():
        values = _select_values(rows, feature, column)
        statistics = _statistics(values, outlier_rule)
        if statistics is not None:
            features[feature] = statistics
    if not features:
        raise ValueError("No valid numeric baseline features were found")
    inferred_duration = _duration_seconds(rows)
    return PersonalBaseline(
        athlete_id=athlete_id,
        created_at=datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z"),
        duration_seconds=inferred_duration if duration_seconds is None else duration_seconds,
        sample_count=len(rows),
        baseline_version=baseline_version,
        features=features,
        calibration_activity={
            "physiological": "REST where available, otherwise all samples",
            "gait": "WALKING where available, otherwise all samples",
            "outlier_rule": "Remove values beyond N population standard deviations" if outlier_rule else "Disabled",
        },
    )


def _duration_seconds(rows: Sequence[Mapping[str, Any]]) -> float:
    try:
        start = datetime.fromisoformat(str(rows[0]["timestamp"]).replace("Z", "+00:00"))
        end = datetime.fromisoformat(str(rows[-1]["timestamp"]).replace("Z", "+00:00"))
        return max(0.0, (end - start).total_seconds())
    except (KeyError, TypeError, ValueError):
        return 0.0

