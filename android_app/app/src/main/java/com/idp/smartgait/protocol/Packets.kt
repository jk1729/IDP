package com.idp.smartgait.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RiskPacket(
    val type: String = "risk",
    val ts: Long = 0,
    val injury: Float = 0f,
    val injury_level: String = "",
    val physio: Float = 0f,
    val physio_level: String = "",
    val overall: Float = 0f,
    val overall_level: String = "",
    val status: String = "",
    val alert: Boolean = false,
    val alert_type: String = "",
    val priority: String = "",
    val message: String = ""
)

@Serializable
data class LiveDataPacket(
    val type: String = "live",
    val ts: Long = 0,
    val hr: Float? = null,
    val temp: Float? = null,
    val heel: Float? = null,
    val forefoot: Float? = null,
    val acc: Float? = null,
    val cad: Float? = null,
    val mode: String = "",
    val v1: Float? = null,
    val r1: Float? = null,
    val v2: Float? = null,
    val r2: Float? = null
)

data class Esp32FsrReading(
    val raw1: Int,
    val v1: Float,
    val r1: Float?,
    val raw2: Int,
    val v2: Float,
    val r2: Float?
)

@Serializable
data class StatusPacket(
    val type: String = "status",
    val device: String = "",
    val firmware: String = "",
    val battery: Int = 0,
    val connected: Boolean = false,
    val uptime: Long = 0,
    val sensor_ready: Boolean = false,
    val mode: String = ""
)

@Serializable
data class CommandPacket(
    val command: String
)

data class BleCommand(
    val wire: String
)

object CommandCodec {

    private val json = Json

    fun encode(command: String): String {
        return json.encodeToString(
            CommandPacket(command)
        )
    }
}