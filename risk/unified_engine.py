"""Combines independent Phase 5 and Phase 6 results without recalculating them."""

from __future__ import annotations

import math
from datetime import datetime, timezone

from injury_risk.schemas import RiskResult
from physiological.schemas import DehydrationRiskResult

from .alert_logic import (
    AlertStateManager, determine_alert_type, determine_priority,
    determine_status, should_alert,
)
from .messages import message_for
from .persistence import RiskTransitionTracker
from .schemas import UnifiedRiskResult, UnifiedSignal


MODEL_VERSION = "unified-risk-v1"


def _score(value: float, name: str) -> float:
    try:
        number = float(value)
    except (TypeError, ValueError) as error:
        raise ValueError(f"{name} score is missing or invalid") from error
    if not math.isfinite(number):
        raise ValueError(f"{name} score must be finite")
    return max(0.0, min(100.0, number))


def _timestamp(injury: RiskResult, physiological: DehydrationRiskResult) -> str:
    for result in (injury, physiological):
        value = getattr(result, "timestamp", None)
        if value:
            return str(value)
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def _signals(injury: RiskResult, physiological: DehydrationRiskResult) -> list[UnifiedSignal]:
    output: list[UnifiedSignal] = []
    seen: set[tuple[str, str]] = set()
    for source, signals in (("INJURY", injury.signals), ("PHYSIOLOGICAL", physiological.signals)):
        for signal in signals:
            name = getattr(signal, "signal", str(signal))
            key = (source, name)
            if key not in seen:
                output.append(UnifiedSignal(source, name, getattr(signal, "severity", "unknown"),
                                            getattr(signal, "feature", "")))
                seen.add(key)
    return output


def evaluate_system_risk(
    injury_result: RiskResult,
    physiological_result: DehydrationRiskResult,
    *,
    state_manager: AlertStateManager | None = None,
    transition_tracker: RiskTransitionTracker | None = None,
    now: datetime | None = None,
) -> UnifiedRiskResult:
    if injury_result is None or physiological_result is None:
        raise ValueError("Both injury and physiological results are required")
    injury_score = _score(injury_result.risk_score, "injury")
    physiological_score = _score(physiological_result.risk_score, "physiological")
    overall = max(injury_score, physiological_score)
    injury_level = getattr(injury_result.risk_level, "value", str(injury_result.risk_level))
    physiological_level = getattr(physiological_result.risk_level, "value", str(physiological_result.risk_level))
    injury_alert = injury_level == "HIGH" and bool(injury_result.persistent_abnormality)
    physiological_alert = physiological_level == "HIGH" and bool(physiological_result.persistent)
    alert = should_alert(injury_level == "HIGH", bool(injury_result.persistent_abnormality),
                         physiological_level == "HIGH", bool(physiological_result.persistent))
    status = determine_status(injury_level, physiological_level, alert)
    alert_type = determine_alert_type(injury_alert, physiological_alert)
    persistent = injury_alert or physiological_alert
    priority = determine_priority(alert, persistent, status is not status.NORMAL)
    timestamp = _timestamp(injury_result, physiological_result)
    state_manager = state_manager or AlertStateManager()
    triggered, state = state_manager.update(status, alert, now or datetime.now(timezone.utc))
    tracker = transition_tracker or RiskTransitionTracker()
    tracker.update(getattr(status, "value", str(status)), overall, timestamp)
    return UnifiedRiskResult(
        timestamp=timestamp,
        injury_risk_score=injury_score,
        injury_risk_level=injury_level,
        injury_persistent=bool(injury_result.persistent_abnormality),
        physiological_risk_score=physiological_score,
        physiological_risk_level=physiological_level,
        physiological_persistent=bool(physiological_result.persistent),
        overall_risk_score=overall,
        overall_risk_level="HIGH" if overall >= 70 else "MODERATE" if overall >= 40 else "LOW",
        system_status=status,
        alert=alert,
        alert_type=alert_type,
        alert_priority=priority,
        message=message_for(alert_type, alert),
        signals=_signals(injury_result, physiological_result),
        vibration_request=triggered,
        alert_state=state,
        model_version=MODEL_VERSION,
    )

