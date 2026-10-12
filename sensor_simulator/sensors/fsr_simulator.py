"""Two-channel heel and forefoot FSR simulation."""

import math
import random

from sensor_simulator.models import ActivityState


class FSRSensorSimulator:
    def __init__(self, rng: random.Random, minimum: int = 0, maximum: int = 4095):
        self.rng, self.minimum, self.maximum = rng, minimum, maximum

    def read(self, seconds: float, state: ActivityState) -> tuple[int, int]:
        if state in (ActivityState.REST, ActivityState.STANDING):
            base = 80 if state == ActivityState.REST else 1100
            return self._bounded(base + self.rng.gauss(0, 18)), self._bounded(base + self.rng.gauss(0, 18))
        cycle = 2.0 * math.pi * (2.4 if state == ActivityState.EXERCISE else 1.8) * seconds
        heel_wave = max(0.0, math.sin(cycle))
        forefoot_wave = max(0.0, math.sin(cycle + 0.85))
        if state == ActivityState.ABNORMAL_WALKING:
            heel_wave *= 0.55
            forefoot_wave *= 1.25
        elif state == ActivityState.WALKING and int(seconds * 2) % 2:
            heel_wave *= 1.15
        heel = 180 + 2200 * heel_wave
        forefoot = 220 + 2700 * forefoot_wave
        if state == ActivityState.EXERCISE:
            heel *= 1.15
            forefoot *= 1.15
        return self._bounded(heel + self.rng.gauss(0, 35)), self._bounded(forefoot + self.rng.gauss(0, 35))

    def _bounded(self, value: float) -> int:
        return int(max(self.minimum, min(self.maximum, round(value))))

