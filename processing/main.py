"""Command-line entry point for Phase 3."""

import argparse
from pathlib import Path

from sensor_simulator.config import SimulationConfig
from .pipeline import process_sensor_data


def main() -> None:
    parser = argparse.ArgumentParser(description="Process raw smart-insole sensor CSV data.")
    parser.add_argument("--input", default="sensor_simulator/output/simulated_data.csv")
    parser.add_argument("--output", default="data/processed/processed_data.csv")
    parser.add_argument("--interval", type=float, default=0.1)
    parser.add_argument("--contact-threshold", type=float, default=SimulationConfig.contact_threshold)
    parser.add_argument("--step-threshold", type=float, default=SimulationConfig.step_threshold)
    parser.add_argument("--min-step-interval", type=float, default=SimulationConfig.min_step_interval)
    args = parser.parse_args()
    features = process_sensor_data(args.input, args.output, sample_interval=args.interval,
                                   contact_threshold=args.contact_threshold,
                                   step_threshold=args.step_threshold,
                                   min_step_interval=args.min_step_interval)
    steps = sum(item.step_detected for item in features)
    print("Processing complete.")
    print(f"Input samples: {len(features)}")
    print(f"Detected steps: {steps}")
    print(f"Average cadence: {max((item.cadence for item in features), default=0.0):.2f} steps/min")
    print(f"Average heel loading: {sum(item.heel_percentage for item in features) / len(features):.2f}%")
    print(f"Average forefoot loading: {sum(item.forefoot_percentage for item in features) / len(features):.2f}%")
    print(f"Output: {Path(args.output)}")


if __name__ == "__main__":
    main()
