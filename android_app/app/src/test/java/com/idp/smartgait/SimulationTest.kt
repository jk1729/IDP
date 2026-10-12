package com.idp.smartgait
import com.idp.smartgait.repository.SimulationSource
import kotlinx.coroutines.flow.first; import kotlinx.coroutines.runBlocking; import org.junit.Assert.*; import org.junit.Test
class SimulationTest{@Test fun emitsLabeledPacket()=runBlocking{assertEquals("SIMULATION",SimulationSource().packets().first().status)}}
