package com.idp.smartgait.viewmodel
import androidx.lifecycle.ViewModel; import androidx.lifecycle.viewModelScope; import com.idp.smartgait.data.*; import com.idp.smartgait.repository.*; import kotlinx.coroutines.flow.*; import kotlinx.coroutines.launch
class MainViewModel(private val repository:RiskRepository=Repository()):ViewModel() {
 private val _state=MutableStateFlow(AppState(simulation=true)); val state:StateFlow<AppState> = _state.asStateFlow()

 // ── Additional UI state (purely additive) ──────────────
 private val _selectedScenario = MutableStateFlow(SimulationScenario.SMOOTH)
 val selectedScenario: StateFlow<SimulationScenario> = _selectedScenario.asStateFlow()

 private val _selectedSession = MutableStateFlow<SessionSummary?>(null)
 val selectedSession: StateFlow<SessionSummary?> = _selectedSession.asStateFlow()

 init {
  viewModelScope.launch { repository.risks.collect{ p -> _state.update{it.copy(connection=ConnectionState.CONNECTED,risk=RiskState(p,System.currentTimeMillis(),false),riskHistory=(it.riskHistory+p).takeLast(60))} } }
  viewModelScope.launch { repository.lives.collect{p->_state.update{it.copy(live=p,samples=(it.samples+p).takeLast(60))} } }
  viewModelScope.launch { repository.statuses.collect{p->_state.update{it.copy(status=p)}} }
  viewModelScope.launch { repository.connection.collect{p->_state.update{it.copy(connection=p)}} }
  viewModelScope.launch{repository.start()}
 }
 fun command(command:BleCommand){
  when(command) {
   BleCommand.START_SESSION -> startMonitoring()
   BleCommand.STOP_SESSION -> stopMonitoring()
   BleCommand.START_CALIBRATION, BleCommand.CALIBRATE -> calibrate()
   else -> viewModelScope.launch{repository.sendCommand(command)}
  }
 }
 fun startMonitoring(){ if (_state.value.monitoring) return; _state.update{it.copy(monitoring=true,sessionStartedAt=System.currentTimeMillis(),message="Monitoring active")}; viewModelScope.launch{repository.sendCommand(BleCommand.START_SESSION)} }
 fun stopMonitoring(){
  val current=_state.value
  if (!current.monitoring) return
  val ended=System.currentTimeMillis(); val values=current.samples
  val risks=current.riskHistory
  val summary=SessionSummary(current.sessionStartedAt ?: ended,ended,values.mapNotNull{it.hr}.averageOrNull(),values.mapNotNull{it.temp}.averageOrNull(),values.mapNotNull{it.cad}.averageOrNull(),risks.maxOfOrNull{it.overall} ?: 0f,risks.maxOfOrNull{it.injury} ?: 0f,risks.maxOfOrNull{it.physio} ?: 0f,risks.lastOrNull()?.overall_level ?: "LOW")
  _state.update{it.copy(monitoring=false,sessionStartedAt=null,history=(it.history+summary).takeLast(20),message="Session complete")}
  viewModelScope.launch{repository.sendCommand(BleCommand.STOP_SESSION)}
 }
 fun calibrate(){
  if (_state.value.calibrating) return
  _state.update{it.copy(calibrating=true,calibrationProgress=0,message="Calibrating...")}
  viewModelScope.launch {
   repeat(5) { kotlinx.coroutines.delay(400); _state.update{it.copy(calibrationProgress=(it.calibrationProgress+20).coerceAtMost(100))} }
   val values=_state.value.samples.takeLast(10)
   _state.update{it.copy(calibrating=false,baseline=Baseline(values.mapNotNull{v->v.hr}.averageOrNull() ?: 0f,values.mapNotNull{v->v.temp}.averageOrNull() ?: 0f,values.mapNotNull{v->v.heel}.averageOrNull(),values.mapNotNull{v->v.forefoot}.averageOrNull()),message="Calibration complete")}
   repository.sendCommand(BleCommand.START_CALIBRATION)
  }
 }
 fun scenario(s:SimulationScenario){
  repository.selectScenario(s)
  _selectedScenario.value = s
  _state.update{it.copy(message="${s.name.lowercase().replaceFirstChar(Char::uppercase)} simulation selected")}
 }
 fun markStale(now:Long=System.currentTimeMillis(), timeout:Long=5000){_state.update{it.copy(risk=it.risk.copy(stale=now-it.risk.receivedAt>timeout))}}

 // ── New functions ──────────────────────────────────────
 fun selectSession(session: SessionSummary?) { _selectedSession.value = session }
 fun clearMessage() { _state.update { it.copy(message = null) } }

 override fun onCleared(){viewModelScope.launch{repository.stop()};super.onCleared()}
}
private fun List<Float>.averageOrNull():Float? = if (isEmpty()) null else average().toFloat()
