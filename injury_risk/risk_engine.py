"""Pure-Python prototype injury-risk estimation orchestration."""

from __future__ import annotations

from typing import Any, Mapping

from baseline.baseline_comparison import compare_to_baseline
from baseline.baseline_model import PersonalBaseline

from .config import ABNORMAL_Z_THRESHOLD, MODEL_VERSION, REQUIRED_CONSECUTIVE_CYCLES
from .feature_scoring import (
    calculate_gait_cycle_score,
    calculate_gait_motion_score,
    calculate_impact_score,
    calculate_pressure_deviation,
    calculate_risk_score,
    classify_risk,
)
from .persistence import PersistenceTracker
from .schemas import RiskLevel, RiskResult
from .signal_generator import generate_risk_signals


def evaluate_injury_risk(
    current_features: Mapping[str, Any],
    baseline: PersonalBaseline,
    persistence_tracker: PersistenceTracker | None = None,
) -> RiskResult:
    """Evaluate abnormal gait/loading patterns without diagnosing injury."""
    pressure = calculate_pressure_deviation(current_features, baseline)
    gait_motion = calculate_gait_motion_score(current_features, baseline)
    impact = calculate_impact_score(current_features, baseline)
    gait_cycle = calculate_gait_cycle_score(current_features, baseline)
    score = calculate_risk_score(pressure, gait_motion, impact, gait_cycle)
    comparison = compare_to_baseline(current_features, baseline)
    deviations = {item.feature: item.z_score for item in comparison}
    signals = generate_risk_signals(deviations)
    abnormal = any(abs(value) >= ABNORMAL_Z_THRESHOLD for value in deviations.values())
    tracker = persistence_tracker or PersistenceTracker(REQUIRED_CONSECUTIVE_CYCLES)
    persistent = tracker.update(abnormal)
    level = RiskLevel(classify_risk(score))
    return RiskResult(
        risk_score=round(score, 2),
        risk_level=level,
        components={
            "pressure": round(pressure, 2),
            "gait_motion": round(gait_motion, 2),
            "impact": round(impact, 2),
            "gait_cycle": round(gait_cycle, 2),
        },
        signals=signals,
        abnormal=abnormal,
        persistent_abnormality=persistent,
        consecutive_abnormal_cycles=tracker.consecutive_abnormal_cycles,
        vibration_alert=level is RiskLevel.HIGH and persistent,
        model_version=MODEL_VERSION,
    )

