"""Phase 4 demonstration: build, save, load, and compare a baseline."""

import argparse
import csv

from .baseline_builder import build_baseline
from .baseline_comparison import compare_to_baseline, session_feature_means
from .baseline_storage import load_baseline, save_baseline
from .report import generate_baseline_report


def main() -> None:
    parser = argparse.ArgumentParser(description="Build and compare a personal baseline.")
    parser.add_argument("--input", default="data/processed/processed_data.csv")
    parser.add_argument("--current-input", default=None,
                        help="Optional separate processed session to compare")
    parser.add_argument("--athlete-id", default="ATHLETE_001")
    parser.add_argument("--output", default="data/baseline/personal_baseline.json")
    parser.add_argument("--minimum-samples", type=int, default=30)
    args = parser.parse_args()
    baseline = build_baseline(args.input, args.athlete_id, minimum_samples=args.minimum_samples)
    save_baseline(baseline, args.output)
    loaded = load_baseline(args.output)
    current_path = args.current_input or args.input
    with open(current_path, encoding="utf-8", newline="") as handle:
        current = session_feature_means(list(csv.DictReader(handle)))
    comparisons = compare_to_baseline(current, loaded)
    print("BASELINE CREATED")
    print(generate_baseline_report(loaded))
    print("\nCURRENT SESSION COMPARISON")
    for item in comparisons:
        print(f"{item.feature}: current={item.current_value:.2f}, "
              f"deviation={item.absolute_deviation:.2f}, z-score={item.z_score:.2f}")
    print(f"\nSaved: {args.output}")


if __name__ == "__main__":
    main()
