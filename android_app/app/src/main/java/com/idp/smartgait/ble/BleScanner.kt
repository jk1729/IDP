package com.idp.smartgait.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid

class BleScanner(
    private val adapter: BluetoothAdapter?,
    private val onDevice: (ScanResult) -> Unit,
    private val onError: (Int) -> Unit
) {
    private var isScanning = false
    private val scanner get() = adapter?.bluetoothLeScanner

    private val callback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device
            val scanRecord = result.scanRecord
            val name = scanRecord?.deviceName ?: device?.name

            val matchesName = name != null && name.contains(BleUuids.DEVICE_NAME, ignoreCase = true)
            val matchesUuid = scanRecord?.serviceUuids?.any { it.uuid == BleUuids.SERVICE } == true

            if (matchesName || matchesUuid) {
                onDevice(result)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            onError(errorCode)
        }
    }

    @Synchronized
    fun start() {
        if (isScanning) return

        val leScanner = scanner
        if (adapter == null || !adapter.isEnabled || leScanner == null) {
            onError(-2) // Bluetooth disabled or unavailable
            return
        }

        try {
            isScanning = true
            val filters = listOf(
                ScanFilter.Builder().setServiceUuid(ParcelUuid(BleUuids.SERVICE)).build(),
                ScanFilter.Builder().setDeviceName(BleUuids.DEVICE_NAME).build()
            )
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            leScanner.startScan(filters, settings, callback)
        } catch (_: SecurityException) {
            isScanning = false
            onError(-1) // Permission denied
        } catch (_: Exception) {
            isScanning = false
            onError(-3) // Generic scan error
        }
    }

    @Synchronized
    fun stop() {
        if (!isScanning) return
        isScanning = false
        try {
            scanner?.stopScan(callback)
        } catch (_: SecurityException) {
            // Ignored on teardown
        } catch (_: Exception) {
            // Ignored on teardown
        }
    }
}

