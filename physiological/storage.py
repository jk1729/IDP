"""JSON storage for physiological baselines."""

import json
from pathlib import Path

from .schemas import PhysiologicalBaseline


def save_physiological_baseline(
    baseline: PhysiologicalBaseline,
    path: str | Path = "data/baseline/physiological_baseline.json",
) -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(baseline.to_dict(), indent=2) + "\n", encoding="utf-8")
    return target


def load_physiological_baseline(
    path: str | Path = "data/baseline/physiological_baseline.json",
) -> PhysiologicalBaseline:
    source = Path(path)
    if not source.exists():
        raise FileNotFoundError(f"Physiological baseline file does not exist: {source}")
    return PhysiologicalBaseline.from_dict(json.loads(source.read_text(encoding="utf-8")))

