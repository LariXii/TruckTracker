package axxes.prototype.trucktracker.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyFileUtils {
    companion object{
        private const val TAG = "TruckTracker_FILE"

        const val TYPE_MAPM = "MAPM"
        const val TYPE_EVNT = "EVNT"
        const val CREATOR_ID = "ea0bb5a01de9"
        const val CSV_EXT = "csv"
        const val TMP_EXT = "tmp"
        const val SEP = ";"
        const val EOF = "\r\n"

        fun nameFile(fileContent: ByteArray, fileDate: LocalDateTime, type: String, ext: String): String{

            //Initialisation des formatters
            val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val timeFormatter = DateTimeFormatter.ofPattern("HHmmss")
            //Initialisation de la date et du temps sous le bon format à l'aide de leur formatter respectif
            val parsedDate: String = fileDate.format(dateFormatter)
            val parsedTime: String = fileDate.format(timeFormatter)

            val checksumId = getMD5EncryptedString(fileContent)

            //Création du nom du fichier
            return "${parsedDate}_${parsedTime}_${type}_${CREATOR_ID}_$checksumId.${ext}"
        }

        fun cleanUpFiles(context: Context){
            val files = context.fileList()
            Log.d(TAG,"Suppression de tous les fichiers\n")
            for(f in files){
                File(context.filesDir, f).delete()
            }
        }

        fun getMD5EncryptedString(fileContent: ByteArray): String{
            val mdEnc: MessageDigest = MessageDigest.getInstance("MD5")
            mdEnc.update(fileContent,0,fileContent.size)
            val bigInt = BigInteger(1, mdEnc.digest())
            var md5: String = bigInt.toString(16)
            // Fill to 32 chars
            md5 = String.format("%32s", md5).replace(' ', '0')
            return md5
        }
    }
}