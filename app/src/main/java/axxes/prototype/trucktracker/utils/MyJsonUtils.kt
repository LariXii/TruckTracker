package axxes.prototype.trucktracker.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import axxes.prototype.trucktracker.json.FileJSON
import axxes.prototype.trucktracker.manager.DSRCAttributManager
import axxes.prototype.trucktracker.model.DSRCAttribut
import com.google.gson.Gson
import java.io.File

class MyJsonUtils {
    companion object{
        @SuppressLint("SdCardPath")
        fun readJSONFile(context: Context, filePath: String): List<DSRCAttribut>{
            val gson = Gson()

            val t = File(context.filesDir,filePath)
            Log.d("MyFileUTILS","File find : $t")
            val jsonContent = "{\"_comment\": \"Premier test de parsing d'un fichier de configuration JSON\",\"version\": 1,\"tables\": [{\"eid\": 1,\"attributes\": [{\"name\": \"ContextMark\",\"id\": 0,\"ct\": 32,\"data\": \"C77FF00001000\"},{\"name\": \"ContractAuthenticator\",\"id\": 4,\"ct\": 36,\"data\": \"0401020304\"},{\"name\": \"ReceiptText\",\"id\": 12,\"ct\": 44,\"data\": \"050102030405\"},{\"name\": \"ReceiptAuthenticator\",\"id\": 13,\"ct\": 45,\"data\": \"03010203\"}]}]}"
            val fileJson: FileJSON = gson.fromJson(jsonContent, FileJSON::class.java)

            val listAttributes: MutableList<DSRCAttribut> = mutableListOf()

            for(eid in fileJson.mTables){
                val eID = eid.mEid
                for(attr in eid.mAttributes){
                    val attribute = DSRCAttributManager.finAttribut(eID, attr.mId)
                    if(attribute != null){
                        attribute.data = stringToByteArray(attr.mData)
                        listAttributes.add(attribute)
                    }
                }
            }
            return listAttributes
        }

        fun stringToByteArray(dataString: String): ByteArray{
            val listInt = mutableListOf<Int>()
            dataString.toLowerCase()
            //Iterate through string
            for(i in dataString.indices){
                var value = 0
                //If char at indice i is a letter
                if(dataString[i].isLetter()){
                    //Take is value minus 87 (see ascii table)
                    value = dataString[i].toInt() - 87
                }
                else{
                    //If char at indice i is a digit
                    if(dataString[i].isDigit()){
                        //Cast char to string then to int
                        value = dataString[i].toString().toInt()
                    }
                }
                //2 char are needed to create a byte, so first char is multiplied by 16 and second char is just added to the first
                if(i%2 == 0) {
                    value *= 16
                    listInt.add(value)
                }
                else{
                    listInt[listInt.lastIndex] = listInt.last() + value
                }
            }
            //Cast first array to an array of Byte
            val byteArray = mutableListOf<Byte>()
            listInt.forEach { byteArray.add(it.toByte()) }

            return byteArray.toByteArray()
        }
    }
}
