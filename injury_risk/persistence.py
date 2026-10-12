"""Consecutive-cycle persistence tracking."""

from dataclasses import dataclass

from .config import REQUIRED_CONSECUTIVE_CYCLES


@dataclass
class PersistenceTracker:
    required_consecutive_cycles: int = REQUIRED_CONSECUTIVE_CYCLES
    consecutive_abnormal_cycles: int = 0
    persistent_abnormality: bool = False

    def update(self, abnormal: bool) -> bool:
        if abnormal:
            self.consecutive_abnormal_cycles += 1
        else:
            self.consecutive_abnormal_cycles = 0
            self.persistent_abnormality = False
        if self.consecutive_abnormal_cycles >= self.required_consecutive_cycles:
            self.persistent_abnormality = True
        return self.persistent_abnormality

