"""Small, dependency-free filters used by the processing pipeline."""

from math import isfinite
from typing import Iterable, List, Sequence


def moving_average(values: Sequence[float], window_size: int) -> List[float]:
    if window_size < 1:
        raise ValueError("window_size must be at least 1")
    output: List[float] = []
    for index in range(len(values)):
        window = values[max(0, index - window_size + 1): index + 1]
        output.append(sum(window) / len(window))
    return output


def validate_numeric(values: Iterable[float], field_name: str) -> None:
    for value in values:
        if not isfinite(float(value)):
            raise ValueError(f"{field_name} contains a non-finite value")


def linear_trend(values: Sequence[float]) -> float:
    if len(values) < 2:
        return 0.0
    return (values[-1] - values[0]) / (len(values) - 1)

