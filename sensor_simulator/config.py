"""Configuration for reproducible Phase 2 simulation runs."""

from dataclasses import dataclass


@dataclass(frozen=True)
class SimulationConfig:
    duration_seconds: float = 60.0
    sample_interval_seconds: float = 0.1
    seed: int = 42
    fsr_min: int = 0
    fsr_max: int = 4095
    heart_rate_min_bpm: float = 40.0
    heart_rate_max_bpm: float = 220.0
    temperature_min_c: float = 15.0
    temperature_max_c: float = 45.0
    output_path: str = "sensor_simulator/output/simulated_data.csv"

    # Phase 3 processing defaults are centralized here so changing the sample rate
    # does not require editing multiple processing modules.
    fsr_filter_window: int = 3
    imu_filter_window: int = 3
    processing_window_seconds: float = 5.0
    contact_threshold: float = 500.0
    step_threshold: float = 1.0
    min_step_interval: float = 0.3
    min_baseline_samples: int = 30
    baseline_version: str = "baseline-v1"
    outlier_rule: float | None = 4.0
    zero_std_epsilon: float = 1e-9

    @property
    def sample_count(self) -> int:
        return max(1, int(round(self.duration_seconds / self.sample_interval_seconds)))
