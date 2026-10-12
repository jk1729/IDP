"""Phase 5 demonstration using an actual processed session and saved baseline."""

import argparse
import csv
import json

from baseline.baseline_comparison import session_feature_means
from baseline.baseline_storage import load_baseline

from .persistence import PersistenceTracker
from .risk_engine import evaluate_injury_risk


def main() -> None:
    parser = argparse.ArgumentParser(description="Estimate abnormal gait/loading patterns.")
    parser.add_argument("--baseline", default="data/baseline/personal_baseline.json")
    parser.add_argument("--current-input", default="data/processed/processed_data.csv")
    args = parser.parse_args()
    baseline = load_baseline(args.baseline)
    with open(args.current_input, encoding="utf-8", newline="") as handle:
        current = session_feature_means(list(csv.DictReader(handle)))
    result = evaluate_injury_risk(current, baseline, PersistenceTracker())
    print("PHASE 5 PROTOTYPE RISK ESTIMATION")
    print("This system estimates abnormal gait/loading patterns and does not diagnose injury.")
    print(json.dumps(result.to_dict(), indent=2))


if __name__ == "__main__":
    main()

