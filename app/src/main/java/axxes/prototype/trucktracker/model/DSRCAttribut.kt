package axxes.prototype.trucktracker.model

import android.os.Parcel
import android.os.Parcelable

class DSRCAttribut(val attrName: String, val attrId: Int, val attrEID: Int, val containerType: Int, _length: Int, val cacheable: Boolean = false, var data: ByteArray? = null): Parcelable {
    // Length of container type
    val length = _length + 1

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.createByteArray()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(attrName)
        parcel.writeInt(attrId)
        parcel.writeInt(attrEID)
        parcel.writeInt(containerType)
        parcel.writeInt(length)
        parcel.writeByte(if (cacheable) 1 else 0)
        parcel.writeByteArray(data)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DSRCAttribut> {
        override fun createFromParcel(parcel: Parcel): DSRCAttribut {
            return DSRCAttribut(parcel)
        }

        override fun newArray(size: Int): Array<DSRCAttribut?> {
            return arrayOfNulls(size)
        }
    }
}