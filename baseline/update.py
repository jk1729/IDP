"""Explicit baseline update helper; callers must opt in deliberately."""

from __future__ import annotations

from typing import Any, Mapping, Sequence

from .baseline_builder import build_baseline
from .baseline_model import PersonalBaseline


def update_baseline(
    old_baseline: PersonalBaseline,
    new_data: str | Sequence[Mapping[str, Any]],
    *,
    minimum_samples: int,
    outlier_rule: float | None,
) -> PersonalBaseline:
    """Build a replacement baseline explicitly; no automatic event updates occur."""
    return build_baseline(
        new_data,
        old_baseline.athlete_id,
        minimum_samples=minimum_samples,
        baseline_version=old_baseline.baseline_version,
        outlier_rule=outlier_rule,
    )

