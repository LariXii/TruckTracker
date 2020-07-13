package axxes.prototype.trucktracker

import android.bluetooth.*
import android.content.Context
import android.util.Log
import axxes.prototype.trucktracker.manager.DSRCAttributManager
import axxes.prototype.trucktracker.manager.DSRCManager
import axxes.prototype.trucktracker.model.*
import axxes.prototype.trucktracker.utils.ServiceDsrcUtils

class DeviceBluetoothGatt(_context: Context) {

    interface ListenerBluetoothGatt{
        fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)
        fun serviceFind()
        fun responseNotificationEnabled()
        fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int)
        fun onCharacteristicRead(gatt: BluetoothGatt?,characteristic: BluetoothGattCharacteristic?,status: Int)
        fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?)
    }

    var listenerBluetoothGattCallback: ListenerBluetoothGatt? = null

    private val context: Context = _context

    val dsrcManager: DSRCManager =
        DSRCManager()

    private var bluetoothGatt: BluetoothGatt? = null
    var requestCharacteristic: BluetoothGattCharacteristic? = null
    var responseCharacteristic: BluetoothGattCharacteristic? = null
    var eventCharacteristic: BluetoothGattCharacteristic? = null

    var responseParameters: List<Pair<String, Int>>? = null

    fun connectBLE(device: BluetoothDevice?) {
        Log.d(TAG,"onConnectBLE")
        if (device == null) {
            return
        }
        bluetoothGatt = device.connectGatt(context, true, bluetoothGattCallback)
        listenerBluetoothGattCallback?.onConnectionStateChange(bluetoothGatt!!, BluetoothGatt.STATE_DISCONNECTED, BluetoothGatt.STATE_CONNECTING)
    }

    fun disconnectBLE(){
        Log.d(TAG,"onDisconnectBLE")
        bluetoothGatt?.disconnect()
        listenerBluetoothGattCallback?.onConnectionStateChange(bluetoothGatt!!, BluetoothGatt.STATE_CONNECTED, BluetoothGatt.STATE_DISCONNECTING)
    }

    fun closeBLE(){
        Log.d(TAG,"onCloseBLE")
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun discoverServices() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt!!.discoverServices()
    }

    fun enabledGattNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic){
        val enabled = true
        val descriptor = characteristic.getDescriptor(characteristic.descriptors[0].uuid)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
        gatt.setCharacteristicNotification(characteristic, enabled)
    }

    fun getPacketGetAttribut(attributes: DSRCAttribut): ByteArray{
        return dsrcManager.prepareReadCommandPacket(attributes, false)
    }

    /*private fun getPacketSetGNSSAttribut(): ByteArray{
        val latitude = 6043662
        val longitude = 47258182
        val timeStamp: Long = System.currentTimeMillis()

        val gnssData = dsrcManager.prepareGNSSPacket(timeStamp,longitude,latitude,12,4)

        val attribut = DSRCAttributManager.finAttribut(2,50)

        return dsrcManager.prepareWriteCommandPacket(attribut!!,gnssData,
            autoFillWithZero = true,
            temporaryData = true
        )
    }*/

    fun sendPacketToBDO(characteristic: BluetoothGattCharacteristic, packet: ByteArray): Boolean{
        characteristic.value = packet
        return bluetoothGatt!!.writeCharacteristic(characteristic)
    }

    /*private fun getMenuAttribut(){
        // Store all packets to send
        val queueRequest: MutableList<Pair<DSRCAttribut,ByteArray>> = mutableListOf()
        // Store all packets receive
        var queueResponse: String = ""
        // Store all attributes to get
        val listAttribut: MutableList<DSRCAttribut> = mutableListOf()
        var attribut = DSRCAttributManager.finAttribut(1, 4)
        attribut?.let{listAttribut.add(it)}
        attribut = DSRCAttributManager.finAttribut(1, 19)
        attribut?.let{listAttribut.add(it)}
        attribut = DSRCAttributManager.finAttribut(1, 24)
        attribut?.let{listAttribut.add(it)}

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
                        packetResponse?.let { queueResponse += dsrcManager.readResponse(responseParameters!!,it) + "\n"}
                        // Remove first request packet added
                        queueRequest.removeAt(0)

                        if(queueRequest.isNotEmpty()){
                            sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!,queueRequest[0].second)
                            responseParameters = dsrcManager.readGetAttributPacket(queueRequest[0].first)
                        }
                        else{
                            obtainMessage(END).sendToTarget()
                        }
                    }
                    END -> {
                        // TODO change var ect with queueReponse
                        showResponse(queueResponse)
                        resetHandler()
                        enabledAll()
                    }
                }
            }
        }
        // TODO add all packet to queueRequest
        setResponseHandler(handler)

        for(attr in listAttribut){
            queueRequest.add(Pair(attr,getPacketGetAttribut(attr)))
        }

        sendPacketToBDO(bluetoothGatt!!,requestCharacteristic!!,queueRequest[0].second)
        responseParameters = dsrcManager.readGetAttributPacket(queueRequest[0].first)
    }*/

    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            listenerBluetoothGattCallback?.onConnectionStateChange(gatt, status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
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
                enabledGattNotification(gatt,responseCharacteristic!!)
            }
            listenerBluetoothGattCallback?.serviceFind()
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            listenerBluetoothGattCallback?.responseNotificationEnabled()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            listenerBluetoothGattCallback?.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            listenerBluetoothGattCallback?.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            listenerBluetoothGattCallback?.onCharacteristicChanged(gatt, characteristic)
        }
    }

    companion object{
        private const val TAG = "DeviceBluetoothGatt"
    }

}