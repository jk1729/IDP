"""Filtered heart-rate and temperature summaries without medical thresholds."""

from dataclasses import dataclass
from statistics import fmean, pstdev
from typing import Sequence

from .filters import linear_trend, moving_average, validate_numeric


@dataclass
class PhysiologicalFeatures:
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


def process_physiology(hr: Sequence[float], temperature: Sequence[float], window_size: int) -> list[PhysiologicalFeatures]:
    if len(hr) != len(temperature):
        raise ValueError("Heart-rate and temperature sequences must have equal length")
    validate_numeric(hr, "heart_rate")
    validate_numeric(temperature, "temperature")
    hr_filtered = moving_average(hr, window_size)
    temp_filtered = moving_average(temperature, window_size)
    return [
        PhysiologicalFeatures(
            h, fmean(hr_filtered[:index + 1]), min(hr_filtered[:index + 1]), max(hr_filtered[:index + 1]),
            pstdev(hr_filtered[:index + 1]) if index else 0.0, linear_trend(hr_filtered[:index + 1]),
            t, fmean(temp_filtered[:index + 1]), min(temp_filtered[:index + 1]), max(temp_filtered[:index + 1]),
            pstdev(temp_filtered[:index + 1]) if index else 0.0, linear_trend(temp_filtered[:index + 1]),
        )
        for index, (h, t) in enumerate(zip(hr_filtered, temp_filtered))
    ]

