"""Raw MPU6050-style walking, standing, and irregular-motion simulation."""

import math
import random

from sensor_simulator.models import ActivityState


class IMUSensorSimulator:
    def __init__(self, rng: random.Random):
        self.rng = rng

    def read(self, seconds: float, state: ActivityState) -> tuple[float, float, float, float, float, float]:
        if state in (ActivityState.REST, ActivityState.STANDING):
            movement = 0.03 if state == ActivityState.REST else 0.12
            return (
                self.rng.gauss(0, movement), self.rng.gauss(0, movement),
                9.81 + self.rng.gauss(0, movement),
                self.rng.gauss(0, movement * 10), self.rng.gauss(0, movement * 10),
                self.rng.gauss(0, movement * 10),
            )
        frequency = 2.4 if state == ActivityState.EXERCISE else 1.8
        phase = 2.0 * math.pi * frequency * seconds
        amplitude = 2.8 if state == ActivityState.EXERCISE else 1.8
        if state == ActivityState.ABNORMAL_WALKING:
            amplitude *= 0.8
            phase += 0.25 * math.sin(seconds * 0.7)
        return (
            amplitude * math.sin(phase) + self.rng.gauss(0, 0.12),
            0.7 * math.cos(phase) + self.rng.gauss(0, 0.12),
            9.81 + amplitude * 0.75 * max(0, math.sin(phase)) + self.rng.gauss(0, 0.18),
            18 * math.sin(phase) + self.rng.gauss(0, 1.2),
            12 * math.cos(phase) + self.rng.gauss(0, 1.2),
            25 * math.sin(phase + 0.3) + self.rng.gauss(0, 1.2),
        )

