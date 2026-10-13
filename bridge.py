#!/usr/bin/env python3
"""
Local bridge: ESP32 Hardware (Bluetooth Classic SPP or USB Serial) -> Local HTTP & SSE API.
Exposes real-time sensor readings on:
    http://127.0.0.1:8765/data     (JSON API)
    http://127.0.0.1:8765/ports    (List available COM ports)
    http://127.0.0.1:8765/events   (Server-Sent Events stream)
    http://127.0.0.1:8765/         (Web Dashboard)
"""
import argparse
import collections
import json
import os
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Dict, List, Optional

import serial
from serial.tools import list_ports

# Connection state constants
STATE_DISCONNECTED = "DISCONNECTED"
STATE_CONNECTING = "CONNECTING"
STATE_CONNECTED = "CONNECTED"
STATE_ERROR = "ERROR"

STALE_THRESHOLD_SECONDS = 1.5

latest: Dict[str, Any] = {
    "state": STATE_DISCONNECTED,
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
}

# In-memory history buffer (last 100 samples)
history_buffer = collections.deque(maxlen=100)

lock = threading.Lock()
stop_event = threading.Event()
serial_reconnect_event = threading.Event()
requested_port: Optional[str] = None

# SSE client listeners
sse_clients = []
sse_lock = threading.Lock()


def get_available_ports() -> List[Dict[str, str]]:
    """List available COM ports with description and hardware IDs."""
    ports = []
    for p in list_ports.comports():
        ports.append({
            "device": p.device,
            "description": p.description or p.device,
            "hwid": p.hwid or ""
        })
    return ports


def find_default_port(preferred_name: str = "ESP32_FSR") -> Optional[str]:
    """Auto-detect ESP32 Bluetooth SPP or USB Serial port."""
    ports = list_ports.comports()
    # 1. Search for ESP32 in description or HWID
    for p in ports:
        desc = (p.description or "").lower()
        hwid = (p.hwid or "").lower()
        if preferred_name.lower() in desc or preferred_name.lower() in hwid:
            return p.device

    # 2. Search for standard Bluetooth link ports
    for p in ports:
        desc = (p.description or "").lower()
        if "bluetooth" in desc:
            return p.device

    # 3. Search for common USB serial chips (CP210x, CH340, FTDI, USB Serial)
    for p in ports:
        desc = (p.description or "").lower()
        if any(keyword in desc for keyword in ("cp210", "ch340", "ftdi", "usb serial", "silicon labs", "uart")):
            return p.device

    # 4. Fallback to first available port
    if ports:
        return ports[0].device
    return None


def parse_line(line: str) -> Optional[Dict[str, Any]]:
    """
    Parse one CSV line transmitted by ESP32 firmware:
    Expected CSV: FSR,raw1,voltage1,resistance1,raw2,voltage2,resistance2
    Example: FSR,1250,1.0073,22700.0,980,0.7901,31800.0

    Returns parsed dict or None if invalid/corrupt.
    """
    line = line.strip()
    if not line:
        return None

    parts = line.split(",")
    if len(parts) != 7 or parts[0] != "FSR":
        return None

    # Ignore header line if sent by firmware on boot
    if parts[1].strip() == "raw1":
        return None

    try:
        raw1 = int(parts[1].strip())
        v1 = float(parts[2].strip())
        r1 = float(parts[3].strip())

        raw2 = int(parts[4].strip())
        v2 = float(parts[5].strip())
        r2 = float(parts[6].strip())

        # Validate range constraints
        # ADC is 12-bit (0..4095)
        if not (0 <= raw1 <= 4095 and 0 <= raw2 <= 4095):
            return None
        # Voltage is 0..3.6V for ESP32
        if not (0.0 <= v1 <= 3.6 and 0.0 <= v2 <= 3.6):
            return None

        # Resistance: -1.0 means calculation unreliable near limits (near 0 or near 4095)
        res1 = None if r1 < 0 else round(r1, 1)
        res2 = None if r2 < 0 else round(r2, 1)

        now = time.time()
        return {
            "raw1": raw1,
            "voltage1": round(v1, 4),
            "resistance1": res1,
            "raw2": raw2,
            "voltage2": round(v2, 4),
            "resistance2": res2,
            "updated": now,
            "stale": False,
            "connected": True,
            "state": STATE_CONNECTED,
            "error": None,
        }
    except (ValueError, IndexError):
        return None


def broadcast_sse(data_json: str):
    """Send SSE message to connected browser clients."""
    with sse_lock:
        dead_clients = []
        msg = f"data: {data_json}\n\n".encode("utf-8")
        for client in sse_clients:
            try:
                client.write(msg)
                client.flush()
            except Exception:
                dead_clients.append(client)
        for client in dead_clients:
            if client in sse_clients:
                sse_clients.remove(client)


def serial_worker(initial_port: Optional[str], baud: int):
    """
    Serial reader worker thread.
    Handles auto-reconnection, port switching, and reading incoming lines.
    """
    global requested_port
    current_port = initial_port

    while not stop_event.is_set():
        if requested_port:
            current_port = requested_port
            requested_port = None

        if not current_port:
            current_port = find_default_port()

        if not current_port:
            with lock:
                latest["state"] = STATE_DISCONNECTED
                latest["connected"] = False
                latest["stale"] = True
                latest["error"] = "No serial/Bluetooth COM ports found on system"
            stop_event.wait(2)
            continue

        with lock:
            latest["state"] = STATE_CONNECTING
            latest["connected"] = False
            latest["stale"] = True
            latest["port"] = current_port
            latest["baud"] = baud
            latest["error"] = None

        print(f"Connecting to ESP32 on {current_port} at {baud} baud...")

        ser = None
        try:
            ser = serial.Serial(current_port, baud, timeout=1.0)
            print(f"Successfully opened {current_port}. Awaiting ESP32 FSR stream...")

            with lock:
                latest["state"] = STATE_CONNECTED
                latest["connected"] = True
                latest["error"] = None

            # Reset input buffer to discard old buffered bytes
            try:
                ser.reset_input_buffer()
            except Exception:
                pass

            last_data_time = time.time()

            while not stop_event.is_set() and not serial_reconnect_event.is_set():
                # Check for dynamic port switch request
                if requested_port and requested_port != current_port:
                    break

                try:
                    line_bytes = ser.readline()
                except Exception as read_exc:
                    raise IOError(f"Serial read error on {current_port}: {read_exc}")

                if not line_bytes:
                    # Timeout occurred with no data
                    now = time.time()
                    if now - last_data_time > STALE_THRESHOLD_SECONDS:
                        with lock:
                            latest["stale"] = True
                    continue

                line = line_bytes.decode("utf-8", errors="ignore").strip()
                if not line:
                    continue

                parsed = parse_line(line)
                if parsed:
                    last_data_time = parsed["updated"]
                    with lock:
                        latest.update(parsed)
                        latest["samples_count"] = latest.get("samples_count", 0) + 1
                        history_item = {
                            "time": parsed["updated"],
                            "raw1": parsed["raw1"],
                            "v1": parsed["voltage1"],
                            "r1": parsed["resistance1"],
                            "raw2": parsed["raw2"],
                            "v2": parsed["voltage2"],
                            "r2": parsed["resistance2"],
                        }
                        history_buffer.append(history_item)
                        payload_snapshot = dict(latest)

                    # Stream to SSE clients
                    broadcast_sse(json.dumps(payload_snapshot))
                else:
                    # Malformed or debug line
                    if not line.startswith("FSR,raw1"):
                        print(f"[{current_port}] Non-data line ignored: {line}")

        except Exception as exc:
            print(f"Connection error on {current_port}: {exc}")
            with lock:
                latest["state"] = STATE_ERROR
                latest["connected"] = False
                latest["stale"] = True
                latest["error"] = str(exc)
        finally:
            if ser:
                try:
                    ser.close()
                except Exception:
                    pass
            serial_reconnect_event.clear()

        # Wait before retrying connection
        if not stop_event.is_set():
            time.sleep(2)


class Handler(BaseHTTPRequestHandler):
    """HTTP Request Handler providing REST and SSE endpoints."""

    def _headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate")

    def do_OPTIONS(self):
        self.send_response(204)
        self._headers()
        self.end_headers()

    def do_GET(self):
        # Route 1: SSE Live Stream
        if self.path == "/events":
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Connection", "keep-alive")
            self.send_header("Cache-Control", "no-cache")
            self._headers()
            self.end_headers()

            with sse_lock:
                sse_clients.append(self.wfile)

            # Keep connection open until client disconnects
            try:
                while not stop_event.is_set():
                    time.sleep(1)
            except Exception:
                pass
            finally:
                with sse_lock:
                    if self.wfile in sse_clients:
                        sse_clients.remove(self.wfile)
            return

        # Route 2: JSON Sensor Data
        if self.path == "/data" or self.path.startswith("/data?"):
            now = time.time()
            with lock:
                # Disconnected or no readings received recently => stale
                if not latest.get("connected", False) or latest["updated"] is None:
                    latest["stale"] = True
                else:
                    latest["stale"] = (now - latest["updated"]) > STALE_THRESHOLD_SECONDS
                payload = dict(latest)
                payload["server_time"] = now
                payload["history"] = list(history_buffer)

            body = json.dumps(payload).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._headers()
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        # Route 3: List Available COM Ports
        if self.path == "/ports":
            ports = get_available_ports()
            with lock:
                current_p = latest.get("port")
            body = json.dumps({"current_port": current_p, "ports": ports}).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._headers()
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        # Route 4: Switch / Reconnect Port via GET: /connect?port=COM...
        if self.path.startswith("/connect?"):
            global requested_port
            query = self.path.split("?", 1)[1]
            params = dict(part.split("=", 1) for part in query.split("&") if "=" in part)
            new_port = params.get("port")
            if new_port:
                requested_port = new_port
                serial_reconnect_event.set()
                resp = {"status": "ok", "connecting_to": new_port}
            else:
                serial_reconnect_event.set()
                resp = {"status": "ok", "action": "reconnect"}

            body = json.dumps(resp).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._headers()
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        # Route 5: Web Dashboard
        if self.path in ("/", "/dashboard.html", "/index.html"):
            dashboard_paths = [
                os.path.join(os.path.dirname(__file__), "dashboard.html"),
                os.path.join(os.getcwd(), "dashboard.html"),
                os.path.join(os.getcwd(), "IDP", "dashboard.html"),
            ]
            content = None
            for p in dashboard_paths:
                if os.path.exists(p):
                    try:
                        with open(p, "rb") as f:
                            content = f.read()
                            break
                    except Exception:
                        pass

            if content:
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self._headers()
                self.send_header("Content-Length", str(len(content)))
                self.end_headers()
                self.wfile.write(content)
                return
            else:
                body = (
                    b"<!DOCTYPE html><html><body>"
                    b"<h2>ESP32 FSR Bridge is Running</h2>"
                    b"<p>Please place dashboard.html in the same directory.</p>"
                    b"<p><a href='/data'>View JSON /data</a></p>"
                    b"</body></html>"
                )
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self._headers()
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
                return

        # 404 for unknown endpoints
        self.send_response(404)
        self.end_headers()

    def do_POST(self):
        if self.path == "/connect":
            global requested_port
            content_len = int(self.headers.get("Content-Length", 0))
            body_bytes = self.rfile.read(content_len) if content_len > 0 else b""
            try:
                data = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
                new_port = data.get("port")
                if new_port:
                    requested_port = new_port
                serial_reconnect_event.set()
                resp = {"status": "ok", "connecting_to": requested_port}
            except Exception as e:
                resp = {"status": "error", "message": str(e)}

            body = json.dumps(resp).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._headers()
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        self.send_response(404)
        self.end_headers()

    def log_message(self, fmt, *args):
        # Suppress noisy HTTP polling logs in console
        pass


def main():
    parser = argparse.ArgumentParser(description="ESP32 FSR Bluetooth/Serial to Local HTTP Bridge")
    parser.add_argument("--port", help="Serial/Bluetooth COM port (e.g. COM3, COM33). Auto-detected if omitted.")
    parser.add_argument("--baud", type=int, default=115200, help="Serial baud rate (default: 115200)")
    parser.add_argument("--http-port", type=int, default=8765, help="HTTP server port (default: 8765)")
    parser.add_argument("--host", default="0.0.0.0", help="HTTP server host address (default: 0.0.0.0)")
    args = parser.parse_args()

    port = args.port
    if not port:
        detected = find_default_port()
        if detected:
            print(f"Auto-selected COM port: {detected}")
            port = detected
        else:
            print("No serial ports found yet. Waiting for ESP32 connection...")

    print("==================================================================")
    print("  ESP32 Dual FSR Real-Time Bridge")
    print(f"  Target Port   : {port or 'Auto-detect'}")
    print(f"  Baud Rate     : {args.baud}")
    print(f"  Dashboard UI  : http://localhost:{args.http_port}/")
    print(f"  JSON API      : http://localhost:{args.http_port}/data")
    print(f"  COM Ports API : http://localhost:{args.http_port}/ports")
    print("==================================================================")

    thread = threading.Thread(target=serial_worker, args=(port, args.baud), daemon=True)
    thread.start()

    server = ThreadingHTTPServer((args.host, args.http_port), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping bridge server...")
    finally:
        stop_event.set()
        serial_reconnect_event.set()
        server.shutdown()
        server.server_close()


if __name__ == "__main__":
    main()
