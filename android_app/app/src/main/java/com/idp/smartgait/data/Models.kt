package com.idp.smartgait.data
import com.idp.smartgait.protocol.RiskPacket
import com.idp.smartgait.protocol.LiveDataPacket
enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR }
data class RiskState(val packet:RiskPacket?=null, val receivedAt:Long=0, val stale:Boolean=false)
data class SessionSummary(
    val startedAt: Long,
    val endedAt: Long,
    val averageHr: Float?,
    val averageTemp: Float?,
    val averageCadence: Float?,
    val peakOverall: Float,
    val peakInjury: Float,
    val peakPhysio: Float,
    val finalLevel: String
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0)
}
data class Baseline(val hr: Float, val temp: Float, val heel: Float?, val forefoot: Float?)
data class AppState(
    val connection:ConnectionState=ConnectionState.DISCONNECTED,
    val deviceName:String?=null,
    val risk:RiskState=RiskState(),
    val live:LiveDataPacket?=null,
    val status:com.idp.smartgait.protocol.StatusPacket?=null,
    val simulation:Boolean=false,
    val error:String?=null,
    val monitoring:Boolean=false,
    val sessionStartedAt:Long?=null,
    val history:List<SessionSummary> = emptyList(),
    val samples:List<LiveDataPacket> = emptyList(),
    val riskHistory:List<RiskPacket> = emptyList(),
    val baseline:Baseline?=null,
    val calibrating:Boolean=false,
    val calibrationProgress:Int=0,
    val message:String?=null
)

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val height: Float = 0f,
    val weight: Float = 0f,
    val activityType: String = ""
)
