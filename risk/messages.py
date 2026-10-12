"""Safe user-facing messages for unified risk patterns."""

from .schemas import AlertType


def message_for(alert_type: AlertType, alert: bool) -> str:
    if not alert:
        return "Monitor the athlete's current pattern."
    return {
        AlertType.INJURY_RISK: "Persistent abnormal gait/loading pattern detected.",
        AlertType.PHYSIOLOGICAL_RISK: "Persistent physiological deviation detected.",
        AlertType.BOTH: "Persistent gait/loading and physiological deviations detected.",
    }.get(alert_type, "Monitor the athlete's current pattern.")

