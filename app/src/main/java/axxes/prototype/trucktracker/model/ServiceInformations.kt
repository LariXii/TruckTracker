package axxes.prototype.trucktracker.model

import android.os.Parcel
import android.os.Parcelable

class ServiceInformations() : Parcelable {

    var isSending = false
    var isFromMockProvider = false
    var isGpsPresent = false
    var isGpsUsable = false
    var isJourneyRunning = false

    constructor(parcel: Parcel) : this() {
        isSending = parcel.readByte() != 0.toByte()
        isFromMockProvider = parcel.readByte() != 0.toByte()
        isGpsPresent = parcel.readByte() != 0.toByte()
        isGpsUsable = parcel.readByte() != 0.toByte()
        isJourneyRunning = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isSending) 1 else 0)
        parcel.writeByte(if (isFromMockProvider) 1 else 0)
        parcel.writeByte(if (isGpsPresent) 1 else 0)
        parcel.writeByte(if (isGpsUsable) 1 else 0)
        parcel.writeByte(if (isJourneyRunning) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ServiceInformations> {
        override fun createFromParcel(parcel: Parcel): ServiceInformations {
            return ServiceInformations(parcel)
        }

        override fun newArray(size: Int): Array<ServiceInformations?> {
            return arrayOfNulls(size)
        }
    }
}