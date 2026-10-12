"""Raw sensor contract shared with the Phase 1 processing pipeline."""

from dataclasses import asdict, dataclass
from enum import Enum
from typing import Any, Dict

from analytics.models import RawSensorData


class ActivityState(str, Enum):
    REST = "REST"
    STANDING = "STANDING"
    WALKING = "WALKING"
    EXERCISE = "EXERCISE"
    ABNORMAL_WALKING = "ABNORMAL_WALKING"


@dataclass
class SensorReading(RawSensorData):
    """One raw reading; FSR values are ADC/relative values, never Newtons."""

    activity_state: ActivityState = ActivityState.REST

    @property
    def heel_fsr(self) -> int:
        return self.heel_fsr_raw

    @property
    def forefoot_fsr(self) -> int:
        return self.forefoot_fsr_raw

    def as_dict(self) -> Dict[str, Any]:
        result = asdict(self)
        result["activity_state"] = self.activity_state.value
        return result
