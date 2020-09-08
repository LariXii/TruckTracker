package axxes.prototype.trucktracker

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import axxes.prototype.trucktracker.fragment.FragmentConnexionBDO
import axxes.prototype.trucktracker.fragment.FragmentEnd
import axxes.prototype.trucktracker.fragment.FragmentJourney
import axxes.prototype.trucktracker.fragment.FragmentMenuInformations
import axxes.prototype.trucktracker.model.DSRCAttribut
import axxes.prototype.trucktracker.service.MainService
import axxes.prototype.trucktracker.utils.MyJsonUtils
import axxes.prototype.trucktracker.utils.SharedPreferenceUtils
import axxes.prototype.trucktracker.viewmodel.ContextStateViewModelFactory
import axxes.prototype.trucktracker.viewmodel.ViewModelContextState
import com.google.android.gms.location.LocationSettingsStates
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    FragmentConnexionBDO.ListenerFragmentConnexionBDO,
    FragmentMenuInformations.ListenerFragmentMenuInformations,
    FragmentJourney.ListenerFragmentJourney,
    FragmentEnd.ListenerFragmentEnd {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var fragmentManager: FragmentManager
    private lateinit var sharedPreferences: SharedPreferences

    private val fragmentMenuInformations = FragmentMenuInformations()
    private val fragmentConnexionBDO = FragmentConnexionBDO()
    private val fragmentJourney = FragmentJourney()
    private val fragmentEnd = FragmentEnd()
    private lateinit var currentFragment: Fragment

    private lateinit var deviceSelected: BluetoothDevice

    private lateinit var viewModelContextState: ViewModelContextState

    internal var mainService: MainService? = null
    private var mainServiceBound: Boolean = false
    private lateinit var mainServiceBroadcastReceiver: ForegroundOnlyBroadcastReceiver
    private var stateDevice: Int = MainService.STATE_DISCONNECTED
    private var restarting = false
    private var waitResult = false

    private var dialogConnection: AlertDialog? = null
    // Monitors connection to the while-in-use service.
    private val mainServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MainService.LocalBinder
            mainService = binder.service
            mainServiceBound = true
            //Lors du bind au service change les préférences si celui-ci n'est pas en train de tourner (arrive lors de la relance de l'application via Android Studio)
            SharedPreferenceUtils.saveLocationTrackingPref(applicationContext,mainService!!.serviceRunning)

            if(mainService!!.serviceRunning){
                replaceFragment(fragmentJourney, addToBackStack = false)
            }
            else{
                if(restarting)
                    replaceFragment(fragmentConnexionBDO, addToBackStack = false)
            }

            dialogConnection?.dismiss()
            dialogConnection = null
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mainService = null
            mainServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"onCreate()")
        setContentView(R.layout.activity_main)

        // Managers
        initializeManagers()
        viewModelContextState = ViewModelProvider(this, ContextStateViewModelFactory()).get(ViewModelContextState::class.java)

        mainServiceBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        // Permissions
        if(!locationPermissionApproved()){
            viewModelContextState.updatePermissionLocation(false)
            requestForegroundPermissions()
        }
        else{
            viewModelContextState.updatePermissionLocation(true)
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

        dialogConnection = createLoadingDialog("Connexion au service...", false)
        restarting = false
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG,"onStart()")

        // Receivers
        registerReceiver(broadcastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        initializeReveivers()

        //Liaison du service de localisation avec l'activité principale
        if(!mainServiceBound){
            val serviceIntent = Intent(this, MainService::class.java)
            bindService(serviceIntent, mainServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG,"onRestart()")
        restarting = true
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG,"onStop()")
        //Enlève le listener associé aux changements de préférences
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        //Enlève le listener sur le LocalBroadCast
        unregisterReceiver(broadcastBluetooth)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            mainServiceBroadcastReceiver
        )
        if (mainServiceBound && !waitResult) {
            if(!mainService!!.serviceRunning){
                mainService!!.disconnectToBDO()
            }
            unbindService(mainServiceConnection)
            mainServiceBound = false
        }
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy()")
        super.onDestroy()
    }

    private fun initializeManagers(){
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        fragmentManager = supportFragmentManager

        sharedPreferences = getSharedPreferences(SharedPreferenceUtils.PREFERENCE_KEY, Context.MODE_PRIVATE)
    }

    private fun initializeReveivers(){
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_BDO_STATE)
        )
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS)
        )
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_MULTIPLE_ATTRIBUTES_SETTED)
        )
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mainServiceBroadcastReceiver,
            IntentFilter(MainService.ACTION_SERVICE_LOCATION_BROADCAST_UPLOAD_STATE)
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
                        MainService.PERMISSION_LOCATION_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MainService.PERMISSION_LOCATION_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            10 -> {
                when(resultCode){
                    Activity.RESULT_OK -> {
                        val pathFile = data?.data?.path
                        if(pathFile != null){
                            val listAttributes = MyJsonUtils.readJSONFile(this ,pathFile)
                            mainService?.setMultipleAttributes(listAttributes, MainService.ACTION_SERVICE_LOCATION_BROADCAST_MULTIPLE_ATTRIBUTES_SETTED)
                            dialogConnection = createLoadingDialog("Chargement des données...", false)
                            dialogConnection!!.show()
                            waitResult = false
                        }
                    }
                }
            }
            MainService.PERMISSION_BLUETOOTH_REQUEST_CODE -> {
                when(resultCode){
                    Activity.RESULT_OK -> {

                    }
                    Activity.RESULT_CANCELED -> {

                    }
                }
            }
            MainService.CHECK_LOCATION_REQUEST_CODE -> {
                val states: LocationSettingsStates = LocationSettingsStates.fromIntent(data)
                mainService?.setServiceInformationsStates(states.isGpsPresent,states.isGpsUsable)
                when(resultCode){
                    Activity.RESULT_OK -> {

                    }
                    Activity.RESULT_CANCELED -> {
                        Snackbar.make(
                            findViewById(R.id.activity_main),
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(R.string.settings) {
                                // Build intent that displays the App settings screen.
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                            .show()
                    }
                }
            }
            MainService.PERMISSION_LOCATION_REQUEST_CODE -> {
                when(resultCode){
                    Activity.RESULT_OK -> {
                        viewModelContextState.updatePermissionLocation(true)
                    }
                    Activity.RESULT_CANCELED -> {
                        finish()
                    }
                }
            }
        }
    }

    private fun createLoadingDialog(title: String, cancelable: Boolean): AlertDialog{
        val builderDialog = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_progressbar, null)
        builderDialog.setTitle(title)
            .setView(dialogView)
            .setCancelable(false)
        if(cancelable)
            builderDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                onLoadingDialogCancel()
            })
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

    private fun onLoadingDialogCancel(){
        if(stateDevice == MainService.STATE_CONNECTING){
            mainService?.disconnectToBDO()
            dialogConnection?.dismiss()
            dialogConnection = null
            fragmentConnexionBDO.updateConnexion(true)
        }
    }

    private fun gotoFragment(fragment: Fragment){
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.container, fragment)
        transaction.commit()
        currentFragment = fragment
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean){
        val transaction = fragmentManager.beginTransaction()
        fragmentManager.primaryNavigationFragment?.let { transaction.remove(it) }
        transaction.replace(R.id.container, fragment)
        if(addToBackStack)
            transaction.addToBackStack(null)
        transaction.commit()
        currentFragment = fragment
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

                // CHECK_REQUEST
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST")
                    val pendingIntent = intent.getParcelableExtra<PendingIntent>(
                        MainService.EXTRA_CHECK_REQUEST
                    )

                    if(pendingIntent != null) {
                        try {
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                MainService.CHECK_LOCATION_REQUEST_CODE,
                                null,
                                0,
                                0,
                                0
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error
                        }
                    }
                }
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_BDO_STATE -> {
                    val state = intent.getIntExtra(MainService.EXTRA_STATE_BDO, -1)
                    Log.d(TAG,"State du BDO : $state")
                    when(state){
                        MainService.STATE_CONNECTED -> {
                            stateDevice = state
                            replaceFragment(fragmentMenuInformations, false)
                            dialogConnection!!.dismiss()
                            dialogConnection = null
                        }
                        MainService.STATE_CONNECTING -> {
                            //TODO Dialog connecting
                            stateDevice = state
                            dialogConnection = createLoadingDialog("Connexion au BDO...", true)
                            dialogConnection!!.show()
                        }
                        MainService.STATE_DISCONNECTED -> {
                            stateDevice = state
                        }
                        MainService.STATE_FAILURE -> {
                            //TODO Dialog disconnecting
                            dialogConnection!!.dismiss()
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
                        dialogConnection!!.dismiss()
                        dialogConnection = null
                    }
                }

                MainService.ACTION_SERVICE_LOCATION_BROADCAST_MULTIPLE_ATTRIBUTES_SETTED -> {
                    val values = intent.getIntArrayExtra(MainService.EXTRA_RETURN_CODE)
                    if(values != null){
                        dialogConnection!!.dismiss()
                        dialogConnection = null
                        if(values.sum() != 0){
                            dialogConnection = createErrorDialog("Une erreur est arrivé lors du chargement des données via le fichier")
                        }
                        else{
                            fragmentMenuInformations.getMenuInformations()
                        }
                    }
                }
                MainService.ACTION_SERVICE_LOCATION_BROADCAST_UPLOAD_STATE ->{
                    Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_UPLOAD_STATE")
                    when(intent.getIntExtra(MainService.EXTRA_STATE_UPLOAD, -1)){
                        MainService.CONNECTION -> {
                            Log.d(TAG,"CONNECTION")
                            dialogConnection = createLoadingDialog("Upload...", false)
                            dialogConnection!!.show()
                        }
                        MainService.START_UPLOAD -> {

                        }
                        MainService.END_UPLOAD -> {
                            Log.d(TAG,"END_UPLOAD")
                            dialogConnection?.dismiss()
                            dialogConnection = null
                        }
                        MainService.ERROR_UPLOAD -> {
                            Log.d(TAG,"ERROR_UPLOAD")
                            dialogConnection?.dismiss()
                            dialogConnection = null
                        }
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
        dialogConnection = createLoadingDialog("Chargement des données...", false)
        dialogConnection!!.show()
    }

    override fun onClickValide() {
        mainService?.user = fragmentMenuInformations.userInfo
        replaceFragment(fragmentJourney, false)
    }

    override fun onClickSave(listAttribut: List<DSRCAttribut>) {
        mainService?.setMultipleAttributes(listAttribut, MainService.ACTION_SERVICE_LOCATION_BROADCAST_MULTIPLE_ATTRIBUTES_SETTED)
        dialogConnection = createLoadingDialog("Sauvegarde des données...", false)
        dialogConnection!!.show()
    }

    override fun onClickDownload() {
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
        fileIntent.type = "*/*"
        waitResult = true
        startActivityForResult(fileIntent, 10)
    }

    override fun onClickJourney() {
        //Récupèration de l'état de l'application
        val enabled = sharedPreferences.getBoolean(SharedPreferenceUtils.KEY_FOREGROUND_ENABLED, false)
        //SharedPreferenceUtil.saveLocationTrackingPref(applicationContext,false)
        //Si la récupération des localisations était en cours on l'arrête
        if (enabled) {
            //Lors d'un prochain clic on stop l'update de la localisation
            mainService?.unsubscribeToLocationUpdates()
            fragmentJourney.updateChronometer(false, 0)
            replaceFragment(fragmentEnd, false)
        } else {
            // TODO: Step 1.0, Review Permissions: Checks and requests if needed.
            // Si la permission de localisation est approuvé, on lance la récupération des localisations
            if (locationPermissionApproved()) {
                mainService?.subscribeToLocationUpdates()
                    ?: Log.d(TAG, "Service Not Bound")
                fragmentJourney.updateChronometer(true, mainService?.getJourney()!!.startTime)
                fragmentJourney.journeyInformationsToScreen(mainService?.getJourney()!!)
            }
            // Sinon on envoi la demande de permission
            else {
                requestForegroundPermissions()
            }
        }
    }

    override fun onJourneyCreated() {
        if(mainServiceBound && mainService!!.serviceRunning) {
            fragmentJourney.updateChronometer(true, mainService?.getJourney()!!.startTime)
            fragmentJourney.journeyInformationsToScreen(mainService?.getJourney()!!)
        }
    }

    override fun onClickOk(){
        mainService?.disconnectToBDO()
        stopService(Intent(applicationContext, MainService::class.java))
        finish()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

    }
}
