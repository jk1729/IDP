"""Smooth heart-rate simulation with activity-dependent targets."""

import random

from sensor_simulator.models import ActivityState


class HeartRateSimulator:
    TARGETS = {
        ActivityState.REST: 72.0,
        ActivityState.STANDING: 82.0,
        ActivityState.WALKING: 112.0,
        ActivityState.EXERCISE: 155.0,
        ActivityState.ABNORMAL_WALKING: 125.0,
    }

    def __init__(self, rng: random.Random, initial_bpm: float = 72.0):
        self.rng, self.current_bpm = rng, initial_bpm

    def read(self, state: ActivityState) -> float:
        self.current_bpm += (self.TARGETS[state] - self.current_bpm) * 0.025
        return self.current_bpm + self.rng.gauss(0, 0.8)

