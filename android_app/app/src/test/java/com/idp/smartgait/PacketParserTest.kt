package com.idp.smartgait
import com.idp.smartgait.protocol.PacketParser
import org.junit.Assert.*; import org.junit.Test
class PacketParserTest { @Test fun parsesRisk(){val p=PacketParser.risk("""{"type":"risk","overall":0.7,"alert":true}""".toByteArray()); assertEquals(.7f,p?.overall);assertTrue(p!!.alert)} @Test fun rejectsWrongType(){assertNull(PacketParser.risk("""{"type":"live"}""".toByteArray()))} }
