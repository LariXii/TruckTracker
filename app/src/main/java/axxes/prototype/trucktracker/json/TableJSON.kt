package axxes.prototype.trucktracker.json

import com.google.gson.annotations.SerializedName

class TableJSON(_eid: Int, _attributes: List<AttributeJSON>){
    @SerializedName("eid")
    val mEid: Int = _eid
    @SerializedName("attributes")
    val mAttributes: List<AttributeJSON> = _attributes
}