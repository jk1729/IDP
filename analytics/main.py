"""Run the complete Phase 1 simulated data flow."""

from pathlib import Path

from .pipeline import calibrate_baseline, export_session, process_readings
from .simulator import simulate_readings


def main() -> None:
    raw = list(simulate_readings())
    processed = process_readings(raw)
    baseline = calibrate_baseline(processed)
    export_session(Path("data/simulated"), raw, processed, baseline)
    print("Phase 1 simulation complete.")
    print(f"Generated {len(raw)} simulated readings.")
    print(f"Baseline valid: {baseline.valid}; quality: {baseline.quality_score:.2f}")
    print("Outputs: data/simulated/session.jsonl, session.csv, example_reading.json, baseline.json")


if __name__ == "__main__":
    main()

