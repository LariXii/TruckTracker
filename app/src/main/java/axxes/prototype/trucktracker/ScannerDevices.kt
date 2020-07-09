package axxes.prototype.trucktracker

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService

class ScannerDevices(_context: Context, listenerScanner: ListenerScanner) {
    var listener: ListenerScanner? = null
    private val contextContainer = _context

    private val bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
    private val bluetoothLeScanner: BluetoothLeScanner

    private var deviceDetected = false
    var isScanning = false

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("ScannerDevices","Device find")
            if(result.device.name != null){
                if(result.device.name.matches(Regex("TA\\w*")))
                    Log.d("ScannerDevices","Device match with TA")
                listener?.onDeviceFind(result.device)
                deviceDetected = true
            }
        }
    }

    interface ListenerScanner {
        fun onDeviceFind(device: BluetoothDevice)
        fun onStartScan()
        fun onStopScan()
    }

    init{
        bluetoothManager = contextContainer.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        listener = listenerScanner
    }

    fun startScan() {
        bluetoothLeScanner.startScan(scanCallback)
        listener?.onStartScan()
        isScanning = true
    }

    fun stopScan(){
        bluetoothLeScanner.stopScan(scanCallback)
        listener?.onStopScan()
        isScanning = false
    }
}