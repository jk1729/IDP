"""Convert baseline deviations into bounded component scores."""

from __future__ import annotations

import math
from typing import Any, Mapping

from baseline.baseline_comparison import compare_to_baseline
from baseline.baseline_model import PersonalBaseline

from .config import ZERO_STD_EPSILON


def normalize_deviation(z_score: float | None, scale: float = 3.0) -> float:
    """Map absolute z-score smoothly to 0-100, with three z as the upper anchor."""
    if z_score is None or not math.isfinite(float(z_score)) or scale <= 0:
        return 0.0
    magnitude = abs(float(z_score))
    return max(0.0, min(100.0, magnitude / scale * 100.0))


def _z_scores(current: Mapping[str, Any], baseline: PersonalBaseline, names: tuple[str, ...]) -> dict[str, float]:
    comparisons = compare_to_baseline(current, baseline, ZERO_STD_EPSILON)
    allowed = set(names)
    return {item.feature: item.z_score for item in comparisons if item.feature in allowed}


def _combined_score(z_scores: dict[str, float]) -> float:
    return normalize_deviation(max((abs(value) for value in z_scores.values()), default=0.0))


def calculate_pressure_deviation(current: Mapping[str, Any], baseline: PersonalBaseline) -> float:
    """Score loading-distribution deviation, not calibrated physical pressure."""
    return _combined_score(_z_scores(
        current, baseline,
        ("heel_percentage", "forefoot_percentage", "loading_imbalance", "total_loading"),
    ))


def calculate_gait_motion_score(current: Mapping[str, Any], baseline: PersonalBaseline) -> float:
    """Score gyroscope-derived gait-motion deviation."""
    return _combined_score(_z_scores(current, baseline, ("angular_motion_mean", "angular_motion_peak")))


def calculate_impact_score(current: Mapping[str, Any], baseline: PersonalBaseline) -> float:
    return _combined_score(_z_scores(current, baseline, ("impact_peak", "impact_mean")))


def calculate_gait_cycle_score(current: Mapping[str, Any], baseline: PersonalBaseline) -> float:
    return _combined_score(_z_scores(
        current, baseline, ("cadence", "step_interval", "gait_cycle_duration", "contact_duration"),
    ))


def calculate_risk_score(
    pressure_score: float,
    gait_motion_score: float,
    impact_score: float,
    gait_cycle_score: float,
) -> float:
    from .config import (
        GAIT_CYCLE_WEIGHT, GAIT_MOTION_WEIGHT, IMPACT_WEIGHT, PRESSURE_WEIGHT,
    )
    values = [pressure_score, gait_motion_score, impact_score, gait_cycle_score]
    if not all(math.isfinite(float(value)) for value in values):
        raise ValueError("Risk component scores must be finite")
    score = (
        PRESSURE_WEIGHT * max(0.0, min(100.0, pressure_score))
        + GAIT_MOTION_WEIGHT * max(0.0, min(100.0, gait_motion_score))
        + IMPACT_WEIGHT * max(0.0, min(100.0, impact_score))
        + GAIT_CYCLE_WEIGHT * max(0.0, min(100.0, gait_cycle_score))
    )
    return max(0.0, min(100.0, score))


def classify_risk(score: float) -> str:
    from .config import LOW_RISK_MAX, MODERATE_RISK_MAX
    if score <= LOW_RISK_MAX:
        return "LOW"
    if score <= MODERATE_RISK_MAX:
        return "MODERATE"
    return "HIGH"

