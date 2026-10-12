import math
import unittest

from baseline.baseline_model import FeatureStatistics, PersonalBaseline
from injury_risk.feature_scoring import calculate_risk_score, classify_risk, normalize_deviation
from injury_risk.persistence import PersistenceTracker
from injury_risk.risk_engine import evaluate_injury_risk
from injury_risk.signal_generator import generate_risk_signals


def baseline():
    features = {}
    for name in (
        "heel_percentage", "forefoot_percentage", "loading_imbalance", "total_loading",
        "angular_motion_mean", "angular_motion_peak", "impact_peak", "impact_mean",
        "cadence", "step_interval", "gait_cycle_duration", "contact_duration",
    ):
        features[name] = FeatureStatistics(10.0, 1.0, 0.0, 20.0, 10)
    return PersonalBaseline("A1", "2026-01-01T00:00:00Z", 300, 100, "baseline-v1", features)


class Phase5Tests(unittest.TestCase):
    def test_normalization_and_invalid_values(self):
        self.assertEqual(normalize_deviation(0), 0)
        self.assertEqual(normalize_deviation(3), 100)
        self.assertEqual(normalize_deviation(30), 100)
        self.assertEqual(normalize_deviation(float("nan")), 0)
        self.assertEqual(normalize_deviation(None), 0)

    def test_weighted_formula(self):
        self.assertEqual(calculate_risk_score(100, 0, 0, 0), 35)
        self.assertEqual(calculate_risk_score(100, 100, 100, 100), 100)

    def test_classification(self):
        self.assertEqual(classify_risk(0), "LOW")
        self.assertEqual(classify_risk(40), "MODERATE")
        self.assertEqual(classify_risk(70), "HIGH")

    def test_signals_only_for_abnormal_features(self):
        signals = generate_risk_signals({"loading_imbalance": 2.1, "cadence": 0.2})
        self.assertEqual(len(signals), 1)
        self.assertEqual(signals[0].signal, "loading imbalance")

    def test_persistence_and_reset(self):
        tracker = PersistenceTracker(required_consecutive_cycles=3)
        self.assertFalse(tracker.update(True))
        self.assertFalse(tracker.update(True))
        self.assertTrue(tracker.update(True))
        self.assertFalse(tracker.update(False))
        self.assertEqual(tracker.consecutive_abnormal_cycles, 0)

    def test_risk_engine_and_vibration_requires_persistence(self):
        current = {name: 14.0 for name in baseline().features}
        tracker = PersistenceTracker(2)
        first = evaluate_injury_risk(current, baseline(), tracker)
        self.assertFalse(first.vibration_alert)
        second = evaluate_injury_risk(current, baseline(), tracker)
        self.assertTrue(second.persistent_abnormality)
        self.assertTrue(second.vibration_alert)
        self.assertTrue(0 <= second.risk_score <= 100)

    def test_missing_features_are_safe(self):
        result = evaluate_injury_risk({}, baseline())
        self.assertEqual(result.risk_score, 0)
        self.assertFalse(result.abnormal)


if __name__ == "__main__":
    unittest.main()

