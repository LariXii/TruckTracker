package axxes.prototype.trucktracker.model

import android.os.Parcel
import android.os.Parcelable

class ContextState() : Parcelable {
    var isGpsEnabled = false
    var isNetworkEnabled = false
    var isBluetoothEnabled = false

    constructor(parcel: Parcel) : this() {
        isGpsEnabled = parcel.readByte() != 0.toByte()
        isNetworkEnabled = parcel.readByte() != 0.toByte()
        isBluetoothEnabled = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isGpsEnabled) 1 else 0)
        parcel.writeByte(if (isNetworkEnabled) 1 else 0)
        parcel.writeByte(if (isBluetoothEnabled) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ContextState> {
        override fun createFromParcel(parcel: Parcel): ContextState {
            return ContextState(parcel)
        }

        override fun newArray(size: Int): Array<ContextState?> {
            return arrayOfNulls(size)
        }
    }
}