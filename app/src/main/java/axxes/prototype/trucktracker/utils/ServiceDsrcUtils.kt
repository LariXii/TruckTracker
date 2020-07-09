package axxes.prototype.trucktracker.utils

import kotlin.math.pow

class ServiceDsrcUtils {
    companion object{
        val COMMAND = "db280001-f473-4446-bf88-cc2c09294427"
        val RESPONSE = "db280002-f473-4446-bf88-cc2c09294427"
        val EVENT = "db280003-f473-4446-bf88-cc2c09294427"

        private val attributes: HashMap<String, String> = HashMap()

        fun lookup(uuid: String, defaultName: String): String{
            val name = attributes[uuid]
            return name ?: defaultName
        }

        fun isKnowing(uuid: String): Boolean{
            return attributes.containsKey(uuid)
        }

        init{
            // Sample Services.
            attributes["0000180D-0000-1000-8000-00805F9B34FB"] = "Advertiser primary service"
            // Sample Characteristics.
            attributes[COMMAND] = "Command characteristic"
            attributes[RESPONSE] = "Response characteristic"
            attributes[EVENT] = "Event characteristic"
        }
    }
}