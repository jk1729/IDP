# Smart Gait Android (Phase 9)

Standalone Kotlin/Jetpack Compose BLE client. UUIDs exactly match `esp32_firmware/include/ble_config.h`. It parses JSON risk/live/status notifications and displays incoming risk values only; risk computation and hardware drivers remain on the firmware. `SimulationSource` supports smooth, elevated, and alert scenarios; every simulated screen value is explicitly labelled.

Build with `gradle :app:assembleDebug` (Android SDK/JDK 17 required; generate a wrapper with `gradle wrapper` if desired). BLE scanning/connection classes are intentionally small and can be wired to a platform callback. Runtime permissions for Android 12+ are declared in the manifest; the UI permission prompt is a host-app concern.
