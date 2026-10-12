package com.idp.smartgait.repository
import com.idp.smartgait.protocol.RiskPacket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
enum class SimulationScenario { SMOOTH, ELEVATED, ALERT }
class SimulationSource(private val intervalMs:Long=1000, var scenario:SimulationScenario=SimulationScenario.SMOOTH) {
 fun packets():Flow<RiskPacket> = flow {
  var i=0
  while(true) {
   val score=when(scenario) {
    SimulationScenario.SMOOTH -> 25f + (i++ % 10) * 2f
    SimulationScenario.ELEVATED -> 62f + (i++ % 5) * 3f
    SimulationScenario.ALERT -> 90f
   }
   val level=when { score >= 70f -> "HIGH"; score >= 40f -> "MODERATE"; else -> "LOW" }
   emit(RiskPacket(
    ts=System.currentTimeMillis(), injury=score, physio=(score+5f).coerceAtMost(100f),
    overall=(score+2f).coerceAtMost(100f), injury_level=level, physio_level=level,
    overall_level=level, status="SIMULATION",
    alert=level=="HIGH", alert_type=if (level=="HIGH") "BOTH" else "NONE",
    priority=if (level=="HIGH") "HIGH" else "LOW",
    message="Simulated data; prototype risk estimation, not a diagnosis."
   ))
   delay(intervalMs)
  }
 }
}
