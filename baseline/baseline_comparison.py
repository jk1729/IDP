"""Current-feature versus baseline comparison; no risk classification."""

from __future__ import annotations

import math
from typing import Any, Mapping, Sequence

from sensor_simulator.config import SimulationConfig

from .baseline_model import BaselineComparison, PersonalBaseline


def compare_to_baseline(
    current_features: Mapping[str, Any],
    baseline: PersonalBaseline,
    zero_std_epsilon: float = SimulationConfig.zero_std_epsilon,
) -> list[BaselineComparison]:
    """Calculate absolute, percentage, and standardized deviations."""
    results = []
    for feature, statistics in baseline.features.items():
        if feature not in current_features or current_features[feature] in (None, ""):
            continue
        current = float(current_features[feature])
        if not math.isfinite(current):
            continue
        absolute = abs(current - statistics.mean)
        percentage = absolute / abs(statistics.mean) * 100.0 if abs(statistics.mean) > zero_std_epsilon else None
        z_score = (
            (current - statistics.mean) / statistics.std
            if statistics.std > zero_std_epsilon else 0.0
        )
        results.append(BaselineComparison(
            feature=feature,
            current_value=current,
            baseline_mean=statistics.mean,
            baseline_std=statistics.std,
            absolute_deviation=absolute,
            percentage_deviation=percentage,
            z_score=z_score,
        ))
    return results


def session_feature_means(rows: Sequence[Mapping[str, Any]]) -> dict[str, float]:
    """Aggregate a current processed session into comparable feature values."""
    output: dict[str, float] = {}
    for feature, column in {
        "heel_filtered": "heel_filtered", "forefoot_filtered": "forefoot_filtered",
        "total_loading": "total_loading", "heel_percentage": "heel_percentage",
        "forefoot_percentage": "forefoot_percentage", "loading_imbalance": "loading_imbalance",
        "cadence": "cadence", "step_interval": "step_interval",
        "gait_cycle_duration": "gait_cycle_duration", "contact_duration": "contact_duration",
        "contact_ratio": "contact_ratio", "impact_peak": "impact_peak",
        "impact_mean": "impact_mean", "angular_motion_mean": "angular_motion_mean",
        "angular_motion_peak": "angular_motion_peak", "heart_rate": "hr_filtered",
        "temperature": "temperature_filtered",
    }.items():
        values = []
        for row in rows:
            try:
                value = float(row[column])
                if math.isfinite(value) and (feature not in {"cadence", "step_interval", "gait_cycle_duration"} or value > 0):
                    values.append(value)
            except (KeyError, TypeError, ValueError):
                continue
        if values:
            output[feature] = sum(values) / len(values)
    return output

