"""Two-channel heel/forefoot FSR processing."""

from dataclasses import dataclass
from math import fsum
from statistics import fmean, pstdev
from typing import Sequence

from .filters import moving_average, validate_numeric


@dataclass
class FSRFeatures:
    heel_filtered: float
    forefoot_filtered: float
    total_loading: float
    heel_percentage: float
    forefoot_percentage: float
    loading_difference: float
    loading_ratio: float
    loading_imbalance: float


def process_fsr(heel: Sequence[float], forefoot: Sequence[float], window_size: int) -> list[FSRFeatures]:
    if len(heel) != len(forefoot):
        raise ValueError("Heel and forefoot sequences must have equal length")
    validate_numeric(heel, "heel_fsr")
    validate_numeric(forefoot, "forefoot_fsr")
    heel_filtered = moving_average(heel, window_size)
    forefoot_filtered = moving_average(forefoot, window_size)
    output = []
    for heel_value, forefoot_value in zip(heel_filtered, forefoot_filtered):
        total = heel_value + forefoot_value
        if total <= 0:
            heel_percentage = forefoot_percentage = loading_ratio = loading_imbalance = 0.0
        else:
            heel_percentage = heel_value / total * 100.0
            forefoot_percentage = forefoot_value / total * 100.0
            loading_ratio = heel_value / forefoot_value if forefoot_value > 0 else 0.0
            loading_imbalance = abs(heel_percentage - 50.0)
        output.append(FSRFeatures(
            heel_value, forefoot_value, total, heel_percentage, forefoot_percentage,
            heel_value - forefoot_value, loading_ratio, loading_imbalance,
        ))
    return output


def loading_summary(features: Sequence[FSRFeatures]) -> dict[str, float]:
    totals = [item.total_loading for item in features]
    return {
        "loading_peak": max(totals, default=0.0),
        "loading_mean": fmean(totals) if totals else 0.0,
        "loading_variability": pstdev(totals) if len(totals) > 1 else 0.0,
    }


def classify_loading_distribution(heel_percentage: float, tolerance: float = 10.0) -> str:
    if heel_percentage > 50.0 + tolerance:
        return "HEEL_DOMINANT"
    if heel_percentage < 50.0 - tolerance:
        return "FOREFOOT_DOMINANT"
    return "BALANCED"

