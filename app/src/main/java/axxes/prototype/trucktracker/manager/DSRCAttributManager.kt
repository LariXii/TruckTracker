package axxes.prototype.trucktracker.manager

import axxes.prototype.trucktracker.model.DSRCAttribut
import axxes.prototype.trucktracker.model.DSRCAttributEID1
import axxes.prototype.trucktracker.model.DSRCAttributEID2

class DSRCAttributManager {
    companion object{
        fun finAttribut(eid: Int, attrId: Int): DSRCAttribut?{
            when(eid){
                1 -> {
                    // Search in EID 1
                    val attrArray = DSRCAttributEID1.values()
                    for(attr in attrArray){
                        if(attr.attribut.attrId == attrId)
                            return attr.attribut
                    }
                }
                2 -> {
                    // Search in EID 2
                    val attrArray = DSRCAttributEID2.values()
                    for(attr in attrArray){
                        if(attr.attribut.attrId == attrId)
                            return attr.attribut
                    }
                }
            }
            return null
        }
    }
}