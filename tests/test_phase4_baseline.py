import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from baseline.baseline_builder import build_baseline
from baseline.baseline_comparison import compare_to_baseline
from baseline.baseline_model import FeatureStatistics
from baseline.baseline_storage import load_baseline, save_baseline


def rows(values, activity="WALKING"):
    return [
        {
            "timestamp": f"2026-01-01T00:00:0{i}.000Z",
            "activity_state": activity,
            "heel_filtered": str(value),
            "forefoot_filtered": str(value * 2),
            "total_loading": str(value * 3),
            "heel_percentage": "33.3",
            "forefoot_percentage": "66.7",
            "loading_imbalance": "16.7",
            "cadence": str(value),
            "step_interval": "0.4",
            "gait_cycle_duration": "0.4",
            "contact_duration": "0.3",
            "contact_ratio": "0.8",
            "impact_peak": "1.2",
            "impact_mean": "0.5",
            "angular_motion_mean": "10",
            "angular_motion_peak": "20",
            "hr_filtered": str(value + 60),
            "temperature_filtered": "30",
        }
        for i, value in enumerate(values)
    ]


class Phase4BaselineTests(unittest.TestCase):
    def test_statistics_and_athlete_id(self):
        baseline = build_baseline(rows([10, 20, 30]), "ATHLETE_X", minimum_samples=3, outlier_rule=None)
        self.assertEqual(baseline.athlete_id, "ATHLETE_X")
        self.assertEqual(baseline.features["cadence"].mean, 20)
        self.assertAlmostEqual(baseline.features["cadence"].minimum, 10)
        self.assertEqual(baseline.features["cadence"].count, 3)

    def test_empty_and_insufficient_data(self):
        with self.assertRaises(ValueError):
            build_baseline([], "ATHLETE_X")
        with self.assertRaises(ValueError):
            build_baseline(rows([10]), "ATHLETE_X", minimum_samples=2)

    def test_missing_values_are_skipped(self):
        data = rows([10, 20, 30])
        data[1]["cadence"] = ""
        baseline = build_baseline(data, "ATHLETE_X", minimum_samples=3, outlier_rule=None)
        self.assertEqual(baseline.features["cadence"].count, 2)

    def test_json_save_load(self):
        baseline = build_baseline(rows([10, 20, 30]), "ATHLETE_X", minimum_samples=3, outlier_rule=None)
        with TemporaryDirectory() as directory:
            path = save_baseline(baseline, Path(directory) / "baseline.json")
            loaded = load_baseline(path)
            self.assertEqual(loaded.athlete_id, "ATHLETE_X")
            self.assertEqual(loaded.features["cadence"].mean, 20)

    def test_comparison_and_zero_standard_deviation(self):
        baseline = build_baseline(rows([10, 20, 30]), "ATHLETE_X", minimum_samples=3, outlier_rule=None)
        comparison = compare_to_baseline({"cadence": 30, "temperature": 31}, baseline)
        cadence = next(item for item in comparison if item.feature == "cadence")
        self.assertAlmostEqual(cadence.z_score, 1.224744871, places=5)
        temperature = next(item for item in comparison if item.feature == "temperature")
        self.assertEqual(temperature.z_score, 0.0)
        self.assertIsNotNone(temperature.percentage_deviation)

    def test_zero_baseline_mean_is_safe(self):
        baseline = type("Baseline", (), {
            "features": {"zero": FeatureStatistics(0, 0, 0, 0, 1)}
        })()
        result = compare_to_baseline({"zero": 1}, baseline)
        self.assertIsNone(result[0].percentage_deviation)
        self.assertEqual(result[0].z_score, 0.0)


if __name__ == "__main__":
    unittest.main()

