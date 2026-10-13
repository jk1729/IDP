import unittest
import sys
import os

# Add parent directory to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
from bridge import parse_line, STATE_CONNECTED, STATE_DISCONNECTED


class TestBridgeParsing(unittest.TestCase):
    def test_parse_valid_line(self):
        line = "FSR,1250,1.0073,22700.0,980,0.7901,31800.0"
        result = parse_line(line)
        self.assertIsNotNone(result)
        self.assertEqual(result["raw1"], 1250)
        self.assertEqual(result["voltage1"], 1.0073)
        self.assertEqual(result["resistance1"], 22700.0)
        self.assertEqual(result["raw2"], 980)
        self.assertEqual(result["voltage2"], 0.7901)
        self.assertEqual(result["resistance2"], 31800.0)
        self.assertTrue(result["connected"])
        self.assertFalse(result["stale"])

    def test_parse_negative_one_resistance_as_unavailable(self):
        line = "FSR,4095,3.3000,-1.0,1,0.0008,-1.0"
        result = parse_line(line)
        self.assertIsNotNone(result)
        self.assertEqual(result["raw1"], 4095)
        self.assertEqual(result["voltage1"], 3.3)
        self.assertIsNone(result["resistance1"])
        self.assertEqual(result["raw2"], 1)
        self.assertEqual(result["voltage2"], 0.0008)
        self.assertIsNone(result["resistance2"])

    def test_ignore_header_line(self):
        line = "FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2"
        result = parse_line(line)
        self.assertIsNone(result)

    def test_reject_malformed_incomplete_lines(self):
        # Missing fields
        self.assertIsNone(parse_line("FSR,100,1.0,500.0"))
        # Extra fields
        self.assertIsNone(parse_line("FSR,100,1.0,500.0,200,2.0,600.0,extra"))
        # Non-numeric
        self.assertIsNone(parse_line("FSR,abc,1.0,500.0,200,2.0,600.0"))
        # Wrong header tag
        self.assertIsNone(parse_line("DATA,1250,1.0073,22700.0,980,0.7901,31800.0"))
        # Empty string
        self.assertIsNone(parse_line(""))
        self.assertIsNone(parse_line("   \r\n "))

    def test_reject_out_of_range_adc_readings(self):
        # 12-bit ADC max is 4095
        self.assertIsNone(parse_line("FSR,5000,1.0,500.0,200,2.0,600.0"))
        self.assertIsNone(parse_line("FSR,-10,1.0,500.0,200,2.0,600.0"))


if __name__ == "__main__":
    unittest.main()

