package com.idp.smartgait.repository
import com.idp.smartgait.protocol.*; import com.idp.smartgait.data.*; import kotlinx.coroutines.flow.*; import kotlinx.coroutines.launch
interface RiskRepository { val risks:Flow<RiskPacket>; val lives:Flow<LiveDataPacket>; val statuses:Flow<StatusPacket>; val connection:Flow<ConnectionState>; suspend fun start(); suspend fun stop(); suspend fun sendCommand(command:BleCommand):Boolean; fun selectScenario(s:SimulationScenario) }
enum class BleCommand(val wire:String){
 PING("PING"),
 GET_STATUS("GET_STATUS"),
 START_SESSION("START_SESSION"),
 STOP_SESSION("STOP_SESSION"),
 START_CALIBRATION("START_CALIBRATION"),
 STOP_CALIBRATION("STOP_CALIBRATION"),
 VIBRATE_TEST("VIBRATE_TEST"),
 CALIBRATE("CALIBRATE")
}
class Repository(private val simulation:SimulationSource=SimulationSource()):RiskRepository {
 private val _risks=MutableSharedFlow<RiskPacket>(extraBufferCapacity=1); override val risks=_risks.asSharedFlow(); private var job:kotlinx.coroutines.Job?=null
 private val _lives=MutableSharedFlow<LiveDataPacket>(extraBufferCapacity=1); override val lives=_lives.asSharedFlow()
 private val _statuses=MutableSharedFlow<StatusPacket>(extraBufferCapacity=1); override val statuses=_statuses.asSharedFlow()
 private val _connection=MutableStateFlow(ConnectionState.DISCONNECTED); override val connection=_connection.asStateFlow()
 override suspend fun start(){ if (job?.isActive == true) return; _connection.value=ConnectionState.CONNECTED; job= kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch { simulation.packets().collect{
  _risks.emit(it)
  val imbalance=(it.injury/100f).coerceIn(0f,1f)
  _lives.emit(LiveDataPacket(ts=it.ts,hr=72f+it.physio*.4f,temp=36.6f+it.physio*.01f,heel=420f*(1f-imbalance*.25f),forefoot=310f*(1f+imbalance*.25f),acc=1.21f,cad=168f,mode="SIMULATION"))
  _statuses.emit(StatusPacket(device="SMART_GAIT_001",firmware="0.8.0",battery=97,connected=true,uptime=it.ts,sensor_ready=false,mode="SIMULATION"))
 } }}
 override suspend fun stop(){job?.cancel(); job=null}
 override suspend fun sendCommand(command:BleCommand)=true
 override fun selectScenario(s:SimulationScenario){simulation.scenario=s}
}
