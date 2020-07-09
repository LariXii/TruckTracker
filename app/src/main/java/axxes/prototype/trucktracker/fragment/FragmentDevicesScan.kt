package axxes.prototype.trucktracker.fragment

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import axxes.prototype.trucktracker.R
import axxes.prototype.trucktracker.ScannerDevices


class FragmentDevicesScan: DialogFragment(), AdapterView.OnItemClickListener, ScannerDevices.ListenerScanner{
    var listener: ListenerFragmentDevicesScan? = null

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var scannerDevices: ScannerDevices? = null

    private var set_pairedDevices: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var adapter_paired_devices: LeDeviceListAdapter

    private lateinit var contextActivity: Context

    private var deviceDetected: Boolean = false
    private var bluetoothActivated: Boolean = false

    private lateinit var lv_paired_devices: ListView
    private lateinit var tvScanning: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnScan: Button
    private lateinit var btnClose: Button

    interface ListenerFragmentDevicesScan{
        fun onDeviceSelected(device: BluetoothDevice)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_devices_scan, container, false)

        lv_paired_devices = v.findViewById(R.id.fsd_lv_paired_devices)
        tvScanning = v.findViewById(R.id.fsd_tv_Scanning)
        progressBar = v.findViewById(R.id.fsd_pbar)
        btnScan = v.findViewById(R.id.fsd_btn_scan)
        btnClose = v.findViewById(R.id.fsd_btn_close)

        btnScan.setOnClickListener {
            if(bluetoothActivated)
                scannerDevices?.startScan()
        }

        btnScan.isEnabled = bluetoothActivated

        btnClose.setOnClickListener {
            if(scannerDevices?.isScanning!!)
                scannerDevices?.stopScan()
            dismiss()
        }

        adapter_paired_devices = LeDeviceListAdapter(contextActivity, R.layout.item_device, set_pairedDevices)
        lv_paired_devices.adapter = adapter_paired_devices
        lv_paired_devices.onItemClickListener = this

        if(bluetoothActivated){
            scannerDevices?.startScan()
            Handler().postDelayed({
                scannerDevices?.stopScan()
            }, 5000)
        }
        return v
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contextActivity = context

        listener = targetFragment as? ListenerFragmentDevicesScan
        if (listener == null) {
            throw ClassCastException("$targetFragment must implement ListenerFragmentDevicesScan")
        }
        Log.d(TAG,"Context de FragmentDevicesScan : $contextActivity")

        bluetoothManager = contextActivity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        contextActivity.registerReceiver(broadCastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        if(bluetoothAdapter.isEnabled){
            scannerDevices = ScannerDevices(contextActivity, this)
            bluetoothActivated = true
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG,"onStop")
        contextActivity.unregisterReceiver(broadCastBluetooth)
    }

    private fun updateScanningInformater(enabled: Boolean){
        btnScan.isEnabled = !enabled

        if(enabled){
            tvScanning.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
        }
        else{
            tvScanning.visibility = View.INVISIBLE
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun updateButtanScanState(enabled: Boolean){
        btnScan.isEnabled = enabled
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        scannerDevices?.stopScan()
        scannerDevices = null
        listener?.onDeviceSelected(set_pairedDevices[position])
        dismiss()
    }

    private class LeDeviceListAdapter(_context: Context,_resources: Int, _objects: MutableList<BluetoothDevice>) : ArrayAdapter<BluetoothDevice>(_context, _resources, _objects) {
        private val mLeDevices: MutableList<BluetoothDevice> = _objects
        private val resources = _resources

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        override fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): BluetoothDevice? {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
            val viewHolder: ViewHolder
            var view = convertView
            Log.d(TAG,"ConvertView null ? : $convertView")
            // General ListView optimization code.
            if (convertView == null) {
                val inflater = context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(resources, viewGroup,false)

                viewHolder = ViewHolder()
                viewHolder.deviceAddress =
                    view.findViewById<View>(R.id.tv_macaddr) as TextView
                viewHolder.deviceName =
                    view.findViewById<View>(R.id.tv_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }

            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.isNotEmpty())
                viewHolder.deviceName?.text = deviceName
            else
                viewHolder.deviceName?.setText(R.string.unknown_device)

            viewHolder.deviceAddress?.text = device.address

            return view!!
        }
    }

    private val broadCastBluetooth: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG,"STATE_OFF")
                        //updateButtanScanState(bluetoothActivated)
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG,"STATE_ON")
                        if(scannerDevices == null)
                            scannerDevices = ScannerDevices(contextActivity, this@FragmentDevicesScan)
                        bluetoothActivated = true
                        updateButtanScanState(bluetoothActivated)
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        Log.d(TAG,"STATE_TURNING_ON")
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Log.d(TAG,"STATE_TURNING_OFF")
                        bluetoothActivated = false
                        if(scannerDevices?.isScanning!!)
                            scannerDevices?.stopScan()
                    }
                }
            }
        }
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    companion object {
        const val TAG = "BLE SCANNING"
        const val PERMISSION_REQUEST_CODE = 101
        const val PERMISSION_BLUETOOTH_REQUEST_CODE = 100
    }

    override fun onDeviceFind(device: BluetoothDevice) {
        deviceDetected = true
        adapter_paired_devices.addDevice(device)
        adapter_paired_devices.notifyDataSetChanged()
        scannerDevices?.stopScan()
    }

    override fun onStartScan() {
        updateScanningInformater(true)
    }

    override fun onStopScan() {
        updateScanningInformater(false)
        if(!deviceDetected && bluetoothActivated)
            scannerDevices?.startScan()
    }

}