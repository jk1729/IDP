"""In-memory event and risk-transition tracking."""

from dataclasses import dataclass, field
from datetime import datetime

from .schemas import RiskEvent


@dataclass
class RiskTransitionTracker:
    previous_level: str | None = None
    events: list[RiskEvent] = field(default_factory=list)

    def update(self, level: str, score: float, timestamp: str) -> list[RiskEvent]:
        if self.previous_level is None:
            self.previous_level = level
            return []
        events: list[RiskEvent] = []
        if level != self.previous_level:
            direction = "RISK_INCREASED" if _rank(level) > _rank(self.previous_level) else "RISK_NORMALIZED"
            events.append(RiskEvent(timestamp, direction, "SYSTEM", score,
                                    f"Risk changed from {self.previous_level} to {level}"))
            self.events.extend(events)
        self.previous_level = level
        return events


def _rank(level: str) -> int:
    return {
        "LOW": 0,
        "NORMAL": 0,
        "MODERATE": 1,
        "CAUTION": 1,
        "HIGH": 2,
        "ALERT": 2,
    }.get(level, 0)
