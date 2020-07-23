package axxes.prototype.trucktracker.model

import android.hardware.SensorManager
import android.os.Parcel
import android.os.Parcelable

class User(val tyreType: Int, val trailerAxles: Int, val tractorAxles: Int): Parcelable {

    var sensorDelay: Int = SensorManager.SENSOR_DELAY_UI

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(tyreType)
        parcel.writeInt(trailerAxles)
        parcel.writeInt(tractorAxles)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "$tyreType $trailerAxles $tractorAxles"
    }
}