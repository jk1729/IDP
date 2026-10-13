package com.idp.smartgait.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.idp.smartgait.ble.BleScanner
import com.idp.smartgait.ble.BleUuids
import com.idp.smartgait.ble.GattDataSource
import com.idp.smartgait.data.*
import com.idp.smartgait.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
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
    private val context: Context? = null,
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
    private var currentGatt: GattDataSource? = null

    companion object {
        var appContext: Context? = null
    }

    private val effectiveContext: Context? get() = context ?: appContext

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

    @SuppressLint("MissingPermission")
    private suspend fun runHardwarePipeline() {
        val ctx = effectiveContext
        val adapter = BluetoothAdapter.getDefaultAdapter()

        // If running in an environment without Bluetooth hardware (e.g. pure emulator), fall back to bridge
        if (adapter == null) {
            runHardwareBridgeLoop()
            return
        }

        while (activeJob?.isActive == true) {
            if (!adapter.isEnabled) {
                _connection.value = ConnectionState.ERROR
                delay(2000)
                continue
            }

            _connection.value = ConnectionState.SCANNING

            var targetDevice: BluetoothDevice? = null
            val scanCompleter = CompletableDeferred<BluetoothDevice?>()

            val scanner = BleScanner(
                adapter = adapter,
                onDevice = { result ->
                    val device = result.device
                    if (scanCompleter.isActive) {
                        scanCompleter.complete(device)
                    }
                },
                onError = { _ ->
                    if (scanCompleter.isActive) {
                        scanCompleter.complete(null)
                    }
                }
            )

            try {
                scanner.start()
                // Wait up to 10 seconds for ESP32 discovery
                withTimeoutOrNull(10000) {
                    targetDevice = scanCompleter.await()
                }
            } finally {
                scanner.stop()
            }

            if (activeJob?.isActive != true) break

            if (targetDevice != null) {
                val device = targetDevice!!
                _connection.value = ConnectionState.CONNECTING

                val gattSource = GattDataSource(ctx, device)
                currentGatt = gattSource

                try {
                    coroutineScope {
                        // Forward connection state from GATT datasource
                        val stateJob = launch {
                            gattSource.connectionState.collect { state ->
                                _connection.value = state
                            }
                        }

                        // Collect and parse live notifications from the ESP32
                        val packetsJob = launch {
                            gattSource.packets.collect { (uuid, bytes) ->
                                if (uuid == BleUuids.FSR_CHARACTERISTIC) {
                                    val text = bytes.toString(Charsets.UTF_8)
                                    val packet = PacketParser.parseEsp32BleString(text)
                                    if (packet != null) {
                                        emitBleReadings(packet, device.name ?: BleUuids.DEVICE_NAME)
                                    }
                                }
                            }
                        }

                        gattSource.connect()

                        // Monitor connection while active
                        while (activeJob?.isActive == true &&
                            gattSource.connectionState.value != ConnectionState.DISCONNECTED &&
                            gattSource.connectionState.value != ConnectionState.ERROR
                        ) {
                            delay(200)
                        }

                        stateJob.cancel()
                        packetsJob.cancel()
                    }
                } finally {
                    gattSource.close()
                    currentGatt = null
                }
            }

            _connection.value = ConnectionState.DISCONNECTED

            // Controlled delay before retrying scan (prevents rapid retry loops)
            if (activeJob?.isActive == true) {
                delay(3000)
            }
        }
    }

    private suspend fun emitBleReadings(packet: LiveDataPacket, deviceName: String) {
        _lives.emit(packet)

        val raw1 = packet.heel?.toInt() ?: 0
        val raw2 = packet.forefoot?.toInt() ?: 0

        _statuses.emit(
            StatusPacket(
                device = deviceName,
                firmware = "1.0.0",
                battery = 100,
                connected = true,
                uptime = packet.ts,
                sensor_ready = true,
                mode = "ESP32_BLE"
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
                ts = packet.ts,
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

                    val packet = LiveDataPacket(
                        ts = updatedTs,
                        heel = raw1.toFloat(),
                        forefoot = raw2.toFloat(),
                        mode = "BRIDGE_HARDWARE"
                    )
                    emitBleReadings(packet, portName)
                } else {
                    _connection.value = ConnectionState.DISCONNECTED
                }
            } else {
                _connection.value = ConnectionState.DISCONNECTED
            }

            delay(200)
        }
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
        currentGatt?.close()
        currentGatt = null
        _connection.value = ConnectionState.DISCONNECTED
    }

    override suspend fun sendCommand(command: BleCommand): Boolean {
        return currentGatt?.writeCommand(command) ?: true
    }

    override fun selectScenario(s: SimulationScenario) {
        simulation.scenario = s
        isSimulationMode = true
        activeJob?.cancel()
        currentGatt?.close()
        currentGatt = null
        activeJob = CoroutineScope(Dispatchers.IO).launch {
            runSimulationLoop()
        }
    }

    fun switchToHardware() {
        isSimulationMode = false
        activeJob?.cancel()
        currentGatt?.close()
        currentGatt = null
        activeJob = CoroutineScope(Dispatchers.IO).launch {
            runHardwarePipeline()
        }
    }
}

