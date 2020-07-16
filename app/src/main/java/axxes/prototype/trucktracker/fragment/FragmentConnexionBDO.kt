package axxes.prototype.trucktracker.fragment

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import axxes.prototype.trucktracker.R
import axxes.prototype.trucktracker.viewmodel.ViewModelContextState
import kotlinx.android.synthetic.main.fragment_connection_bdo.*

class FragmentConnexionBDO: Fragment(), FragmentDevicesScan.ListenerFragmentDevicesScan {

    var listener: ListenerFragmentConnexionBDO? = null
    private var deviceAutoConnexion: BluetoothDevice? = null

    private lateinit var btnConnexion: Button
    private lateinit var btnScan: Button
    private lateinit var nameDevice: TextView

    private lateinit var contextState: ViewModelContextState

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connection_bdo, container, false)

        btnConnexion = view.findViewById(R.id.fcb_btn_connexion_auto)
        btnConnexion.isEnabled = false
        btnConnexion.setOnClickListener {
            onDeviceSelected(deviceAutoConnexion!!)
        }

        if(deviceAutoConnexion != null){
            nameDevice = view.findViewById(R.id.fcb_tv_name_bdo)
            nameDevice.text = deviceAutoConnexion!!.name
            btnConnexion.isEnabled = true
        }

        btnScan = view.findViewById(R.id.fcb_btn_scan)
        btnScan.setOnClickListener {
            val dialogFragmentScanDevice = FragmentDevicesScan()
            dialogFragmentScanDevice.setTargetFragment(this,1)
            fragmentManager?.let { it1 -> dialogFragmentScanDevice.show(it1, "Simple dialog fragment") }
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ListenerFragmentConnexionBDO
        if (listener == null) {
            throw ClassCastException("$context must implement ListenerFragmentConnexionBDO")
        }
    }

    fun setDeviceAutoConnexion(deviceAC: BluetoothDevice){
        deviceAutoConnexion = deviceAC
    }

    fun updateConnexion(state: Boolean){
        btnConnexion.isEnabled = state
        btnScan.isEnabled = state
    }

    interface ListenerFragmentConnexionBDO {
        fun onDeviceSelected(device: BluetoothDevice)
    }

    override fun onDeviceSelected(device: BluetoothDevice) {
        fcb_tv_name_bdo.text = device.name
        listener?.onDeviceSelected(device)
    }

}