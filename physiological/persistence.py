"""Persistence tracking for consecutive high physiological-risk windows."""

from dataclasses import dataclass

from .config import REQUIRED_CONSECUTIVE_WINDOWS


@dataclass
class PhysiologicalPersistenceTracker:
    required_consecutive_windows: int = REQUIRED_CONSECUTIVE_WINDOWS
    consecutive_high_windows: int = 0
    persistent_high: bool = False

    def update(self, high_risk: bool) -> bool:
        if high_risk:
            self.consecutive_high_windows += 1
        else:
            self.consecutive_high_windows = 0
            self.persistent_high = False
        if self.consecutive_high_windows >= self.required_consecutive_windows:
            self.persistent_high = True
        return self.persistent_high

