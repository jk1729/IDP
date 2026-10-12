"""JSON persistence for personal baselines."""

import json
from pathlib import Path

from .baseline_model import PersonalBaseline


def save_baseline(baseline: PersonalBaseline, path: str | Path = "data/baseline/personal_baseline.json") -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(baseline.to_dict(), indent=2) + "\n", encoding="utf-8")
    return target


def load_baseline(path: str | Path = "data/baseline/personal_baseline.json") -> PersonalBaseline:
    source = Path(path)
    if not source.exists():
        raise FileNotFoundError(f"Baseline file does not exist: {source}")
    return PersonalBaseline.from_dict(json.loads(source.read_text(encoding="utf-8")))

