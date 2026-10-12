"""Command-line entry point for Phase 2."""

import argparse

from sensor_simulator.config import SimulationConfig
from sensor_simulator.simulator import SCENARIOS, SimulatedSensorSource, write_csv, write_json
from sensor_simulator.validation import validate_csv, validate_readings


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate SIMULATED DATA for the smart insole.")
    parser.add_argument("--scenario", choices=sorted(SCENARIOS), default="normal_walking")
    parser.add_argument("--duration", type=float, default=60.0, help="Duration in seconds")
    parser.add_argument("--interval", type=float, default=0.1, help="Sampling interval in seconds")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--output", default="sensor_simulator/output/simulated_data.csv")
    parser.add_argument("--json-output", default="sensor_simulator/output/example_reading.json")
    args = parser.parse_args()
    config = SimulationConfig(duration_seconds=args.duration, sample_interval_seconds=args.interval, seed=args.seed, output_path=args.output)
    readings = list(SimulatedSensorSource(config, args.scenario).readings())
    validate_readings(readings, config)
    csv_path = write_csv(readings, args.output)
    write_json(readings[0], args.json_output)
    validate_csv(csv_path)
    print("Phase 2 simulation complete: SIMULATED DATA - NOT EXPERIMENTAL RESULTS")
    print(f"Scenario: {args.scenario}; readings: {len(readings)}; seed: {args.seed}")
    print(f"CSV: {csv_path}")
    print(f"JSON example: {args.json_output}")


if __name__ == "__main__":
    main()

