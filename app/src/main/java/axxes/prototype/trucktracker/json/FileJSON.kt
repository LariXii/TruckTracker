package axxes.prototype.trucktracker.json

import com.google.gson.annotations.SerializedName

class FileJSON(_comment: String, _version: Int,_tables: List<TableJSON>){
    @SerializedName("_comment")
    val mComment: String = _comment
    @SerializedName("version")
    val mVersion: Int = _version
    @SerializedName("tables")
    val mTables: List<TableJSON> = _tables
}