import csv
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from processing.filters import moving_average
from processing.fsr_processing import classify_loading_distribution, process_fsr
from processing.gait_processing import cadence_from_steps, detect_steps
from processing.imu_processing import process_imu
from processing.pipeline import process_sensor_data
from sensor_simulator.config import SimulationConfig
from sensor_simulator.simulator import SimulatedSensorSource, write_csv


class Phase3ProcessingTests(unittest.TestCase):
    def test_moving_average_and_zero_fsr(self):
        self.assertEqual(moving_average([1, 2, 3], 2), [1.0, 1.5, 2.5])
        self.assertEqual(process_fsr([0], [0], 2)[0].heel_percentage, 0.0)

    def test_fsr_features_and_distribution(self):
        item = process_fsr([100], [300], 1)[0]
        self.assertAlmostEqual(item.total_loading, 400)
        self.assertEqual(classify_loading_distribution(item.heel_percentage), "FOREFOOT_DOMINANT")

    def test_imu_magnitudes(self):
        item = process_imu([0], [0], [9.81], [3], [4], [0], 1)[0]
        self.assertAlmostEqual(item.accel_magnitude, 9.81, places=2)
        self.assertAlmostEqual(item.gyro_magnitude, 5.0, places=2)

    def test_steps_and_cadence(self):
        steps = detect_steps([0, 1.5, 0, 0, 1.5, 0], 0.1, 1.0, 0.3)
        self.assertEqual(steps, [1, 4])
        self.assertGreater(cadence_from_steps(steps, 0.1, 6), 0)

    def test_full_pipeline_writes_processed_csv(self):
        config = SimulationConfig(duration_seconds=4, sample_interval_seconds=0.1, seed=42)
        readings = list(SimulatedSensorSource(config, "normal_walking").readings())
        with TemporaryDirectory() as directory:
            raw = write_csv(readings, Path(directory) / "raw.csv")
            output = Path(directory) / "processed.csv"
            processed = process_sensor_data(raw, output)
            self.assertEqual(len(processed), len(readings))
            with output.open(encoding="utf-8", newline="") as handle:
                rows = list(csv.DictReader(handle))
            self.assertIn("total_loading", rows[0])
            self.assertIn("step_detected", rows[0])


if __name__ == "__main__":
    unittest.main()

