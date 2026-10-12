"""Slow temperature simulation with small measurement noise."""

import random

from sensor_simulator.models import ActivityState


class TemperatureSimulator:
    TARGETS = {
        ActivityState.REST: 30.0,
        ActivityState.STANDING: 30.2,
        ActivityState.WALKING: 31.0,
        ActivityState.EXERCISE: 32.2,
        ActivityState.ABNORMAL_WALKING: 31.2,
    }

    def __init__(self, rng: random.Random, initial_c: float = 30.0):
        self.rng, self.current_c = rng, initial_c

    def read(self, state: ActivityState) -> float:
        self.current_c += (self.TARGETS[state] - self.current_c) * 0.004
        return self.current_c + self.rng.gauss(0, 0.025)

