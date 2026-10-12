package com.idp.smartgait.protocol
import kotlinx.serialization.json.Json
object PacketParser {
 private val json = Json { ignoreUnknownKeys=true; isLenient=true }
 fun risk(bytes:ByteArray): RiskPacket? = runCatching { json.decodeFromString<RiskPacket>(bytes.decodeToString()).takeIf { it.type=="risk" } }.getOrNull()
 fun live(bytes:ByteArray): LiveDataPacket? = runCatching { json.decodeFromString<LiveDataPacket>(bytes.decodeToString()).takeIf { it.type=="live" } }.getOrNull()
 fun status(bytes:ByteArray): StatusPacket? = runCatching { json.decodeFromString<StatusPacket>(bytes.decodeToString()).takeIf { it.type=="status" } }.getOrNull()
}
