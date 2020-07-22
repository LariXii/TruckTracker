package axxes.prototype.trucktracker.json

import com.google.gson.annotations.SerializedName

class AttributeJSON(_name: String, _id: Int, _ct: Int, _data: String) {
    @SerializedName("name")
    val mName: String = _name
    @SerializedName("id")
    val mId: Int = _id
    @SerializedName("ct")
    val mCt: Int = _ct
    @SerializedName("data")
    val mData: String = _data
}