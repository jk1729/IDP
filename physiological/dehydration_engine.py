"""Prototype personalized dehydration-risk estimation."""

from __future__ import annotations

import math
from typing import Any, Mapping

from .config import (
    HR_WEIGHT,
    MODEL_VERSION,
    TEMPERATURE_WEIGHT,
    ZERO_STD_EPSILON,
)
from .feature_processing import calculate_current_physiology
from .persistence import PhysiologicalPersistenceTracker
from .schemas import DehydrationRiskResult, PhysiologicalBaseline, PhysiologicalRiskLevel
from .signal_generator import generate_physiological_signals


def calculate_dehydration_score(
    hr_current: float,
    temperature_current: float,
    hr_base: float,
    temperature_base: float,
) -> tuple[float, float]:
    delta_hr = hr_current - hr_base
    delta_temperature = temperature_current - temperature_base
    raw_score = (delta_hr * HR_WEIGHT) + (delta_temperature * TEMPERATURE_WEIGHT)
    if not math.isfinite(raw_score):
        raise ValueError("Physiological score inputs must produce a finite value")
    raw_score = round(raw_score, 10)
    return raw_score, max(0.0, min(100.0, raw_score))


def _z_score(current: float, baseline: float, standard_deviation: float) -> float:
    if not all(math.isfinite(value) for value in (current, baseline, standard_deviation)):
        return 0.0
    if standard_deviation <= ZERO_STD_EPSILON:
        return 0.0
    return (current - baseline) / standard_deviation


def classify_dehydration_risk(score: float) -> PhysiologicalRiskLevel:
    from .config import LOW_RISK_MAX, MODERATE_RISK_MAX
    if score <= LOW_RISK_MAX:
        return PhysiologicalRiskLevel.LOW
    if score <= MODERATE_RISK_MAX:
        return PhysiologicalRiskLevel.MODERATE
    return PhysiologicalRiskLevel.HIGH


def evaluate_dehydration_risk(
    current: Mapping[str, Any],
    baseline: PhysiologicalBaseline,
    tracker: PhysiologicalPersistenceTracker | None = None,
) -> DehydrationRiskResult:
    hr_current = float(current["hr_current"])
    temperature_current = float(current["temperature_current"])
    raw_score, score = calculate_dehydration_score(
        hr_current, temperature_current, baseline.hr_base, baseline.temperature_base,
    )
    level = classify_dehydration_risk(score)
    hr_delta = hr_current - baseline.hr_base
    temperature_delta = temperature_current - baseline.temperature_base
    signals = generate_physiological_signals(
        hr_delta, temperature_delta, str(current.get("hr_trend", "STABLE")),
        str(current.get("temperature_trend", "STABLE")),
    )
    tracker = tracker or PhysiologicalPersistenceTracker()
    persistent = tracker.update(level is PhysiologicalRiskLevel.HIGH)
    alert = level is PhysiologicalRiskLevel.HIGH and persistent
    return DehydrationRiskResult(
        risk_score=round(score, 2),
        risk_level=level,
        heart_rate={
            "current": hr_current, "baseline": baseline.hr_base, "delta": hr_delta,
            "z_score": _z_score(hr_current, baseline.hr_base, baseline.hr_std),
        },
        temperature={
            "current": temperature_current, "baseline": baseline.temperature_base,
            "delta": temperature_delta,
            "z_score": _z_score(temperature_current, baseline.temperature_base, baseline.temperature_std),
        },
        activity_state=str(current.get("activity_state", "UNKNOWN")),
        hr_trend=str(current.get("hr_trend", "STABLE")),
        temperature_trend=str(current.get("temperature_trend", "STABLE")),
        raw_score=raw_score,
        persistent=persistent,
        consecutive_high_windows=tracker.consecutive_high_windows,
        signals=signals,
        physiological_alert=alert,
        alert_requested=alert,
        model_version=MODEL_VERSION,
    )
