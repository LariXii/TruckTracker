package axxes.prototype.trucktracker

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import axxes.prototype.trucktracker.fragment.FragmentConnexionBDO
import axxes.prototype.trucktracker.fragment.FragmentMenuInformations
import axxes.prototype.trucktracker.manager.DSRCAttributManager
import axxes.prototype.trucktracker.model.DSRCAttribut
import axxes.prototype.trucktracker.service.MainService
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), FragmentConnexionBDO.ListenerFragmentConnexionBDO {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var fragmentManager: FragmentManager

    private val fragmentMenuInformations = FragmentMenuInformations()
    private val fragmentConnexionBDO = FragmentConnexionBDO()

    private lateinit var deviceSelected: BluetoothDevice

    private var mainService: MainService? = null
    private var mainServiceBound: Boolean = false
    private lateinit var mainServiceBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private var dialogConnection: AlertDialog? = null
    // Monitors connection to the while-in-use service.
    private val mainServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MainService.LocalBinder
            mainService = binder.service
            mainServiceBound = true
            //Lors du bind au service change les préférences si celui-ci n'est pas en train de tourner (arrive lors de la relance de l'application via Android Studio)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mainService = null
            mainServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Managers
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        fragmentManager = supportFragmentManager

        mainServiceBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        // Permissions
        if(!locationPermissionApproved()){
            requestForegroundPermissions()
        }
        if(!bluetoothAdapter.isEnabled){
            askToEnabledBT()
        }

        gotoFragment(fragmentConnexionBDO)
    }

    override fun onStart() {
        super.onStart()

        // Receivers
        registerReceiver(broadcastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_BDO_STATE)
        )
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS)
        )
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY)
        )

        //Liaison du service de localisation avec l'activité principale
        val serviceIntent = Intent(this, MainService::class.java)
        bindService(serviceIntent, mainServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastBluetooth)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            mainServiceBroadcastReceiver
        )
    }

    private fun askToEnabledBT(){
        val intentEnabledBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(
            intentEnabledBluetooth,
            MainService.PERMISSION_BLUETOOTH_REQUEST_CODE
        )
    }

    private fun locationPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    private fun requestForegroundPermissions() {
        val provideRationale = locationPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                findViewById(R.id.container),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            MainService.PERMISSION_BLUETOOTH_REQUEST_CODE -> {
                when(resultCode){
                    Activity.RESULT_OK -> {

                    }
                    Activity.RESULT_CANCELED -> {

                    }
                }
            }
        }
    }

    private fun createConnectionDialog(title: String): AlertDialog{
        val builderDialog = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_progressbar, null)
        builderDialog.setTitle(title)
            .setView(dialogView)
            .setCancelable(false)
        return builderDialog.create()
    }

    override fun onDeviceSelected(device: BluetoothDevice) {
        deviceSelected = device
        Log.d(TAG,"Device selected : ${device.name}\n${device.address}")
        if(mainServiceBound){
            mainService?.connectToBDO(deviceSelected)
        }
    }

    private fun gotoFragment(fragment: Fragment){
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.container, fragment)
        transaction.commit()
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean){
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        if(addToBackStack)
            transaction.addToBackStack(null)
        transaction.commit()
    }

    private val broadcastBluetooth: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        askToEnabledBT()
                    }
                    BluetoothAdapter.STATE_ON -> {

                    }
                }
            }
        }
    }

    /**
     * Receiver for location broadcasts from [JourneyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                /*// LOCATION
                JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS")
                    val serviceInfos = intent.getParcelableExtra<ServiceInformations>(
                        JourneyLocationService.EXTRA_INFORMATIONS
                    )

                    if (serviceInfos != null) {
                        serviceInformationsToScreen(serviceInfos)
                    }
                }
                // JOURNEY
                JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS")
                    val journeyInfo = intent.getParcelableExtra<Journey>(
                        JourneyLocationService.EXTRA_JOURNEY
                    )

                    if (journeyInfo != null) {
                        // TODO Display journey informations
                        journeyInformationsToScreen(journeyInfo)
                    }
                }

                /*JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_ACCELEROMETER -> {
                    Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_ACCELEROMETER")
                    val accelerometerInfo = intent.getFloatExtra(JourneyLocationService.EXTRA_ACCELEROMETER, 0f)

                    accelerometerInformationsToScreen(accelerometerInfo)
                }*/

                // CHECK_REQUEST
                JourneyLocationService.ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST")
                    val pendingIntent = intent.getParcelableExtra<PendingIntent>(
                        JourneyLocationService.EXTRA_CHECK_REQUEST
                    )

                    if(pendingIntent != null) {
                        try {
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                REQUEST_CHECK_SETTINGS,
                                null,
                                0,
                                0,
                                0
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error
                        }
                    }
                }*/
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_BDO_STATE -> {
                    val state = intent.getIntExtra(MainService.EXTRA_STATE_BDO, -1)
                    Log.d(TAG,"State du BDO : $state")
                    when(state){
                        MainService.STATE_CONNECTED -> {
                            replaceFragment(fragmentMenuInformations, true)
                            dialogConnection!!.hide()
                            dialogConnection = null

                            val listAttribut: MutableList<DSRCAttribut> = mutableListOf()
                            var attribut = DSRCAttributManager.finAttribut(1, 4)
                            attribut?.let{listAttribut.add(it)}
                            attribut = DSRCAttributManager.finAttribut(1, 19)
                            attribut?.let{listAttribut.add(it)}
                            attribut = DSRCAttributManager.finAttribut(1, 24)
                            attribut?.let{listAttribut.add(it)}

                            mainService?.getMultipleAttributes(listAttribut)
                            /*dialogConnection = createConnectionDialog("Chargement des données...")
                            dialogConnection!!.show()*/
                        }
                        MainService.STATE_CONNECTING -> {
                            //TODO Dialog connecting
                            dialogConnection = createConnectionDialog("Connexion au BDO...")
                            dialogConnection!!.show()
                        }
                        MainService.STATE_DISCONNECTED -> {
                            //TODO Dialog disconnected0
                        }
                        MainService.STATE_DISCONNECTING -> {
                            //TODO Dialog disconnecting
                        }
                        else -> {
                            //TODO Not handled
                        }
                    }
                }
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS -> {
                    val values = intent.getParcelableArrayExtra(MainService.EXTRA_MENU_INFORMATIONS)
                    if(values != null){
                        /*dialogConnection!!.hide()
                        dialogConnection = null*/
                        fragmentMenuInformations.setValuesMenu(values.toList() as List<DSRCAttribut>)
                    }
                }
                else -> {

                }
            }
        }
    }

    companion object{
        private const val TAG = "MainActivity"
    }
}
