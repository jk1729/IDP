import unittest
from datetime import datetime, timezone

from injury_risk.schemas import RiskLevel, RiskResult
from physiological.schemas import DehydrationRiskResult, PhysiologicalRiskLevel, PhysiologicalSignal
from risk.alert_logic import AlertCooldown, AlertStateManager
from risk.persistence import RiskTransitionTracker
from risk.schemas import AlertType
from risk.unified_engine import evaluate_system_risk


def injury(score, persistent=False):
    return RiskResult(score, RiskLevel.HIGH if score >= 70 else RiskLevel.MODERATE if score >= 40 else RiskLevel.LOW,
                      {}, [], persistent, persistent, 3 if persistent else 0, False, "injury-risk-v1")


def physiology(score, persistent=False):
    return DehydrationRiskResult(score,
        PhysiologicalRiskLevel.HIGH if score >= 70 else PhysiologicalRiskLevel.MODERATE if score >= 40 else PhysiologicalRiskLevel.LOW,
        {}, {}, "EXERCISE", "STABLE", "STABLE", score, persistent, 3 if persistent else 0, [], False, False,
        "dehydration-risk-v1")


class Phase7Tests(unittest.TestCase):
    def test_maximum_and_statuses(self):
        result = evaluate_system_risk(injury(55), physiology(20))
        self.assertEqual(result.overall_risk_score, 55)
        self.assertEqual(result.system_status.value, "CAUTION")
        self.assertFalse(result.alert)

    def test_persistent_alert_types(self):
        both = evaluate_system_risk(injury(82, True), physiology(78, True))
        self.assertEqual(both.alert_type, AlertType.BOTH)
        self.assertTrue(both.alert)
        self.assertTrue(both.vibration_request)
        only_injury = evaluate_system_risk(injury(82, True), physiology(20))
        self.assertEqual(only_injury.alert_type, AlertType.INJURY_RISK)

    def test_high_without_persistence_is_not_alert(self):
        result = evaluate_system_risk(injury(90), physiology(20))
        self.assertEqual(result.overall_risk_level, "HIGH")
        self.assertEqual(result.system_status.value, "CAUTION")
        self.assertFalse(result.alert)

    def test_invalid_and_missing_inputs(self):
        with self.assertRaises(ValueError):
            evaluate_system_risk(None, physiology(10))
        with self.assertRaises(ValueError):
            evaluate_system_risk(injury(float("nan")), physiology(10))
        self.assertEqual(evaluate_system_risk(injury(200), physiology(-5)).overall_risk_score, 100)

    def test_cooldown_and_acknowledgement(self):
        manager = AlertStateManager(AlertCooldown(30))
        first = evaluate_system_risk(injury(90, True), physiology(10), state_manager=manager,
                                     now=datetime(2026, 1, 1, tzinfo=timezone.utc))
        second = evaluate_system_risk(injury(90, True), physiology(10), state_manager=manager,
                                      now=datetime(2026, 1, 1, 0, 0, 10, tzinfo=timezone.utc))
        self.assertTrue(first.vibration_request)
        self.assertFalse(second.vibration_request)
        manager.acknowledge_alert()
        self.assertTrue(manager.acknowledged)

    def test_transitions_and_signal_deduplication(self):
        tracker = RiskTransitionTracker()
        first = evaluate_system_risk(injury(20), physiology(20), transition_tracker=tracker)
        second = evaluate_system_risk(injury(50), physiology(20), transition_tracker=tracker)
        self.assertEqual(first.signals, [])
        self.assertEqual(len(tracker.events), 1)
        self.assertEqual(tracker.events[0].event_type, "RISK_INCREASED")


if __name__ == "__main__":
    unittest.main()

