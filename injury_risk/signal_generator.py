"""Generate explanatory signals from baseline comparison z-scores."""

from __future__ import annotations

from typing import Mapping

from .config import ABNORMAL_Z_THRESHOLD
from .schemas import RiskSignal


SIGNAL_NAMES = {
    "heel_percentage": "heel loading deviation",
    "forefoot_percentage": "forefoot loading deviation",
    "loading_imbalance": "loading imbalance",
    "total_loading": "total loading deviation",
    "angular_motion_mean": "increased gait-motion deviation",
    "angular_motion_peak": "increased gait-motion deviation",
    "impact_peak": "increased impact deviation",
    "impact_mean": "increased impact deviation",
    "cadence": "gait-cycle deviation",
    "step_interval": "gait-cycle deviation",
    "gait_cycle_duration": "gait-cycle deviation",
    "contact_duration": "gait-cycle deviation",
}


def _severity(z_score: float) -> str:
    magnitude = abs(z_score)
    return "high" if magnitude >= 3.0 else "moderate" if magnitude >= 2.0 else "low"


def generate_risk_signals(
    deviations: Mapping[str, float],
    abnormal_z_threshold: float = ABNORMAL_Z_THRESHOLD,
) -> list[RiskSignal]:
    signals: list[RiskSignal] = []
    emitted: set[str] = set()
    for feature, z_score in deviations.items():
        if feature not in SIGNAL_NAMES or abs(z_score) < abnormal_z_threshold:
            continue
        signal = SIGNAL_NAMES[feature]
        if signal in emitted:
            continue
        emitted.add(signal)
        signals.append(RiskSignal(signal, _severity(z_score), feature, z_score))
    return signals

