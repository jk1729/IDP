"""Optional baseline distribution plots; matplotlib is required only when used."""

import csv
from pathlib import Path
from typing import Sequence

from .baseline_model import PersonalBaseline


def plot_baseline_distributions(
    processed_csv: str | Path,
    baseline: PersonalBaseline,
    output_path: str | Path = "data/baseline/baseline_distributions.png",
) -> Path:
    import matplotlib.pyplot as plt

    with Path(processed_csv).open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    columns = [("cadence", "cadence"), ("heel_percentage", "heel_percentage"),
               ("forefoot_percentage", "forefoot_percentage"), ("heart_rate", "hr_filtered")]
    figure, axes = plt.subplots(2, 2, figsize=(10, 7))
    for axis, (feature, column) in zip(axes.flat, columns):
        values = [float(row[column]) for row in rows if row.get(column, "") not in ("", None)]
        if values:
            axis.hist(values, bins=min(20, max(5, len(values) // 5)))
        statistics = baseline.features.get(feature)
        if statistics:
            axis.axvline(statistics.mean, color="red", label="Baseline mean")
            axis.legend()
        axis.set_title(feature)
    figure.suptitle("SIMULATED BASELINE - NOT CLINICAL DATA")
    figure.tight_layout()
    target = Path(output_path)
    target.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(target, dpi=120)
    plt.close(figure)
    return target

