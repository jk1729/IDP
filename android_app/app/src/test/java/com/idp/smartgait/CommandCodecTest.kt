package com.idp.smartgait
import com.idp.smartgait.protocol.CommandCodec
import org.junit.Assert.assertEquals
import org.junit.Test
class CommandCodecTest { @Test fun serializesFirmwareCommand(){ assertEquals("""{"command":"PING"}""", CommandCodec.encode("PING")) } }
