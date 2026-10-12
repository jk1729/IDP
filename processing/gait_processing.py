"""Transparent prototype step, cadence, and contact calculations."""

from dataclasses import dataclass
from typing import Sequence


@dataclass
class GaitFeatures:
    step_detected: bool
    step_interval: float
    cadence: float
    gait_cycle_duration: float
    contact_state: str
    contact_duration: float
    no_contact_duration: float
    contact_ratio: float


def detect_steps(
    signal: Sequence[float], sample_interval: float, threshold: float, min_step_interval: float,
) -> list[int]:
    if sample_interval <= 0 or min_step_interval <= 0:
        raise ValueError("Sampling and minimum step intervals must be positive")
    minimum_samples = max(1, int(min_step_interval / sample_interval))
    peaks: list[int] = []
    for index in range(1, len(signal) - 1):
        if signal[index] >= threshold and signal[index] >= signal[index - 1] and signal[index] > signal[index + 1]:
            if not peaks or index - peaks[-1] >= minimum_samples:
                peaks.append(index)
            elif signal[index] > signal[peaks[-1]]:
                peaks[-1] = index
    return peaks


def cadence_from_steps(step_indexes: Sequence[int], sample_interval: float, sample_count: int) -> float:
    if len(step_indexes) < 2 or sample_count <= 0:
        return 0.0
    elapsed_minutes = sample_count * sample_interval / 60.0
    return len(step_indexes) / elapsed_minutes if elapsed_minutes else 0.0


def contact_features(total_loading: Sequence[float], threshold: float, sample_interval: float) -> list[GaitFeatures]:
    if sample_interval <= 0:
        raise ValueError("sample_interval must be positive")
    output: list[GaitFeatures] = []
    contact_samples = 0
    previous_step = None
    for index, loading in enumerate(total_loading):
        contact = loading > threshold
        contact_samples = contact_samples + 1 if contact else 0
        no_contact_samples = 0 if contact else 1
        output.append(GaitFeatures(
            step_detected=False, step_interval=0.0, cadence=0.0, gait_cycle_duration=0.0,
            contact_state="CONTACT" if contact else "NO_CONTACT",
            contact_duration=contact_samples * sample_interval,
            no_contact_duration=no_contact_samples * sample_interval,
            contact_ratio=1.0 if contact else 0.0,
        ))
    return output


def apply_step_features(
    features: list[GaitFeatures], step_indexes: Sequence[int], sample_interval: float,
) -> list[GaitFeatures]:
    intervals = [index - previous for previous, index in zip(step_indexes, step_indexes[1:])]
    interval_by_index = {index: interval * sample_interval for index, interval in zip(step_indexes[1:], intervals)}
    cadence = 60.0 / (sum(intervals) * sample_interval / len(intervals)) if intervals else 0.0
    for index in step_indexes:
        old = features[index]
        step_interval = interval_by_index.get(index, 0.0)
        features[index] = GaitFeatures(
            True, step_interval, cadence, step_interval, old.contact_state,
            old.contact_duration, old.no_contact_duration, old.contact_ratio,
        )
    return features

