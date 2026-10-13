import unittest
import threading
import time
import json
import urllib.request
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
import bridge


class TestBridgeServer(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Start bridge server on test port 8799
        cls.port = 8799
        cls.server = bridge.ThreadingHTTPServer(("127.0.0.1", cls.port), bridge.Handler)
        cls.server_thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.server_thread.start()
        time.sleep(0.2)

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def setUp(self):
        with bridge.lock:
            bridge.latest.clear()
            bridge.latest.update({
                "state": bridge.STATE_DISCONNECTED,
                "connected": False,
                "port": None,
                "baud": 115200,
                "updated": None,
                "stale": True,
                "raw1": None,
                "voltage1": None,
                "resistance1": None,
                "raw2": None,
                "voltage2": None,
                "resistance2": None,
                "samples_count": 0,
                "error": "Not connected",
            })

    def test_get_dashboard_html(self):
        url = f"http://127.0.0.1:{self.port}/"
        with urllib.request.urlopen(url, timeout=2.0) as resp:
            self.assertEqual(resp.status, 200)
            self.assertIn("text/html", resp.headers.get("Content-Type", ""))
            content = resp.read().decode("utf-8")
            self.assertIn("ESP32 Dual FSR Monitor", content)
            self.assertIn("GPIO 34", content)
            self.assertIn("GPIO 35", content)

    def test_get_ports(self):
        url = f"http://127.0.0.1:{self.port}/ports"
        with urllib.request.urlopen(url, timeout=2.0) as resp:
            self.assertEqual(resp.status, 200)
            data = json.loads(resp.read().decode("utf-8"))
            self.assertIn("ports", data)
            self.assertIsInstance(data["ports"], list)

    def test_get_data_and_stale_behavior(self):
        url = f"http://127.0.0.1:{self.port}/data"
        with urllib.request.urlopen(url, timeout=2.0) as resp:
            self.assertEqual(resp.status, 200)
            data = json.loads(resp.read().decode("utf-8"))
            self.assertIn("state", data)
            self.assertIn("connected", data)
            self.assertIn("raw1", data)
            self.assertIn("raw2", data)
            self.assertIn("stale", data)
            # When disconnected, stale should be true
            self.assertTrue(data["stale"])

    def test_data_updates_on_valid_parse(self):
        line = "FSR,1250,1.0073,22700.0,980,0.7901,31800.0"
        parsed = bridge.parse_line(line)
        self.assertIsNotNone(parsed)
        with bridge.lock:
            bridge.latest.update(parsed)

        url = f"http://127.0.0.1:{self.port}/data"
        with urllib.request.urlopen(url, timeout=2.0) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            self.assertEqual(data["raw1"], 1250)
            self.assertEqual(data["voltage1"], 1.0073)
            self.assertEqual(data["resistance1"], 22700.0)
            self.assertEqual(data["raw2"], 980)
            self.assertEqual(data["voltage2"], 0.7901)
            self.assertEqual(data["resistance2"], 31800.0)
            self.assertTrue(data["connected"])
            self.assertFalse(data["stale"])


if __name__ == "__main__":
    unittest.main()

