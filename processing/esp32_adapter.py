"""ESP32 Physical Hardware Adapter.

Bridges real-time ESP32 FSR measurements into the canonical RawSensorData
contracts and processing pipeline.
"""
import csv
import json
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Iterator, List, Optional
import urllib.request

from analytics.models import RawSensorData, SCHEMA_VERSION, utc_now


class ESP32Adapter:
    """Adapter for ingesting real physical ESP32 dual FSR sensor data."""

    def __init__(self, bridge_url: str = "http://127.0.0.1:8765/data"):
        self.bridge_url = bridge_url

    def fetch_latest(self) -> Optional[Dict[str, Any]]:
        """Fetch current real-time sensor reading from the local Python bridge."""
        try:
            req = urllib.request.Request(self.bridge_url, headers={"User-Agent": "ESP32Adapter/1.0"})
            with urllib.request.urlopen(req, timeout=1.0) as resp:
                if resp.status == 200:
                    data = json.loads(resp.read().decode("utf-8"))
                    return data
        except Exception:
            return None
        return None

    @staticmethod
    def parse_csv_line(line: str) -> Optional[Dict[str, Any]]:
        """
        Parse CSV line: FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2
        """
        line = line.strip()
        parts = line.split(",")
        if len(parts) != 7 or parts[0] != "FSR" or parts[1] == "raw1":
            return None
        try:
            raw1 = int(parts[1])
            v1 = float(parts[2])
            r1 = float(parts[3])
            raw2 = int(parts[4])
            v2 = float(parts[5])
            r2 = float(parts[6])

            if not (0 <= raw1 <= 4095 and 0 <= raw2 <= 4095):
                return None

            return {
                "raw1": raw1,
                "voltage1": v1,
                "resistance1": None if r1 < 0 else r1,
                "raw2": raw2,
                "voltage2": v2,
                "resistance2": None if r2 < 0 else r2,
                "timestamp": utc_now(),
            }
        except (ValueError, IndexError):
            return None

    @staticmethod
    def to_raw_sensor_data(reading: Dict[str, Any]) -> RawSensorData:
        """Convert ESP32 measurement into canonical RawSensorData dataclass."""
        ts = reading.get("timestamp") or utc_now()
        raw1 = int(reading.get("raw1", 0))
        raw2 = int(reading.get("raw2", 0))

        return RawSensorData(
            timestamp=ts,
            heel_fsr_raw=raw1,
            forefoot_fsr_raw=raw2,
            accel_x_mps2=0.0,
            accel_y_mps2=0.0,
            accel_z_mps2=9.81,
            gyro_x_dps=0.0,
            gyro_y_dps=0.0,
            gyro_z_dps=0.0,
            heart_rate_bpm=72.0,
            temperature_c=36.6,
            device_id="ESP32_FSR",
            data_source="ESP32_HARDWARE",
            schema_version=SCHEMA_VERSION,
        )

    @classmethod
    def convert_csv_session(cls, input_csv_path: str | Path, output_raw_csv_path: str | Path) -> int:
        """
        Convert a recorded ESP32 CSV file (FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2)
        into the processing pipeline's expected RawSensorData CSV format.
        """
        records: List[RawSensorData] = []
        with Path(input_csv_path).open("r", encoding="utf-8") as f:
            for line in f:
                parsed = cls.parse_csv_line(line)
                if parsed:
                    records.append(cls.to_raw_sensor_data(parsed))

        if not records:
            raise ValueError("No valid ESP32 sensor records found in input file.")

        out_path = Path(output_raw_csv_path)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with out_path.open("w", encoding="utf-8", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=[
                "timestamp", "heel_fsr_raw", "forefoot_fsr_raw",
                "accel_x_mps2", "accel_y_mps2", "accel_z_mps2",
                "gyro_x_dps", "gyro_y_dps", "gyro_z_dps",
                "heart_rate_bpm", "temperature_c", "device_id",
                "data_source", "schema_version"
            ])
            writer.writeheader()
            for rec in records:
                writer.writerow({
                    "timestamp": rec.timestamp,
                    "heel_fsr_raw": rec.heel_fsr_raw,
                    "forefoot_fsr_raw": rec.forefoot_fsr_raw,
                    "accel_x_mps2": rec.accel_x_mps2,
                    "accel_y_mps2": rec.accel_y_mps2,
                    "accel_z_mps2": rec.accel_z_mps2,
                    "gyro_x_dps": rec.gyro_x_dps,
                    "gyro_y_dps": rec.gyro_y_dps,
                    "gyro_z_dps": rec.gyro_z_dps,
                    "heart_rate_bpm": rec.heart_rate_bpm,
                    "temperature_c": rec.temperature_c,
                    "device_id": rec.device_id,
                    "data_source": rec.data_source,
                    "schema_version": rec.schema_version,
                })
        return len(records)

