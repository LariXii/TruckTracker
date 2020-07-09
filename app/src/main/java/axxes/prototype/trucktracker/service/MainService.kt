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
import android.os.*
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import axxes.prototype.trucktracker.DeviceBluetoothGatt
import axxes.prototype.trucktracker.model.DSRCAttribut
import axxes.prototype.trucktracker.manager.DSRCManager
import axxes.prototype.trucktracker.utils.ServiceDsrcUtils


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

    private val deviceBluetoothGatt = DeviceBluetoothGatt(this)
    private var requestCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null
    private var eventCharacteristic: BluetoothGattCharacteristic? = null

    private lateinit var dsrcManager: DSRCManager

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

    override fun onCreate() {
        // Manager
        dsrcManager = DSRCManager()
        // Receivers
        registerReceiver(broadcastBluetooth, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        // Bluetooth CallBack
        deviceBluetoothGatt.listenerBluetoothGattCallback = object :
            DeviceBluetoothGatt.ListenerBluetoothGatt {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when(status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d(TAG,"Successfully gatt connected ! \n$newState")
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                //TODO save device on preference
                                stateBluetoothGatt = STATE_CONNECTED
                                sendBroadcastStateBDO()
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

                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.services.forEach{  gattService ->
                        gattService.characteristics.forEach { gattCharacteristic ->
                            if(ServiceDsrcUtils.isKnowing(gattCharacteristic.uuid.toString())){
                                when(gattCharacteristic.uuid.toString()){
                                    ServiceDsrcUtils.COMMAND -> {
                                        requestCharacteristic = gattCharacteristic
                                    }
                                    ServiceDsrcUtils.RESPONSE -> {
                                        responseCharacteristic = gattCharacteristic
                                    }
                                    ServiceDsrcUtils.EVENT -> {
                                        eventCharacteristic = gattCharacteristic
                                    }
                                }
                            }
                        }
                    }
                    deviceBluetoothGatt.enabledGattNotification(gatt,responseCharacteristic!!)
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (characteristic != null) {
                    Log.d(TAG,"Envoi du packet ${DSRCManager.toHexString(characteristic.value)}")
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if(characteristic != null){

                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                if(characteristic != null){
                    Log.d(TAG,"Réception du packet ${DSRCManager.toHexString(characteristic.value)}")
                    handleResponse!!.obtainMessage(RECEIVE,characteristic.value).sendToTarget()
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

    fun getMultipleAttributes(attributes: List<DSRCAttribut>){
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
                        queueRequest[0].first.data = packetResponse
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

    private fun sendBroadcastMenuInformations(listAttr: Array<DSRCAttribut>){
        //Log.d(TAG,"broadCastServiceInformations")
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

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: MainService
            get() = this@MainService
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
    }
}

