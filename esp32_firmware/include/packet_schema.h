#pragma once

// JSON is used initially for readable development packets. Binary encoding can be
// introduced later if packet-size measurements justify it.
constexpr const char* PACKET_MODE_SIMULATION = "SIMULATION";
constexpr const char* PACKET_MODE_HARDWARE = "HARDWARE";

