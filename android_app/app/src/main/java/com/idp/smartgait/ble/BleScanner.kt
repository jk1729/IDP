package com.idp.smartgait.ble
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanFilter
import com.idp.smartgait.ble.BleUuids
class BleScanner(private val adapter:BluetoothAdapter, private val onDevice:(ScanResult)->Unit, private val onError:(Int)->Unit) {
 private val scanner get() = adapter.bluetoothLeScanner
 private val callback=object:ScanCallback(){ override fun onScanResult(type:Int,result:ScanResult)=onDevice(result); override fun onScanFailed(errorCode:Int)=onError(errorCode) }
 fun start(){ try {
  val filter=ScanFilter.Builder().setDeviceName(BleUuids.DEVICE_NAME).build()
  scanner.startScan(listOf(filter), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
 } catch (_:SecurityException) { onError(-1) } }
 fun stop(){ try { scanner.stopScan(callback) } catch (_:SecurityException) { onError(-1) } }
}
