"""Small Phase 7 demonstration using typed Phase 5 and Phase 6 results."""

import json
from injury_risk.schemas import RiskLevel, RiskResult
from physiological.schemas import DehydrationRiskResult, PhysiologicalRiskLevel

from .unified_engine import evaluate_system_risk


def main() -> None:
    injury = RiskResult(68, RiskLevel.MODERATE, {}, [], False, False, 0, False, "injury-risk-v1")
    physiological = DehydrationRiskResult(
        42, PhysiologicalRiskLevel.MODERATE, {}, {}, "EXERCISE",
        "INCREASING", "INCREASING", 42, False, 0, [], False, False, "dehydration-risk-v1",
    )
    print(json.dumps(evaluate_system_risk(injury, physiological).to_dict(), indent=2))


if __name__ == "__main__":
    main()

