import unittest
import tempfile
from pathlib import Path
from processing.esp32_adapter import ESP32Adapter


class TestESP32Adapter(unittest.TestCase):
    def test_parse_csv_line(self):
        line = "FSR,1250,1.0073,22700.0,980,0.7901,31800.0"
        parsed = ESP32Adapter.parse_csv_line(line)
        self.assertIsNotNone(parsed)
        self.assertEqual(parsed["raw1"], 1250)
        self.assertEqual(parsed["voltage1"], 1.0073)
        self.assertEqual(parsed["resistance1"], 22700.0)
        self.assertEqual(parsed["raw2"], 980)
        self.assertEqual(parsed["voltage2"], 0.7901)
        self.assertEqual(parsed["resistance2"], 31800.0)

    def test_to_raw_sensor_data(self):
        reading = {
            "raw1": 1500,
            "voltage1": 1.20,
            "resistance1": 18000.0,
            "raw2": 950,
            "voltage2": 0.76,
            "resistance2": 25000.0,
        }
        raw_data = ESP32Adapter.to_raw_sensor_data(reading)
        self.assertEqual(raw_data.heel_fsr_raw, 1500)
        self.assertEqual(raw_data.forefoot_fsr_raw, 950)
        self.assertEqual(raw_data.data_source, "ESP32_HARDWARE")
        self.assertEqual(raw_data.device_id, "ESP32_FSR")

    def test_convert_csv_session(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            in_file = Path(tmp_dir) / "esp32_stream.csv"
            out_file = Path(tmp_dir) / "raw_data.csv"
            in_file.write_text(
                "FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2\n"
                "FSR,100,0.08,120000.0,200,0.16,90000.0\n"
                "FSR,1500,1.20,18000.0,1800,1.45,15000.0\n"
            )
            count = ESP32Adapter.convert_csv_session(in_file, out_file)
            self.assertEqual(count, 2)
            self.assertTrue(out_file.exists())
            content = out_file.read_text()
            self.assertIn("ESP32_HARDWARE", content)
            self.assertIn("1500", content)


if __name__ == "__main__":
    unittest.main()

