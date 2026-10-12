"""Build and persist a personalized resting physiological baseline."""

from __future__ import annotations

import csv
import math
from datetime import datetime, timezone
from pathlib import Path
from statistics import fmean, pstdev
from typing import Any, Mapping, Sequence

from .config import BASELINE_MAX_SECONDS, BASELINE_MIN_SECONDS, BASELINE_VERSION
from .schemas import PhysiologicalBaseline


def _rows(data: str | Path | Sequence[Mapping[str, Any]]) -> list[dict[str, Any]]:
    if isinstance(data, (str, Path)):
        with Path(data).open(encoding="utf-8", newline="") as handle:
            result = list(csv.DictReader(handle))
    else:
        result = [dict(row) for row in data]
    if not result:
        raise ValueError("Physiological baseline dataset is empty")
    return result


def _number(row: Mapping[str, Any], *names: str) -> float | None:
    for name in names:
        value = row.get(name)
        if value not in (None, ""):
            try:
                numeric = float(value)
                if math.isfinite(numeric):
                    return numeric
            except (TypeError, ValueError):
                pass
    return None


def _duration(rows: Sequence[Mapping[str, Any]]) -> float:
    try:
        start = datetime.fromisoformat(str(rows[0]["timestamp"]).replace("Z", "+00:00"))
        end = datetime.fromisoformat(str(rows[-1]["timestamp"]).replace("Z", "+00:00"))
        return round(max(0.0, (end - start).total_seconds()))
    except (KeyError, TypeError, ValueError):
        return 0.0


def build_physiological_baseline(
    data: str | Path | Sequence[Mapping[str, Any]],
    athlete_id: str,
    *,
    baseline_duration_seconds: float | None = None,
    minimum_samples: int = 1,
    baseline_version: str = BASELINE_VERSION,
) -> PhysiologicalBaseline:
    """Prefer REST rows and calculate resting HR and temperature statistics."""
    if not athlete_id.strip():
        raise ValueError("athlete_id must not be empty")
    rows = _rows(data)
    duration = _duration(rows) if baseline_duration_seconds is None else baseline_duration_seconds
    if not BASELINE_MIN_SECONDS <= duration <= BASELINE_MAX_SECONDS:
        raise ValueError(
            f"Baseline duration must be between {BASELINE_MIN_SECONDS:g} and "
            f"{BASELINE_MAX_SECONDS:g} seconds"
        )
    if len(rows) < minimum_samples:
        raise ValueError(f"Insufficient baseline samples: {len(rows)} < {minimum_samples}")
    rest_rows = [row for row in rows if row.get("activity_state") == "REST"]
    selected = rest_rows or rows
    hr = [_number(row, "hr_filtered", "heart_rate_bpm", "heart_rate") for row in selected]
    temperature = [_number(row, "temperature_filtered", "temperature_c", "temperature") for row in selected]
    hr_values = [value for value in hr if value is not None]
    temperature_values = [value for value in temperature if value is not None]
    if len(hr_values) < minimum_samples or len(temperature_values) < minimum_samples:
        raise ValueError("Insufficient valid heart-rate or temperature samples")
    return PhysiologicalBaseline(
        athlete_id=athlete_id,
        hr_base=fmean(hr_values),
        hr_std=pstdev(hr_values) if len(hr_values) > 1 else 0.0,
        temperature_base=fmean(temperature_values),
        temperature_std=pstdev(temperature_values) if len(temperature_values) > 1 else 0.0,
        baseline_duration_seconds=duration,
        sample_count=len(selected),
        created_at=datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z"),
        baseline_version=baseline_version,
    )
