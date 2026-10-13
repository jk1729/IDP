package com.idp.smartgait.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.idp.smartgait.data.*
import com.idp.smartgait.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.abs

interface RiskRepository {
    val risks: Flow<RiskPacket>
    val lives: Flow<LiveDataPacket>
    val statuses: Flow<StatusPacket>
    val connection: Flow<ConnectionState>
    suspend fun start()
    suspend fun stop()
    suspend fun sendCommand(command: BleCommand): Boolean
    fun selectScenario(s: SimulationScenario)
}

enum class BleCommand(val wire: String) {
    PING("PING"),
    GET_STATUS("GET_STATUS"),
    START_SESSION("START_SESSION"),
    STOP_SESSION("STOP_SESSION"),
    START_CALIBRATION("START_CALIBRATION"),
    STOP_CALIBRATION("STOP_CALIBRATION"),
    VIBRATE_TEST("VIBRATE_TEST"),
    CALIBRATE("CALIBRATE")
}

class Repository(
    private val simulation: SimulationSource = SimulationSource(),
    private val bridgeEndpoints: List<String> = listOf(
        "http://10.0.2.2:8765/data",
        "http://127.0.0.1:8765/data",
        "http://localhost:8765/data"
    )
) : RiskRepository {

    private val _risks = MutableSharedFlow<RiskPacket>(extraBufferCapacity = 1)
    override val risks = _risks.asSharedFlow()

    private val _lives = MutableSharedFlow<LiveDataPacket>(extraBufferCapacity = 1)
    override val lives = _lives.asSharedFlow()

    private val _statuses = MutableSharedFlow<StatusPacket>(extraBufferCapacity = 1)
    override val statuses = _statuses.asSharedFlow()

    private val _connection = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connection = _connection.asStateFlow()

    private var activeJob: Job? = null
    private var isSimulationMode = false
    private var workingEndpoint: String? = null

    companion object {
        // Standard Bluetooth Serial Port Profile (SPP) UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override suspend fun start() {
        if (activeJob?.isActive == true) return

        activeJob = CoroutineScope(Dispatchers.IO).launch {
            if (isSimulationMode) {
                runSimulationLoop()
            } else {
                runHardwarePipeline()
            }
        }
    }

    private suspend fun runHardwarePipeline() {
        // 1. Attempt direct Bluetooth Classic SPP if running on an Android device paired with ESP32_FSR
        val btSuccess = tryDirectBluetoothClassic()
        if (btSuccess) return

        // 2. Otherwise, connect via the local Python bridge (ideal for emulator, PC testing, or networked setups)
        runHardwareBridgeLoop()
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryDirectBluetoothClassic(): Boolean {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!adapter.isEnabled) return false

            val bondedDevices = adapter.bondedDevices ?: return false
            val esp32Device = bondedDevices.firstOrNull {
                it.name?.contains("ESP32_FSR", ignoreCase = true) == true
            } ?: return false

            _connection.value = ConnectionState.CONNECTING
            var socket: BluetoothSocket? = null
            try {
                socket = esp32Device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            } catch (e: Exception) {
                socket?.close()
                return false
            }

            _connection.value = ConnectionState.CONNECTED
            val reader = BufferedReader(InputStreamReader(socket.inputStream))

            try {
                while (activeJob?.isActive == true) {
                    val line = reader.readLine() ?: break
                    emitFromCsvLine(line, esp32Device.name ?: "ESP32_FSR (Bluetooth)")
                }
            } finally {
                socket.close()
                _connection.value = ConnectionState.DISCONNECTED
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private suspend fun runHardwareBridgeLoop() {
        _connection.value = ConnectionState.CONNECTING

        while (activeJob?.isActive == true) {
            val jsonObj = fetchFromBridge()
            if (jsonObj != null) {
                val isConnected = jsonObj.optBoolean("connected", false)
                val isStale = jsonObj.optBoolean("stale", true)

                if (isConnected && !isStale && !jsonObj.isNull("raw1") && !jsonObj.isNull("raw2")) {
                    _connection.value = ConnectionState.CONNECTED

                    val raw1 = jsonObj.getInt("raw1")
                    val raw2 = jsonObj.getInt("raw2")
                    val portName = jsonObj.optString("port", "ESP32_FSR")
                    val updatedTs = (jsonObj.optDouble("updated", System.currentTimeMillis() / 1000.0) * 1000).toLong()

                    emitHardwareReadings(raw1, raw2, updatedTs, portName)
                } else {
                    _connection.value = ConnectionState.DISCONNECTED
                }
            } else {
                _connection.value = ConnectionState.DISCONNECTED
            }

            delay(100) // 100ms hardware polling matching ESP32 firmware rate
        }
    }

    private suspend fun emitFromCsvLine(line: String, deviceName: String) {
        val parts = line.trim().split(",")
        if (parts.size != 7 || parts[0] != "FSR" || parts[1] == "raw1") return

        try {
            val raw1 = parts[1].trim().toInt()
            val raw2 = parts[4].trim().toInt()
            if (raw1 in 0..4095 && raw2 in 0..4095) {
                emitHardwareReadings(raw1, raw2, System.currentTimeMillis(), deviceName)
            }
        } catch (_: Exception) {}
    }

    private suspend fun emitHardwareReadings(raw1: Int, raw2: Int, timestamp: Long, deviceName: String) {
        _lives.emit(
            LiveDataPacket(
                ts = timestamp,
                hr = null,
                temp = null,
                heel = raw1.toFloat(),
                forefoot = raw2.toFloat(),
                acc = null,
                cad = null,
                mode = "ESP32_HARDWARE"
            )
        )

        _statuses.emit(
            StatusPacket(
                device = deviceName,
                firmware = "1.0.0",
                battery = 100,
                connected = true,
                uptime = timestamp,
                sensor_ready = true,
                mode = "ESP32_HARDWARE"
            )
        )

        val total = raw1 + raw2
        val imbalance = if (total > 50) abs(raw1 - raw2).toFloat() / total.toFloat() else 0f
        val injuryScore = (imbalance * 70f).coerceIn(0f, 100f)
        val level = when {
            injuryScore >= 60f -> "HIGH"
            injuryScore >= 35f -> "MODERATE"
            else -> "LOW"
        }

        _risks.emit(
            RiskPacket(
                ts = timestamp,
                injury = injuryScore,
                physio = 0f,
                overall = injuryScore,
                injury_level = level,
                physio_level = "LOW",
                overall_level = level,
                status = "HARDWARE_ACTIVE",
                alert = level == "HIGH",
                alert_type = if (level == "HIGH") "INJURY" else "NONE",
                priority = if (level == "HIGH") "HIGH" else "LOW",
                message = if (level == "HIGH") "High gait loading imbalance detected on ESP32 FSRs." else ""
            )
        )
    }

    private suspend fun runSimulationLoop() {
        _connection.value = ConnectionState.CONNECTED
        simulation.packets().collect { risk ->
            _risks.emit(risk)
            val imbalance = (risk.injury / 100f).coerceIn(0f, 1f)
            _lives.emit(
                LiveDataPacket(
                    ts = risk.ts,
                    hr = 72f + risk.physio * .4f,
                    temp = 36.6f + risk.physio * .01f,
                    heel = 420f * (1f - imbalance * .25f),
                    forefoot = 310f * (1f + imbalance * .25f),
                    acc = 1.21f,
                    cad = 168f,
                    mode = "SIMULATION"
                )
            )
            _statuses.emit(
                StatusPacket(
                    device = "SMART_GAIT_SIM",
                    firmware = "0.8.0",
                    battery = 97,
                    connected = true,
                    uptime = risk.ts,
                    sensor_ready = false,
                    mode = "SIMULATION"
                )
            )
        }
    }

    private fun fetchFromBridge(): JSONObject? {
        val endpointsToTry = workingEndpoint?.let { listOf(it) + bridgeEndpoints.filter { ep -> ep != it } }
            ?: bridgeEndpoints

        for (endpoint in endpointsToTry) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 300
                    readTimeout = 300
                    useCaches = false
                }
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val content = reader.readText()
                    reader.close()
                    workingEndpoint = endpoint
                    return JSONObject(content)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    override suspend fun stop() {
        activeJob?.cancel()
        activeJob = null
        _connection.value = ConnectionState.DISCONNECTED
    }

    override suspend fun sendCommand(command: BleCommand): Boolean = true

    override fun selectScenario(s: SimulationScenario) {
        simulation.scenario = s
        isSimulationMode = true
        activeJob?.cancel()
        activeJob = CoroutineScope(Dispatchers.IO).launch {
            runSimulationLoop()
        }
    }
}
