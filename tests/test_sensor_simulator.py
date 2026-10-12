import csv
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from sensor_simulator.config import SimulationConfig
from sensor_simulator.models import ActivityState, SensorReading
from sensor_simulator.simulator import CSV_COLUMNS, SimulatedSensorSource, write_csv
from sensor_simulator.validation import validate_csv, validate_readings


class SensorSimulatorTests(unittest.TestCase):
    def make_readings(self, scenario="normal_walking", seed=42):
        config = SimulationConfig(duration_seconds=3, sample_interval_seconds=0.1, seed=seed)
        readings = list(SimulatedSensorSource(config, scenario).readings())
        validate_readings(readings, config)
        return readings

    def test_sensor_reading_contains_phase_one_fields_and_activity(self):
        reading = self.make_readings()[0]
        self.assertIsInstance(reading, SensorReading)
        self.assertEqual(reading.activity_state, ActivityState.WALKING)
        self.assertIsInstance(reading.heel_fsr_raw, int)

    def test_seed_reproduces_data(self):
        first = [reading.as_dict() for reading in self.make_readings(seed=10)]
        second = [reading.as_dict() for reading in self.make_readings(seed=10)]
        self.assertEqual(first, second)

    def test_scenarios_change_activity_and_sensor_pattern(self):
        rest = self.make_readings("normal_rest")
        exercise = self.make_readings("exercise")
        self.assertEqual(rest[0].activity_state, ActivityState.REST)
        self.assertEqual(exercise[0].activity_state, ActivityState.EXERCISE)
        self.assertGreater(exercise[-1].heart_rate_bpm, rest[-1].heart_rate_bpm)

    def test_csv_has_expected_columns(self):
        readings = self.make_readings()
        with TemporaryDirectory() as directory:
            path = write_csv(readings, Path(directory) / "data.csv")
            validate_csv(path)
            with path.open(encoding="utf-8", newline="") as handle:
                rows = list(csv.DictReader(handle))
            self.assertEqual(len(rows), len(readings))
            self.assertEqual(list(rows[0]), CSV_COLUMNS)

    def test_invalid_scenario_is_rejected(self):
        with self.assertRaises(ValueError):
            SimulatedSensorSource(SimulationConfig(), "unknown")


if __name__ == "__main__":
    unittest.main()

