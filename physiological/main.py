"""Phase 6 demonstration."""

import argparse
import csv
import json

from baseline.baseline_comparison import session_feature_means
from .baseline import build_physiological_baseline
from .dehydration_engine import evaluate_dehydration_risk
from .feature_processing import calculate_current_physiology
from .persistence import PhysiologicalPersistenceTracker
from .storage import load_physiological_baseline, save_physiological_baseline


def main() -> None:
    parser = argparse.ArgumentParser(description="Estimate personalized physiological deviation.")
    parser.add_argument("--baseline-input", default="data/processed/physiological_baseline.csv")
    parser.add_argument("--current-input", default="data/processed/processed_data.csv")
    parser.add_argument("--output", default="data/baseline/physiological_baseline.json")
    parser.add_argument("--athlete-id", default="ATHLETE_001")
    parser.add_argument("--minimum-samples", type=int, default=1)
    args = parser.parse_args()
    with open(args.baseline_input, encoding="utf-8", newline="") as handle:
        baseline_rows = list(csv.DictReader(handle))
    baseline = build_physiological_baseline(
        baseline_rows, args.athlete_id, minimum_samples=args.minimum_samples,
    )
    save_physiological_baseline(baseline, args.output)
    baseline = load_physiological_baseline(args.output)
    with open(args.current_input, encoding="utf-8", newline="") as handle:
        current_rows = list(csv.DictReader(handle))
    current = calculate_current_physiology(current_rows)
    result = evaluate_dehydration_risk(current, baseline, PhysiologicalPersistenceTracker())
    print("PHYSIOLOGICAL MONITORING")
    print(f"HR baseline: {baseline.hr_base:.2f} bpm")
    print(f"Current HR: {current['hr_current']:.2f} bpm")
    print(f"HR deviation: {result.heart_rate['delta']:+.2f} bpm")
    print(f"Temperature baseline: {baseline.temperature_base:.2f}")
    print(f"Current temperature: {current['temperature_current']:.2f}")
    print(f"Temperature deviation: {result.temperature['delta']:+.2f}")
    print(f"Activity: {result.activity_state}")
    print(f"Dehydration-risk score: {result.risk_score:.2f}/100")
    print(f"Risk level: {result.risk_level.value}")
    print(f"Persistent: {'YES' if result.persistent else 'NO'}")
    print(f"Alert requested: {'YES' if result.alert_requested else 'NO'}")
    print("Signals:")
    for signal in result.signals:
        print(f"- {signal.signal} ({signal.severity})")
    print(json.dumps(result.to_dict(), indent=2))


if __name__ == "__main__":
    main()

