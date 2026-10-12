"""Optional development plots; matplotlib is only needed when this module is used."""

import csv
import math
from pathlib import Path


def plot_csv(path: str | Path, output_path: str | Path = "sensor_simulator/output/simulated_plots.png") -> Path:
    import matplotlib.pyplot as plt

    with Path(path).open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    time = [index * 0.1 for index in range(len(rows))]
    heel = [float(row["heel_fsr_raw"]) for row in rows]
    forefoot = [float(row["forefoot_fsr_raw"]) for row in rows]
    acceleration = [
        math.sqrt(float(row["accel_x_mps2"]) ** 2 + float(row["accel_y_mps2"]) ** 2 + float(row["accel_z_mps2"]) ** 2)
        for row in rows
    ]
    hr = [float(row["heart_rate_bpm"]) for row in rows]
    temperature = [float(row["temperature_c"]) for row in rows]
    fig, axes = plt.subplots(4, 1, sharex=True, figsize=(11, 9))
    fig.suptitle("SIMULATED DATA - NOT EXPERIMENTAL RESULTS")
    axes[0].plot(time, heel, label="Heel FSR")
    axes[0].plot(time, forefoot, label="Forefoot FSR")
    axes[0].set_ylabel("FSR raw")
    axes[0].legend()
    axes[1].plot(time, acceleration, label="Acceleration magnitude")
    axes[1].set_ylabel("m/s²")
    axes[1].legend()
    axes[2].plot(time, hr, label="Heart rate")
    axes[2].set_ylabel("bpm")
    axes[2].legend()
    axes[3].plot(time, temperature, label="Temperature")
    axes[3].set_ylabel("°C")
    axes[3].set_xlabel("Time (s)")
    axes[3].legend()
    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output, dpi=120)
    plt.close(fig)
    return output

