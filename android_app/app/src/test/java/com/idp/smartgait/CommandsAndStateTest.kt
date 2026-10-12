package com.idp.smartgait
import com.idp.smartgait.data.*; import com.idp.smartgait.protocol.RiskPacket; import com.idp.smartgait.repository.BleCommand
import org.junit.Assert.*; import org.junit.Test
class CommandsAndStateTest {
 @Test fun commandsHaveStableWireValues(){assertEquals("START_SESSION",BleCommand.START_SESSION.wire);assertEquals("CALIBRATE",BleCommand.CALIBRATE.wire)}
 @Test fun staleStateIsRepresentable(){val old=RiskState(RiskPacket(ts=1),receivedAt=0,stale=true);assertTrue(old.stale)}
}
