"""Typed result models for risk estimation."""

from dataclasses import asdict, dataclass, field
from enum import Enum
from typing import Any


class RiskLevel(str, Enum):
    LOW = "LOW"
    MODERATE = "MODERATE"
    HIGH = "HIGH"


@dataclass
class RiskSignal:
    signal: str
    severity: str
    feature: str
    z_score: float


@dataclass
class RiskResult:
    risk_score: float
    risk_level: RiskLevel
    components: dict[str, float]
    signals: list[RiskSignal]
    abnormal: bool
    persistent_abnormality: bool
    consecutive_abnormal_cycles: int
    vibration_alert: bool
    model_version: str

    def to_dict(self) -> dict[str, Any]:
        result = asdict(self)
        result["risk_level"] = self.risk_level.value
        return result

