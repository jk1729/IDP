"""Explanatory physiological deviation signals."""

from .config import HR_TREND_THRESHOLD, TEMPERATURE_TREND_THRESHOLD
from .schemas import PhysiologicalSignal


def _severity(delta: float, reference: float) -> str:
    magnitude = abs(delta)
    return "high" if magnitude >= reference * 2 else "moderate" if magnitude >= reference else "low"


def generate_physiological_signals(
    delta_hr: float,
    delta_temperature: float,
    hr_trend: str,
    temperature_trend: str,
) -> list[PhysiologicalSignal]:
    signals = []
    if delta_hr > 0:
        signals.append(PhysiologicalSignal(
            "heart rate elevated above personal baseline", "heart_rate",
            delta_hr, _severity(delta_hr, 10.0),
        ))
    if delta_temperature > 0:
        signals.append(PhysiologicalSignal(
            "temperature elevated above personal baseline", "temperature",
            delta_temperature, _severity(delta_temperature, 0.5),
        ))
    if hr_trend == "INCREASING":
        signals.append(PhysiologicalSignal("heart rate increasing", "heart_rate", delta_hr, "moderate"))
    if temperature_trend == "INCREASING":
        signals.append(PhysiologicalSignal("temperature increasing", "temperature", delta_temperature, "moderate"))
    return signals

