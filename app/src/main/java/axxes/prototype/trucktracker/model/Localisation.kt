package axxes.prototype.trucktracker.model

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import axxes.prototype.trucktracker.utils.MyFileUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Localisation(private val location: Location?): Parcelable{
    //Temps auquel est arrivé la localisation
    val timeFix: Long

    private val c7: String
    //Latitude
    val latitude: Long
    //Longitude
    val longitude: Long
    //Vitesse instantané
    private val c10: Float
    //Direction de la localisation
    private val c11: Float
    //Précision de la localisation
    private val c12: Float
    //Type de localisation
    private val c13: Int = 20
    //Numéro de séquence de la localisation
    //TODO Use system to store and load sequence number
    private val c14: Int = 0
    //Altitude
    private val c15: Double

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    init{
        timeFix = System.currentTimeMillis()

        //Calcul de la date réel de la localisation
        val toSecond = 1000000000
        //Temps en nanosecondes du fix
        val time = location?.elapsedRealtimeNanos?.div(toSecond)
        //Temps en nanosecondes du système au momemt de la réception du fix
        val sTime = SystemClock.elapsedRealtimeNanos()/toSecond
        val ageTime = sTime - time!!

        val date = LocalDateTime.now().minusSeconds(ageTime)
        c7 = date.format(formatter)

        if(location != null){
            latitude = (location.latitude * 1000000).toLong()
            longitude = (location.longitude * 1000000).toLong()
            c10 = location.speed
            c11 = location.bearing
            c12 = location.accuracy/5
            c15 = location.altitude
        }
        else{
            throw ClassFormatError("Une location est requise")
        }
    }

    fun toReadable(): String {
        val speed = location?.speed
        val speedAccuracyMetersPerSecond = location?.speedAccuracyMetersPerSecond

        val bearing = location?.bearing
        val bearingAccuracyDegrees = location?.bearingAccuracyDegrees

        val toSecond = 1000000000
        val time = location?.elapsedRealtimeNanos?.div(toSecond)
        val sTime = SystemClock.elapsedRealtimeNanos()/toSecond

        val date = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val parsedDate = date.format(formatter)

        //val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        //val currentDateandTime: String = sdf.format(Date())

        return if (location != null) {
            "ID : $c14\nLati : ${location.latitude}\nLong : ${location.longitude}\nPrécision : ${location.accuracy/5}\nCap : $bearing ± $bearingAccuracyDegrees\nDate : $parsedDate\nÂge du fix : ${sTime- time!!}s\nVitesse : $speed ± $speedAccuracyMetersPerSecond\n"
        } else{
            "No location"
        }
    }

    override fun toString(): String{
        val sep = MyFileUtils.SEP
        return "\"$c7\"$sep" +
                "$latitude$sep" +
                "$longitude$sep" +
                "$c10$sep" +
                "$c11$sep" +
                "$c12$sep" +
                "$c13$sep" +
                "$c14$sep" +
                "$c15"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(location, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Localisation> {
        override fun createFromParcel(parcel: Parcel): Localisation {
            return Localisation(parcel.readParcelable(Location::class.java.classLoader))
        }

        override fun newArray(size: Int): Array<Localisation?> {
            return arrayOfNulls(size)
        }
    }

}