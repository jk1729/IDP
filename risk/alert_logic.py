"""Unified alert classification and cooldown logic."""

from dataclasses import dataclass
from datetime import datetime

from .schemas import AlertPriority, AlertState, AlertType, SystemStatus


ALERT_COOLDOWN_SECONDS = 30.0


def should_alert(injury_high: bool, injury_persistent: bool,
                 physiological_high: bool, physiological_persistent: bool) -> bool:
    return (injury_high and injury_persistent) or (physiological_high and physiological_persistent)


def determine_alert_type(injury_alert: bool, physiological_alert: bool) -> AlertType:
    if injury_alert and physiological_alert:
        return AlertType.BOTH
    if injury_alert:
        return AlertType.INJURY_RISK
    if physiological_alert:
        return AlertType.PHYSIOLOGICAL_RISK
    return AlertType.NONE


def determine_status(injury_level: str, physiological_level: str,
                     alert: bool) -> SystemStatus:
    if alert:
        return SystemStatus.ALERT
    if injury_level != "LOW" or physiological_level != "LOW":
        return SystemStatus.CAUTION
    return SystemStatus.NORMAL


def determine_priority(alert: bool, persistent: bool, moderate: bool) -> AlertPriority:
    if not alert and not moderate:
        return AlertPriority.NONE
    if alert and persistent:
        return AlertPriority.HIGH
    return AlertPriority.MEDIUM


@dataclass
class AlertCooldown:
    cooldown_seconds: float = ALERT_COOLDOWN_SECONDS
    last_alert_time: datetime | None = None

    def available(self, now: datetime) -> bool:
        return self.last_alert_time is None or (
            now - self.last_alert_time
        ).total_seconds() >= self.cooldown_seconds

    def record(self, now: datetime) -> None:
        self.last_alert_time = now


@dataclass
class AlertStateManager:
    cooldown: AlertCooldown = None
    state: AlertState = AlertState.IDLE
    acknowledged: bool = False

    def __post_init__(self) -> None:
        if self.cooldown is None:
            self.cooldown = AlertCooldown()

    def update(self, status: SystemStatus, alert: bool, now: datetime) -> tuple[bool, AlertState]:
        if alert:
            if self.cooldown.available(now):
                self.cooldown.record(now)
                self.acknowledged = False
                self.state = AlertState.ALERT
                return True, self.state
            self.state = AlertState.COOLDOWN
            return False, self.state
        self.state = AlertState.WARNING if status is SystemStatus.CAUTION else AlertState.IDLE
        return False, self.state

    def acknowledge_alert(self) -> None:
        self.acknowledged = True

