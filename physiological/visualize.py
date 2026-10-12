"""Optional physiological prototype-estimation plots."""

from pathlib import Path
from typing import Sequence

from .schemas import PhysiologicalBaseline, DehydrationRiskResult


def plot_physiology(
    timestamps: Sequence[str],
    heart_rates: Sequence[float],
    temperatures: Sequence[float],
    scores: Sequence[float],
    baseline: PhysiologicalBaseline,
    output_path: str | Path = "data/physiological/physiological_plot.png",
) -> Path:
    import matplotlib.pyplot as plt

    x = list(range(len(timestamps)))
    figure, axes = plt.subplots(3, 1, sharex=True, figsize=(11, 8))
    axes[0].plot(x, heart_rates, label="HR")
    axes[0].axhline(baseline.hr_base, color="red", label="HR baseline")
    axes[0].set_ylabel("bpm")
    axes[1].plot(x, temperatures, label="Temperature")
    axes[1].axhline(baseline.temperature_base, color="red", label="Temperature baseline")
    axes[1].set_ylabel("Temperature")
    axes[2].plot(x, scores, label="Risk score")
    axes[2].set_ylabel("Score")
    axes[2].set_ylim(0, 100)
    for axis in axes:
        axis.legend()
    figure.suptitle("SIMULATED DATA - PROTOTYPE ESTIMATION")
    target = Path(output_path)
    target.parent.mkdir(parents=True, exist_ok=True)
    figure.tight_layout()
    figure.savefig(target, dpi=120)
    plt.close(figure)
    return target

