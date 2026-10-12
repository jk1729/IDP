"""Serializable models for athlete-specific baseline statistics."""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List


@dataclass
class FeatureStatistics:
    mean: float
    std: float
    minimum: float
    maximum: float
    count: int


@dataclass
class PersonalBaseline:
    athlete_id: str
    created_at: str
    duration_seconds: float
    sample_count: int
    baseline_version: str
    features: Dict[str, FeatureStatistics] = field(default_factory=dict)
    calibration_activity: Dict[str, str] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        result = asdict(self)
        result["features"] = {
            name: asdict(statistics) for name, statistics in self.features.items()
        }
        return result

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> "PersonalBaseline":
        return cls(
            athlete_id=value["athlete_id"],
            created_at=value["created_at"],
            duration_seconds=float(value["duration_seconds"]),
            sample_count=int(value["sample_count"]),
            baseline_version=value["baseline_version"],
            features={
                name: FeatureStatistics(
                    mean=float(item["mean"]),
                    std=float(item["std"]),
                    minimum=float(item["minimum"]),
                    maximum=float(item["maximum"]),
                    count=int(item["count"]),
                )
                for name, item in value.get("features", {}).items()
            },
            calibration_activity=dict(value.get("calibration_activity", {})),
        )


@dataclass
class BaselineComparison:
    feature: str
    current_value: float
    baseline_mean: float
    baseline_std: float
    absolute_deviation: float
    percentage_deviation: float | None
    z_score: float

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

