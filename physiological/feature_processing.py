"""Current-window heart-rate and temperature processing."""

from __future__ import annotations

import math
from statistics import fmean
from typing import Any, Mapping, Sequence

from .config import HR_TREND_THRESHOLD, TEMPERATURE_TREND_THRESHOLD


def _trend(values: Sequence[float], threshold: float) -> str:
    if len(values) < 2:
        return "STABLE"
    slope = (values[-1] - values[0]) / (len(values) - 1)
    if slope > threshold:
        return "INCREASING"
    if slope < -threshold:
        return "DECREASING"
    return "STABLE"


def _values(rows: Sequence[Mapping[str, Any]], *names: str) -> list[float]:
    values = []
    for row in rows:
        for name in names:
            try:
                value = float(row[name])
                if math.isfinite(value):
                    values.append(value)
                    break
            except (KeyError, TypeError, ValueError):
                continue
    return values


def calculate_current_physiology(
    rows: Sequence[Mapping[str, Any]],
    *,
    window_size: int = 5,
) -> dict[str, float | str]:
    if not rows:
        raise ValueError("Current physiological window is empty")
    hr = _values(rows, "hr_filtered", "heart_rate_bpm", "heart_rate")
    temperature = _values(rows, "temperature_filtered", "temperature_c", "temperature")
    if not hr or not temperature:
        raise ValueError("Current window lacks valid heart-rate or temperature values")
    hr_window = hr[-window_size:] if window_size > 0 else hr
    temperature_window = temperature[-window_size:] if window_size > 0 else temperature
    return {
        "hr_current": fmean(hr_window),
        "temperature_current": fmean(temperature_window),
        "hr_filtered": fmean(hr_window),
        "temperature_filtered": fmean(temperature_window),
        "hr_trend": _trend(hr, HR_TREND_THRESHOLD),
        "temperature_trend": _trend(temperature, TEMPERATURE_TREND_THRESHOLD),
        "activity_state": str(rows[-1].get("activity_state", "UNKNOWN")),
    }

