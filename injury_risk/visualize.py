"""Optional component visualization for simulated prototype risk estimation."""

from pathlib import Path

from .schemas import RiskResult


def plot_risk_result(
    result: RiskResult,
    output_path: str | Path = "data/risk/injury_risk_components.png",
) -> Path:
    import matplotlib.pyplot as plt

    labels = list(result.components)
    values = list(result.components.values())
    figure, axis = plt.subplots(figsize=(8, 5))
    axis.bar(labels, values)
    axis.set_ylim(0, 100)
    axis.set_ylabel("Score (0-100)")
    axis.set_title("SIMULATED DATA - PROTOTYPE RISK ESTIMATION")
    axis.axhline(result.risk_score, color="red", linestyle="--", label=f"Final: {result.risk_score:.2f}")
    axis.legend()
    target = Path(output_path)
    target.parent.mkdir(parents=True, exist_ok=True)
    figure.tight_layout()
    figure.savefig(target, dpi=120)
    plt.close(figure)
    return target

