"""Typed physiological baseline, signal, and result models."""

from dataclasses import asdict, dataclass
from enum import Enum
from typing import Any


class PhysiologicalRiskLevel(str, Enum):
    LOW = "LOW"
    MODERATE = "MODERATE"
    HIGH = "HIGH"


@dataclass
class PhysiologicalBaseline:
    athlete_id: str
    hr_base: float
    hr_std: float
    temperature_base: float
    temperature_std: float
    baseline_duration_seconds: float
    sample_count: int
    created_at: str
    baseline_version: str

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> "PhysiologicalBaseline":
        return cls(
            athlete_id=value["athlete_id"],
            hr_base=float(value["hr_base"]),
            hr_std=float(value["hr_std"]),
            temperature_base=float(value["temperature_base"]),
            temperature_std=float(value["temperature_std"]),
            baseline_duration_seconds=float(value["baseline_duration_seconds"]),
            sample_count=int(value["sample_count"]),
            created_at=value["created_at"],
            baseline_version=value["baseline_version"],
        )


@dataclass
class PhysiologicalSignal:
    signal: str
    feature: str
    delta: float
    severity: str


@dataclass
class DehydrationRiskResult:
    risk_score: float
    risk_level: PhysiologicalRiskLevel
    heart_rate: dict[str, float]
    temperature: dict[str, float]
    activity_state: str
    hr_trend: str
    temperature_trend: str
    raw_score: float
    persistent: bool
    consecutive_high_windows: int
    signals: list[PhysiologicalSignal]
    physiological_alert: bool
    alert_requested: bool
    model_version: str

    def to_dict(self) -> dict[str, Any]:
        result = asdict(self)
        result["risk_level"] = self.risk_level.value
        return result

