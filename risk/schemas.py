"""Typed, JSON-compatible system-level risk models."""

from dataclasses import asdict, dataclass
from enum import Enum
from typing import Any


class SystemStatus(str, Enum):
    NORMAL = "NORMAL"
    CAUTION = "CAUTION"
    ALERT = "ALERT"


class AlertType(str, Enum):
    NONE = "NONE"
    INJURY_RISK = "INJURY_RISK"
    PHYSIOLOGICAL_RISK = "PHYSIOLOGICAL_RISK"
    BOTH = "BOTH"


class AlertPriority(str, Enum):
    NONE = "NONE"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class AlertState(str, Enum):
    IDLE = "IDLE"
    WARNING = "WARNING"
    ALERT = "ALERT"
    COOLDOWN = "COOLDOWN"


@dataclass
class UnifiedSignal:
    source: str
    signal: str
    severity: str
    feature: str = ""


@dataclass
class AlertCommand:
    vibration_request: bool
    alert_type: AlertType
    priority: AlertPriority
    duration_seconds: float
    timestamp: str


@dataclass
class RiskEvent:
    timestamp: str
    event_type: str
    source: str
    risk_score: float
    message: str
    acknowledged: bool = False


@dataclass
class UnifiedRiskResult:
    timestamp: str
    injury_risk_score: float
    injury_risk_level: str
    injury_persistent: bool
    physiological_risk_score: float
    physiological_risk_level: str
    physiological_persistent: bool
    overall_risk_score: float
    overall_risk_level: str
    system_status: SystemStatus
    alert: bool
    alert_type: AlertType
    alert_priority: AlertPriority
    message: str
    signals: list[UnifiedSignal]
    vibration_request: bool
    alert_state: AlertState
    model_version: str

    def to_dict(self) -> dict[str, Any]:
        result = asdict(self)
        for key in ("system_status", "alert_type", "alert_priority", "alert_state"):
            result[key] = result[key].value
        return result

