"""Human-readable baseline report."""

from .baseline_model import PersonalBaseline


def generate_baseline_report(baseline: PersonalBaseline) -> str:
    def line(name: str, label: str | None = None) -> str:
        statistics = baseline.features.get(name)
        if statistics is None:
            return f"{label or name}: unavailable"
        return f"{label or name}: Mean {statistics.mean:.2f}, Std {statistics.std:.2f}"

    return "\n".join([
        "PERSONAL BASELINE",
        "",
        f"Athlete: {baseline.athlete_id}",
        f"Baseline duration: {baseline.duration_seconds:.1f} seconds",
        f"Samples: {baseline.sample_count}",
        f"Version: {baseline.baseline_version}",
        "",
        "Normal gait:",
        line("cadence", "Cadence"),
        line("heel_percentage", "Heel loading"),
        line("forefoot_percentage", "Forefoot loading"),
        line("impact_mean", "Impact"),
        "",
        "Resting physiology:",
        line("heart_rate", "Resting HR"),
        line("temperature", "Temperature"),
    ])

