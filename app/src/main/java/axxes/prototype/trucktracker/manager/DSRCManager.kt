package axxes.prototype.trucktracker.manager

import axxes.prototype.trucktracker.model.DSRCAttribut
import kotlin.math.pow

class DSRCManager {

    fun prepareGNSSPacket(timestamp: Long, longitude: Int, latitude: Int, hdop: Int, numberOfSattelites: Int): ByteArray{
        var ret = ByteArray(0)

        ret += toBytes(
            timestamp,
            4
        )
        ret += toBytes(
            longitude,
            4
        )
        ret += toBytes(
            latitude,
            4
        )

        ret += toBytes(
            hdop,
            1
        )
        ret += toBytes(
            numberOfSattelites,
            1
        )

        return ret
    }

    fun prepareReadCommandPacket(attribut: DSRCAttribut, temporaryData: Boolean): ByteArray{
        var ret = ByteArray(0)

        // Structure's length of command Get attribut
        val length = 3

        ret += prepareTramePacket(0x02,0x03,0x20,0x00, length)

        // Value property
        ret += if(temporaryData && attribut.cacheable){
            0x02.toByte()
        } else{
            0x00.toByte()
        }

        // EID
        ret += attribut.attrEID.toByte()

        // Attribut id
        ret += attribut.attrId.toByte()

        return ret
    }

    fun prepareWriteCommandPacket(attribut: DSRCAttribut, data: ByteArray, autoFillWithZero: Boolean, temporaryData: Boolean): ByteArray{
        var attrData = data

        if(autoFillWithZero && (attribut.length > data.size)) {
            val diff = attribut.length - data.size
            for(n in 1..diff)
                attrData += 0.toByte()
        }

        attribut.data = attrData

        // Attribut length + structure's length of command Set Attribut + length of container type
        val length = attribut.length + 4 + 1

        var ret = ByteArray(0)

        ret += prepareTramePacket(0x02,0x03,0x21,0x00, length)

        // Value property
        ret += if(temporaryData && attribut.cacheable){
            0x02.toByte()
        } else{
            0x00.toByte()
        }

        // EID
        ret += attribut.attrEID.toByte()

        // Attribut id
        ret += attribut.attrId.toByte()

        // Attribut length
        ret += attribut.length.toByte()

        // Attribut data
        ret += attribut.data!!

        return ret
    }

    fun getReadAttributeParameters(attribut: DSRCAttribut): List<Pair<String,Int>>{
        val parameters = listOf(
            "Target" to 1,
            "Category" to 1,
            "CommandNo" to 1,
            "Reserved" to 1,
            "ParamLength" to 2,
            "ResultCode" to 1,
            "ValueProperty" to 1,
            "EID" to 1,
            "AttributeId" to 1,
            "AttrLength" to 1,
            "Attribute" to attribut.length + 1
        )
        return parameters
    }

    fun readGNSSPacket(): List<Pair<String,Int>>{
        // String for parameter's name and Int for length
        val parameters = listOf(
            "Target" to 1,
            "Category" to 1,
            "CommandNo" to 1,
            "Reserved" to 1,
            "ParamLength" to 2,
            "ResultCode" to 1,
            "ValueProperty" to 1,
            "EID" to 1,
            "AttributeId" to 1
        )

        return parameters
    }

    fun readResponse(parameters: List<Pair<String,Int>>, data: ByteArray): String{
        val ite = data.iterator()
        var currByte: Byte
        var text = "Response : \n"
        var l: Int
        var n = 0
        while(ite.hasNext() && n != parameters.size){
            l = parameters[n].second
            text += parameters[n].first + " "
            currByte = ite.next()
            text += "%02x".format(currByte)

            if(l > 1){
                var stop = 1
                while(ite.hasNext() && (l - stop) != 0){
                    currByte = ite.next()
                    text += " "+"%02x".format(currByte)
                    stop++
                }
            }

            text += "\n"
            n++
        }

        return text
    }

    private fun prepareTramePacket(target: Int, category: Int, commandNo: Int, reserved: Int = 0, paramLength: Int): ByteArray{
        var ret = ByteArray(0)

        ret += target.toByte()
        ret += category.toByte()
        ret += commandNo.toByte()
        ret += reserved.toByte()
        ret += toBytes(
            paramLength,
            2
        )

        return ret
    }

    companion object{

        const val trameLength = 5

        fun toBytes(value: Long, length: Int): List<Byte> {
            val lengthByte = Byte.SIZE_BITS
            val listBytes: MutableList<Byte> = mutableListOf()
            for(n in length-1 downTo  0){
                val shift = lengthByte * n
                if(shift != 0)
                    listBytes.add(value.div(2f.pow(shift)).toByte())
                else
                    listBytes.add(value.toByte())
                value.rem(2f.pow(shift))
            }
            return listBytes
        }
        fun toBytes(value: Int, length: Int): List<Byte> {
            val lengthByte = Byte.SIZE_BITS
            val listBytes: MutableList<Byte> = mutableListOf()
            for(n in length-1 downTo  0){
                val shift = lengthByte * n
                if(shift != 0)
                    listBytes.add(value.div(2f.pow(shift)).toByte())
                else
                    listBytes.add(value.toByte())
                value.rem(2f.pow(shift))
            }
            return listBytes
        }

        fun toHexString(byteArray: ByteArray): String{
            var ret = ""
            for(b in byteArray){
                ret += "%02x".format(b) + " "
            }
            return ret
        }
    }
}