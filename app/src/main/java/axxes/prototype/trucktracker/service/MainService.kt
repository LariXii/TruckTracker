/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package axxes.prototype.trucktracker.service

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.content.*
import android.content.res.Configuration
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import axxes.prototype.trucktracker.DeviceBluetoothGatt
import axxes.prototype.trucktracker.MainActivity
import axxes.prototype.trucktracker.manager.DSRCManager
import axxes.prototype.trucktracker.utils.SharedPreferenceUtils
import axxes.prototype.trucktracker.manager.EventManager
import axxes.prototype.trucktracker.manager.MAPMManager
import axxes.prototype.trucktracker.model.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.io.File
import java.io.FileInputStream
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply

/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */
class MainService : Service(){
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private val localBinder = LocalBinder()
    private var configurationChange = false

    private var serviceRunningInForeground = false
    private var device: BluetoothDevice? = null

    var serviceRunning = false
    var stateBluetoothGatt = STATE_DISCONNECTED

    // ################### BLUETOOTH VARIABLES #################### \\
    private val deviceBluetoothGatt = DeviceBluetoothGatt(this)
    private var requestCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null
    private var eventCharacteristic: BluetoothGattCharacteristic? = null
    // ########################################################## \\

    // ################### LOCATIONS VARIABLES #################### \\
    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest
    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback
    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private lateinit var currentLocation: Localisation

    private val countDownTimerNoFix = CountDownTimerNoFix(TIME_TO_WAIT_FOR_NO_FIX,1000)
    private var noFixHappened = 0
    private lateinit var journey: Journey
    private lateinit var user: User
    // ########################################################## \\

    // ########################### FILES ########################### //
    private lateinit var fileWriting: String

    private val filesUploading: MutableList<String> = mutableListOf()
    // ############################################################# //

    // ################### MANAGERS #################### \\
    private lateinit var dsrcManager: DSRCManager
    private lateinit var eventManager: EventManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var locationManager: LocationManager
    private lateinit var mapmManager: MAPMManager
    private lateinit var serviceInformations: ServiceInformations
    private var notificationBuilder: NotificationCompat.Builder? = null
    // ########################################################## \\

    // ################### BLUETOOTH HANDLER #################### \\
    val SEND = 0
    val RECEIVE = 1
    val END = 2

    @SuppressLint("HandlerLeak")
    private val mainResponseHandler = object: Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var packetResponse: ByteArray? = null
            if(msg.obj != null) {
                packetResponse = msg.obj as ByteArray
            }
            when(msg.what){
                RECEIVE -> {

                }
            }
        }
    }

    private var handleResponse: Handler? = mainResponseHandler

    private fun setResponseHandler(newHandler: Handler){
        handleResponse = newHandler
    }

    private fun resetHandler(){
        handleResponse = mainResponseHandler
    }
    // ########################################################## \\

    // ################### LOCATION HANDLER #################### \\
    var timerHandler: Handler = Handler()
    private var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.d(TAG,"Fichier à uploader : $filesUploading")

            var files = applicationContext.fileList()
            Log.d(TAG,"Liste des fichiers dans la mémoire interne avant envoi : \n")
            for(f in files){
                Log.d(TAG,"\t-$f size : ${File(applicationContext.filesDir, f).length()}octets\n")
            }

            //Création du nouveau fichier dans lequel écrire et changement du flux sur celui-ci
            val fileToSend = mapmManager.closeFile()
            val name = mapmManager.openFile()
            fileWriting = name
            filesUploading.add(fileWriting)

            //Envoi du fichier au serveur
            uploadFile(fileToSend)

            timerHandler.postDelayed(this, (TIME_TO_WAIT_BEFORE_SEND_MAPM).toLong())
        }
    }
    // ########################################################## \\

    override fun onCreate() {
        // Manager
        initializeManagers()
        // Receivers
        initializeReceiver()
        // Bluetooth CallBack
        initializeBluetoothGattCallBack()
        // Location update parameters
        initializeLocationParameters()
    }

    private fun initializeManagers(){
        dsrcManager = DSRCManager()
        eventManager = EventManager(applicationContext, 399367311, 3, 10747906)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        serviceInformations = ServiceInformations()
        mapmManager = MAPMManager(this,399367311, 3, 10747906,2,2,4)
    }

    private fun initializeReceiver(){
        registerReceiver(broadcastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_LOW))
        registerReceiver(contextServiceBroadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_OKAY))
    }

    private fun initializeBluetoothGattCallBack(){
        deviceBluetoothGatt.listenerBluetoothGattCallback = object :
            DeviceBluetoothGatt.ListenerBluetoothGatt {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when(status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                //TODO save device on preference
                                deviceBluetoothGatt.discoverServices()
                            }
                            BluetoothProfile.STATE_CONNECTING -> {
                                stateBluetoothGatt = STATE_CONNECTING
                                sendBroadcastStateBDO()
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                stateBluetoothGatt = STATE_DISCONNECTED
                                sendBroadcastStateBDO()
                            }
                            BluetoothProfile.STATE_DISCONNECTING -> {
                                stateBluetoothGatt = STATE_DISCONNECTING
                                sendBroadcastStateBDO()
                            }
                        }
                    }
                    BluetoothGatt.GATT_FAILURE -> {
                        stateBluetoothGatt = STATE_FAILURE
                        sendBroadcastStateBDO()
                    }
                }
            }

            override fun serviceFind(){
                requestCharacteristic = deviceBluetoothGatt.requestCharacteristic
                responseCharacteristic = deviceBluetoothGatt.responseCharacteristic
                eventCharacteristic = deviceBluetoothGatt.eventCharacteristic
            }

            override fun responseNotificationEnabled() {
                stateBluetoothGatt = STATE_CONNECTED
                sendBroadcastStateBDO()
                saveDeviceToPreference()
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                Log.d(TAG,"onCharacteristicWrite")
                if (characteristic != null) {
                    Log.d(TAG,"Envoi du packet ${DSRCManager.toHexString(characteristic.value)}")
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                Log.d(TAG,"onCharacteristicRead")
                if(characteristic != null){

                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                Log.d(TAG,"onCharacteristicChanged")
                if(characteristic != null){
                    Log.d(TAG,"Réception du packet ${DSRCManager.toHexString(characteristic.value)}")
                    handleResponse!!.obtainMessage(RECEIVE,characteristic.value).sendToTarget()
                }
            }
        }
    }

    private fun initializeLocationParameters(){
        // TODO: Step 1.2, Review the FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // TODO: Step 1.3, Create a LocationRequest.
        locationRequest = LocationRequest().apply {
            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.
            //
            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            interval = 5 * 1000

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            fastestInterval = 3 * 1000

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = 10 * 1000

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // TODO: Step 1.4, Initialize the LocationCallback.
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                Log.d(TAG,"Localisations disponible : ${locationResult?.locations}")
                Log.d(TAG,"Nombre : ${locationResult?.locations?.size}")

                if (locationResult?.locations != null) {

                    countDownTimerNoFix.cancel()
                    countDownTimerNoFix.start()
                    noFixHappened = 0

                    for(loc in locationResult.locations){
                        currentLocation = Localisation(loc)
                        journey.addLocation(currentLocation)

                        // Write event if location isFromMockProvider = true
                        if(loc.isFromMockProvider){
                            eventManager.writeEvent(Event(EventCode.GPS_FROM_MOCK_PROVIDER))
                        }
                        // Write new location
                        mapmManager.writeLocation(currentLocation)

                        //Send informations to the main activity about service and journey
                        broadCastServiceInformations()
                        broadCastJourneyInformations()
                    }

                } else {
                    Log.d(TAG, "Location information isn't available.")
                }
            }
            override fun onLocationAvailability(availability: LocationAvailability?) {
                super.onLocationAvailability(availability)
                if (availability != null) {
                    if(!availability.isLocationAvailable) {
                        Log.d(TAG,"Location Availability changed ! ")
                        val builderSettingsLocation: LocationSettingsRequest.Builder  = LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest)

                        val task = LocationServices.getSettingsClient(applicationContext).checkLocationSettings(builderSettingsLocation.build())

                        task.addOnSuccessListener { response ->
                            val states = response.locationSettingsStates
                            serviceInformations.isGpsUsable = states.isGpsUsable
                            serviceInformations.isGpsPresent = states.isGpsPresent
                            broadCastServiceInformations()
                        }
                        task.addOnFailureListener { e ->
                            //Log.d(TAG,"task.onFailure")
                            if (e is ResolvableApiException) {
                                try {
                                    //If service is running in foreground
                                    if(serviceRunningInForeground){
                                        //Start activity to resolve problem
                                        val intent = Intent(applicationContext, MainActivity::class.java)
                                        startActivity(intent)
                                    }
                                    else{
                                        // Send Intent to resolve problem
                                        val intent = Intent(
                                            ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST
                                        )
                                        intent.putExtra(EXTRA_CHECK_REQUEST, e.resolution)
                                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                                    }
                                } catch (sendEx: IntentSender.SendIntentException) { }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind()")

        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")
        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        disconnectToBDO()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        journey = Journey()
        journey.startJourney()

        SharedPreferenceUtils.saveLocationTrackingPref(
            this,
            true
        )

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, MainService::class.java))
        serviceRunning = true

        try {
            // TODO: Step 1.5, Subscribe to location changes.
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

            //Start the timer to send all 5 min a MAPM file
            timerHandler.postDelayed(timerRunnable, (TIME_TO_WAIT_BEFORE_SEND_MAPM).toLong())
            //Start the timer for event no fix and no fix persistent
            countDownTimerNoFix.start()

            //Création du fichier MAPM
            val name = mapmManager.openFile()
            fileWriting = name
            filesUploading.add(fileWriting)
            //Création du fichier de log EVNT
            eventManager.openFile()


        } catch (unlikely: SecurityException) {
            SharedPreferenceUtils.saveLocationTrackingPref(
                this,
                false
            )
            countDownTimerNoFix.cancel()
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            // TODO: Step 1.6, Unsubscribe to location changes.
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                    countDownTimerNoFix.cancel()
                    // Stop journey
                    journey.stopJourney()
                    //stopSelf()
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }

            // Arrêt du chronomètre d'envoi des fichiers
            timerHandler.removeCallbacks(timerRunnable)

            // Fermeture des streams des fichiers MAPM et EVNT
            val nameFileMAPM = mapmManager.closeFile()
            val nameFileEvnt = eventManager.closeFile()
            //TODO upload nameFileEvnt uploadFile(nameFileEvnt)+
            // Envoi du fichier en cours d'écriture à la fin du service
            uploadFile(nameFileMAPM)

            // Sauvegarde de l'état du service (ici arrêté) dans les préférences
            SharedPreferenceUtils.saveLocationTrackingPref(this, false)
            serviceRunning = false

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtils.saveLocationTrackingPref(
                this,
                true
            )
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    /**
     * DEVICE BLUETOOTH
     */

    fun connectToBDO(_device: BluetoothDevice){
        if(device == null)
            device = _device
        deviceBluetoothGatt.connectBLE(device)
    }

    fun disconnectToBDO(){
        deviceBluetoothGatt.disconnectBLE()
        deviceBluetoothGatt.closeBLE()
    }

    private fun saveDeviceToPreference(){
        SharedPreferenceUtils.saveStringPreference(applicationContext, SharedPreferenceUtils.KEY_DEVICE_NAME,
            device?.name
        )
        SharedPreferenceUtils.saveStringPreference(applicationContext, SharedPreferenceUtils.KEY_DEVICE_ADDRESS,
            device?.address
        )
    }

    fun getMultipleAttributes(attributes: List<DSRCAttribut>){
        Log.d(TAG, "getMultipleAttributes")
        // Store all responses packets
        val responses: MutableList<DSRCAttribut> = mutableListOf()
        // Store all packets to send
        val queueRequest: MutableList<Pair<DSRCAttribut,ByteArray>> = mutableListOf()
        for(attr in attributes){
            queueRequest.add(Pair(attr,dsrcManager.prepareReadCommandPacket(attr, false)))
        }

        val handler = @SuppressLint("HandlerLeak")
        object: Handler(){
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                var packetResponse: ByteArray? = null
                if(msg.obj != null) {
                    packetResponse = msg.obj as ByteArray
                }

                when(msg.what){
                    SEND -> {

                    }
                    RECEIVE -> {
                        // Add response
                        queueRequest[0].first.data = packetResponse?.copyOfRange(12,12 + queueRequest[0].first.length)
                        responses.add(queueRequest[0].first)
                        // Remove first request packet added
                        queueRequest.removeAt(0)

                        if(queueRequest.isNotEmpty()){
                            deviceBluetoothGatt.sendPacketToBDO(requestCharacteristic!!,queueRequest[0].second)
                        }
                        else{
                            obtainMessage(END).sendToTarget()
                        }
                    }
                    END -> {
                        sendBroadcastMenuInformations(responses.toTypedArray())
                        resetHandler()
                    }
                }
            }
        }
        // TODO add all packet to queueRequest
        setResponseHandler(handler)
        deviceBluetoothGatt.sendPacketToBDO(requestCharacteristic!!,queueRequest[0].second)
    }

    private fun uploadFile(fileName: String){
        if(serviceRunning){
            Log.d(TAG,"Lancement de l'upload")
            Thread(
                Runnable{
                    val ftpClient = FTPClient()

                    ftpClient.connect(SERVER_NAME)
                    Log.d(TAG,"Connection au serveur FTP")

                    val reply = ftpClient.replyCode

                    if(!FTPReply.isPositiveCompletion(reply)){
                        ftpClient.disconnect()
                        Log.e(TAG,"FTP server refused connection")
                        // TODO handle this exception
                        //exitProcess(1)
                    }

                    ftpClient.login(LOGIN, PASSWORD)
                    Log.d(TAG,"Login au serveur FTP")

                    //Transfer File
                    val ret = ftpClient.storeFile("mapm_files/$fileName", FileInputStream(File(applicationContext.filesDir,fileName)))
                    if(ret){
                        Log.d(TAG,"StoreFile")
                    }
                    else{
                        //TODO Handle this event
                    }
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            ).start()
        }
    }

    private fun sendBroadcastMenuInformations(listAttr: Array<DSRCAttribut>){
        val intent = Intent(ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS)
        intent.putExtra(EXTRA_MENU_INFORMATIONS, listAttr)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun sendBroadcastStateBDO(){
        Log.d(TAG,"broadCastServiceStateBDO : $stateBluetoothGatt")
        val intent = Intent(ACTION_SERVICE_LOCATION_BROADCAST_BDO_STATE)
        intent.putExtra(EXTRA_STATE_BDO, stateBluetoothGatt)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun broadCastServiceInformations(){
        //Log.d(TAG,"broadCastServiceInformations")
        val intent = Intent(ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS)
        intent.putExtra(EXTRA_INFORMATIONS, serviceInformations)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun broadCastJourneyInformations(){
        //Log.d(TAG,"broadCastServiceInformations")
        val intent = Intent(ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY)
        intent.putExtra(EXTRA_JOURNEY, journey)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    // ################################################################################ \\
    // ############################## BROADCAST RECEIVER ############################## \\
    // ################################################################################ \\

    private val broadcastBluetooth: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                if(device != null){
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> {
                            if(stateBluetoothGatt == STATE_CONNECTED)
                                disconnectToBDO()
                        }

                        BluetoothAdapter.STATE_ON -> {
                            if(stateBluetoothGatt == STATE_DISCONNECTED)
                                connectToBDO(device!!)
                        }

                    }
                }
            }
        }
    }

    private val contextServiceBroadcastReceiver : BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                //Mode changed
                LocationManager.MODE_CHANGED_ACTION -> {
                    //Log.d(TAG,"ACTION_SERVICE_LOCATION_BROADCAST_MODE_CHANGED")
                    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    serviceInformations.isGpsUsable = isGpsEnabled
                    broadCastServiceInformations()

                    if(isGpsEnabled || isNetworkEnabled){
                        //Log.d(TAG,"La localisation est activé !")
                    }
                    else{
                        //Log.d(TAG,"La localisation est désactivé !")
                        if(serviceRunning)
                            eventManager.writeEvent(Event(EventCode.GPS_NO_COMMUNICATION))
                    }
                }
                //Mode changed
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    //Log.d(TAG,"PROVIDERS_CHANGED_ACTION")
                    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if(isGpsEnabled || isNetworkEnabled){
                        //Log.d(TAG,"La localisation est activé !")
                    }
                    else{
                        //Log.d(TAG,"La localisation est désactivé !")
                        if(serviceRunning)
                            eventManager.writeEvent(Event(EventCode.GPS_NO_COMMUNICATION))
                    }
                }
                Intent.ACTION_BATTERY_LOW -> {
                    //Log.d(TAG,"Le niveau de la batterie est faible : ${getBatteryPercentage(applicationContext)}")
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    //Log.d(TAG,"Le niveau de la batterie est ok : ${getBatteryPercentage(applicationContext)}")
                }
                //Intent.ACTION_BATTERY_CHANGED -> {
                //    Log.d(TAG,"Le niveau de batterie à changé : ${getBatteryPercentage(applicationContext)}")
                //}
                else -> {

                }
            }
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: MainService
            get() = this@MainService
    }

    /**
     * Class used for count down how many time there are no fix
     */
    inner class CountDownTimerNoFix(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval){
        override fun onFinish() {
            if(noFixHappened >= 5){
                Log.d(TAG,"onFinish CountDownTimerNoFix > 5")
                eventManager.writeEvent(Event(EventCode.GPS_NO_FIX_PERSISTENT))
            }
            else{
                Log.d(TAG,"onFinish CountDownTimerNoFix")
                eventManager.writeEvent(Event(EventCode.GPS_NO_FIX))
            }
            noFixHappened++
            countDownTimerNoFix.start()
        }

        override fun onTick(millisUntilFinished: Long) {

        }
    }

    companion object {
        // #################### VARIABLES #################### //
        private const val TAG = "MainService"
        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        private const val SERVER_NAME = "ftp.cluster029.hosting.ovh.net"
        private const val LOGIN = "thecjub"
        private const val PASSWORD = "s23Kpn9jgUS3"

        private const val NOTIFICATION_ID = 12345678
        private const val NOTIFICATION_FILE_ID = 87654321
        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
        private const val NOTIFICATION_CHANNEL_FILE_ID = "while_in_use_channel_02"

        private const val TIME_TO_WAIT_BEFORE_SEND_MAPM = 1 * 60 * 1000
        private const val TIME_TO_WAIT_FOR_NO_FIX: Long = 2 * 60 * 1000

        // ################################################ //

        // #################### ACTIONS #################### //
        internal const val ACTION_SERVICE_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_INFORMATIONS =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST_LOCATION"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_JOURNEY =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST_JOURNEY"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_CHECK_REQUEST =
            "$PACKAGE_NAME.action.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST_CHECK_REQUEST"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_BDO_STATE =
            "$PACKAGE_NAME.action.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST_BDO_STATE"

        internal const val ACTION_SERVICE_LOCATION_BROADCAST_MENU_INFORMATIONS =
            "$PACKAGE_NAME.action.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST_MENU_INFORMATIONS"

        // ################################################ //

        // #################### EXTRAS #################### //
        internal const val EXTRA_INFORMATIONS = "$PACKAGE_NAME.extra.LOCATION"

        internal const val EXTRA_JOURNEY = "$PACKAGE_NAME.extra.JOURNEY"

        internal const val EXTRA_DEVICE = "$PACKAGE_NAME.extra.DEVICE"

        internal const val EXTRA_STATE_BDO  = "$PACKAGE_NAME.extra.STATE_BDO"

        internal const val EXTRA_MENU_INFORMATIONS  = "$PACKAGE_NAME.extra.INFORMATIONS"

        internal const val EXTRA_CHECK_REQUEST = "$PACKAGE_NAME.extra.CHECK_REQUEST"
        // ################################################ //

        // #################### PERMISSIONS CODES #################### //
        internal const val PERMISSION_BLUETOOTH_REQUEST_CODE = 100

        // ################################################ //

        internal const val STATE_SEND = 0
        internal const val STATE_SENT = 1
        internal const val STATE_ERROR = 2
        internal const val STATE_CLOSE = 3

        internal const val STATE_CONNECTED = 10
        internal const val STATE_CONNECTING = 11
        internal const val STATE_DISCONNECTED = 12
        internal const val STATE_DISCONNECTING = 13
        internal const val STATE_FAILURE = -10
    }
}

