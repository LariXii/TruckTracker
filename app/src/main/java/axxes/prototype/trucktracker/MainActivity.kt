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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import axxes.prototype.trucktracker.fragment.FragmentConnexionBDO
import axxes.prototype.trucktracker.fragment.FragmentEnd
import axxes.prototype.trucktracker.fragment.FragmentJourney
import axxes.prototype.trucktracker.fragment.FragmentMenuInformations
import axxes.prototype.trucktracker.model.DSRCAttribut
import axxes.prototype.trucktracker.service.MainService
import axxes.prototype.trucktracker.utils.SharedPreferenceUtils
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), FragmentConnexionBDO.ListenerFragmentConnexionBDO, FragmentMenuInformations.ListenerFragmentMenuInformations, FragmentJourney.ListenerFragmentJourney {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var fragmentManager: FragmentManager
    private lateinit var sharedPreferences: SharedPreferences

    private val fragmentMenuInformations = FragmentMenuInformations()
    private val fragmentConnexionBDO = FragmentConnexionBDO()
    private val fragmentJourney = FragmentJourney()
    private val fragmentEnd = FragmentEnd()

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
            SharedPreferenceUtils.saveLocationTrackingPref(applicationContext,mainService!!.serviceRunning)
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

        sharedPreferences = getSharedPreferences(SharedPreferenceUtils.PREFERENCE_KEY, Context.MODE_PRIVATE)

        mainServiceBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        // Permissions
        if(!locationPermissionApproved()){
            requestForegroundPermissions()
        }
        if(!bluetoothAdapter.isEnabled){
            askToEnabledBT()
        }

        gotoFragment(fragmentConnexionBDO)

        if(sharedPreferences.contains(SharedPreferenceUtils.KEY_DEVICE_ADDRESS)
            && sharedPreferences.contains(SharedPreferenceUtils.KEY_DEVICE_NAME)){
            val deviceAC = bluetoothAdapter.getRemoteDevice(sharedPreferences.getString(SharedPreferenceUtils.KEY_DEVICE_ADDRESS, null))
            fragmentConnexionBDO.setDeviceAutoConnexion(deviceAC)
        }
    }

    override fun onStart() {
        super.onStart()

        // Receivers
        registerReceiver(broadcastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_BDO_STATE)
        )
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS)
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
    private fun createErrorDialog(title: String): AlertDialog{
        val builderDialog = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_error, null)
        builderDialog.setTitle(title)
            .setView(dialogView)
            .setCancelable(true)
        return builderDialog.create()
    }

    override fun onDeviceSelected(device: BluetoothDevice) {
        deviceSelected = device
        Log.d(TAG,"Device selected : ${device.name}\n${device.address}")
        if(mainServiceBound){
            SharedPreferenceUtils.saveStringPreference(this,SharedPreferenceUtils.KEY_DEVICE_NAME, deviceSelected.name)
            SharedPreferenceUtils.saveStringPreference(this,SharedPreferenceUtils.KEY_DEVICE_ADDRESS, deviceSelected.address)
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
                        }
                        MainService.STATE_CONNECTING -> {
                            //TODO Dialog connecting
                            dialogConnection = createConnectionDialog("Connexion au BDO...")
                            dialogConnection!!.show()
                        }
                        MainService.STATE_DISCONNECTED -> {
                            //TODO Dialog disconnected
                        }
                        MainService.STATE_DISCONNECTING -> {
                            //TODO Dialog disconnecting
                        }
                        MainService.STATE_FAILURE -> {
                            //TODO Dialog disconnecting
                            dialogConnection!!.hide()
                            dialogConnection = null
                            dialogConnection = createErrorDialog("Error, device not found !")
                        }
                        else -> {
                            //TODO Not handled
                        }
                    }
                }
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS -> {
                    val values = intent.getParcelableArrayExtra(MainService.EXTRA_MENU_INFORMATIONS)
                    if(values != null){
                        dialogConnection!!.hide()
                        dialogConnection = null
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

    override fun onGetAttributesMenu(listAttribut: MutableList<DSRCAttribut>) {
        mainService?.getMultipleAttributes(listAttribut)
        dialogConnection = createConnectionDialog("Chargement des données...")
        dialogConnection!!.show()
    }

    override fun onClickValide() {
        replaceFragment(fragmentJourney, false)
    }

    override fun onClickJourney() {
        //Récupèration de l'état de l'application
        val enabled = sharedPreferences.getBoolean(SharedPreferenceUtils.KEY_FOREGROUND_ENABLED, false)
        //SharedPreferenceUtil.saveLocationTrackingPref(applicationContext,false)
        //Si la récupération des localisations était en cours on l'arrête
        if (enabled) {
            //Lors d'un prochain clic on stop l'update de la localisation
            mainService?.unsubscribeToLocationUpdates()
            replaceFragment(fragmentEnd, false)
        } else {
            // TODO: Step 1.0, Review Permissions: Checks and requests if needed.
            // Si la permission de localisation est approuvé, on lance la récupération des localisations
            if (locationPermissionApproved()) {
                mainService?.subscribeToLocationUpdates()
                    ?: Log.d(TAG, "Service Not Bound")
            }
            // Sinon on envoi la demande de permission
            else {
                requestForegroundPermissions()
            }
        }
    }
}
