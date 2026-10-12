"""Combines independent feature groups into the Phase 3 output model."""

from dataclasses import asdict, dataclass

from .fsr_processing import FSRFeatures
from .gait_processing import GaitFeatures
from .imu_processing import IMUFeatures
from .physiological_processing import PhysiologicalFeatures


@dataclass
class ProcessedFeatures:
    timestamp: str
    activity_state: str
    heel_filtered: float
    forefoot_filtered: float
    total_loading: float
    heel_percentage: float
    forefoot_percentage: float
    loading_difference: float
    loading_ratio: float
    loading_imbalance: float
    loading_distribution: str
    accel_magnitude: float
    accel_magnitude_filtered: float
    gyro_magnitude: float
    gyro_magnitude_filtered: float
    dynamic_acceleration: float
    step_detected: bool
    step_interval: float
    cadence: float
    gait_cycle_duration: float
    contact_state: str
    contact_duration: float
    no_contact_duration: float
    contact_ratio: float
    impact_peak: float
    impact_mean: float
    impact_std: float
    angular_motion_mean: float
    angular_motion_peak: float
    angular_motion_std: float
    hr_filtered: float
    hr_mean: float
    hr_min: float
    hr_max: float
    hr_std: float
    hr_trend: float
    temperature_filtered: float
    temperature_mean: float
    temperature_min: float
    temperature_max: float
    temperature_std: float
    temperature_trend: float


def combine(timestamp: str, activity_state: str, fsr: FSRFeatures, imu: IMUFeatures,
            gait: GaitFeatures, physiology: PhysiologicalFeatures, distribution: str,
            impact: dict[str, float], angular: dict[str, float]) -> ProcessedFeatures:
    return ProcessedFeatures(
        timestamp, activity_state, fsr.heel_filtered, fsr.forefoot_filtered, fsr.total_loading,
        fsr.heel_percentage, fsr.forefoot_percentage, fsr.loading_difference, fsr.loading_ratio,
        fsr.loading_imbalance, distribution, imu.accel_magnitude, imu.accel_magnitude_filtered,
        imu.gyro_magnitude, imu.gyro_magnitude_filtered, imu.dynamic_acceleration,
        gait.step_detected, gait.step_interval, gait.cadence, gait.gait_cycle_duration,
        gait.contact_state, gait.contact_duration, gait.no_contact_duration, gait.contact_ratio,
        impact["impact_peak"], impact["impact_mean"], impact["impact_std"],
        angular["angular_motion_mean"], angular["angular_motion_peak"], angular["angular_motion_std"],
        physiology.hr_filtered, physiology.hr_mean, physiology.hr_min, physiology.hr_max,
        physiology.hr_std, physiology.hr_trend, physiology.temperature_filtered,
        physiology.temperature_mean, physiology.temperature_min, physiology.temperature_max,
        physiology.temperature_std, physiology.temperature_trend,
    )

