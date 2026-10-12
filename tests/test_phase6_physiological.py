import unittest

from physiological.baseline import build_physiological_baseline
from physiological.dehydration_engine import (
    calculate_dehydration_score,
    classify_dehydration_risk,
    evaluate_dehydration_risk,
)
from physiological.feature_processing import calculate_current_physiology
from physiological.persistence import PhysiologicalPersistenceTracker


def rows(activity="REST", count=3):
    return [
        {
            "timestamp": f"2026-01-01T00:0{i}:00.000Z",
            "activity_state": activity,
            "hr_filtered": str(70 + i),
            "temperature_filtered": str(32.5 + i * 0.1),
        }
        for i in range(count)
    ]


class Phase6Tests(unittest.TestCase):
    def test_baseline_uses_rest_and_calculates_statistics(self):
        baseline = build_physiological_baseline(rows(), "A1", baseline_duration_seconds=120, minimum_samples=3)
        self.assertEqual(baseline.athlete_id, "A1")
        self.assertAlmostEqual(baseline.hr_base, 71)
        self.assertAlmostEqual(baseline.temperature_base, 32.6)

    def test_baseline_rejects_invalid_duration_and_missing_data(self):
        with self.assertRaises(ValueError):
            build_physiological_baseline(rows(), "A1", baseline_duration_seconds=60, minimum_samples=3)
        bad = rows()
        bad[0]["hr_filtered"] = ""
        with self.assertRaises(ValueError):
            build_physiological_baseline(bad, "A1", baseline_duration_seconds=120, minimum_samples=3)

    def test_current_features_and_trends(self):
        current = calculate_current_physiology(rows("EXERCISE", 4), window_size=2)
        self.assertEqual(current["hr_trend"], "INCREASING")
        self.assertEqual(current["temperature_trend"], "INCREASING")

    def test_formula_and_clamping(self):
        raw, score = calculate_dehydration_score(80, 32.8, 70, 32.5)
        self.assertEqual(raw, 26)
        self.assertEqual(score, 26)
        self.assertEqual(calculate_dehydration_score(200, 40, 50, 20)[1], 100)
        self.assertEqual(calculate_dehydration_score(40, 20, 80, 40)[1], 0)

    def test_levels_and_persistence(self):
        self.assertEqual(classify_dehydration_risk(20).value, "LOW")
        self.assertEqual(classify_dehydration_risk(50).value, "MODERATE")
        self.assertEqual(classify_dehydration_risk(80).value, "HIGH")
        tracker = PhysiologicalPersistenceTracker(2)
        self.assertFalse(tracker.update(True))
        self.assertTrue(tracker.update(True))
        self.assertFalse(tracker.update(False))

    def test_result_activity_and_alert_logic(self):
        baseline = build_physiological_baseline(rows(), "A1", baseline_duration_seconds=120, minimum_samples=3)
        current = {
            "hr_current": 130.0, "temperature_current": 35.0,
            "hr_trend": "INCREASING", "temperature_trend": "INCREASING",
            "activity_state": "EXERCISE",
        }
        tracker = PhysiologicalPersistenceTracker(2)
        first = evaluate_dehydration_risk(current, baseline, tracker)
        second = evaluate_dehydration_risk(current, baseline, tracker)
        self.assertEqual(second.activity_state, "EXERCISE")
        self.assertTrue(second.persistent)
        self.assertTrue(second.alert_requested)
        self.assertGreater(len(first.signals), 0)


if __name__ == "__main__":
    unittest.main()

