"""Canonical data contracts shared by simulation, firmware, Android, and analytics."""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List


SCHEMA_VERSION = "1.0"


class RiskState(str, Enum):
    LOW = "LOW"
    MODERATE = "MODERATE"
    HIGH = "HIGH"


class PersistenceStatus(str, Enum):
    NONE = "NONE"
    OBSERVED = "OBSERVED"
    PERSISTING = "PERSISTING"
    CLEARED = "CLEARED"


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


@dataclass
class RawSensorData:
    timestamp: str
    heel_fsr_raw: int
    forefoot_fsr_raw: int
    accel_x_mps2: float
    accel_y_mps2: float
    accel_z_mps2: float
    gyro_x_dps: float
    gyro_y_dps: float
    gyro_z_dps: float
    heart_rate_bpm: float
    temperature_c: float
    device_id: str = "SIMULATED_INSOLE_001"
    data_source: str = "SIMULATED_DATA"
    schema_version: str = SCHEMA_VERSION


@dataclass
class ProcessedSensorData:
    heel_loading_filtered: float
    forefoot_loading_filtered: float
    total_loading: float
    heel_percentage: float
    forefoot_percentage: float
    heel_forefoot_imbalance: float
    acceleration_magnitude_mps2: float
    acceleration_filtered_mps2: float
    step_count: int
    cadence_steps_per_min: float
    gait_cycle_duration_s: float
    stance_time_s: float
    impact_mps2: float
    angular_motion_dps: float
    gait_angle_deg: float
    heart_rate_filtered_bpm: float
    temperature_filtered_c: float
    hr_deviation_bpm: float = 0.0
    temperature_deviation_c: float = 0.0


@dataclass
class PersonalBaseline:
    baseline_id: str
    created_at: str
    duration_seconds: int
    activity_type: str
    sample_count: int
    hr_mean: float
    hr_stddev: float
    temperature_mean: float
    temperature_stddev: float
    heel_loading_mean: float
    heel_loading_stddev: float
    forefoot_loading_mean: float
    forefoot_loading_stddev: float
    total_loading_mean: float
    total_loading_stddev: float
    heel_percentage_mean: float
    heel_percentage_stddev: float
    forefoot_percentage_mean: float
    forefoot_percentage_stddev: float
    cadence_mean: float
    cadence_stddev: float
    gait_cycle_mean: float
    gait_cycle_stddev: float
    stance_time_mean: float
    stance_time_stddev: float
    impact_mean: float
    impact_stddev: float
    gait_angle_mean: float
    gait_angle_stddev: float
    quality_score: float
    valid: bool


@dataclass
class InjuryRiskFeatures:
    loading_deviation_raw: float
    loading_deviation_score: float
    gait_angle_deviation_raw: float
    gait_angle_deviation_score: float
    impact_deviation_raw: float
    impact_deviation_score: float
    gait_cycle_deviation_raw: float
    gait_cycle_abnormality_score: float


@dataclass
class PhysiologicalRiskFeatures:
    hr_base_bpm: float
    temperature_base_c: float
    hr_current_bpm: float
    temperature_current_c: float
    hr_delta_bpm: float
    temperature_delta_c: float
    dehydration_risk_score_raw: float
    dehydration_risk_score_normalized: float
    heuristic_name: str = "Prototype dehydration-risk heuristic"
    heuristic_version: str = "0.1"


@dataclass
class RiskResult:
    timestamp: str
    injury_risk_score: float
    physiological_risk_score: float
    injury_risk_state: RiskState
    physiological_risk_state: RiskState
    abnormality_detected: bool
    abnormality_types: List[str]
    persistence_status: PersistenceStatus
    persistence_duration_s: float
    vibration_alert: bool
    vibration_reason: str
    model_version: str = "prototype-0.1"


def to_dict(value: Any) -> Dict[str, Any]:
    """Convert dataclasses and enums into JSON-compatible dictionaries."""
    result = asdict(value)
    return _normalize(result)


def _normalize(value: Any) -> Any:
    if isinstance(value, Enum):
        return value.value
    if isinstance(value, dict):
        return {key: _normalize(item) for key, item in value.items()}
    if isinstance(value, list):
        return [_normalize(item) for item in value]
    return value
