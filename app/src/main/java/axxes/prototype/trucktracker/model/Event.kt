package axxes.prototype.trucktracker.model

import axxes.prototype.trucktracker.utils.MyFileUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Event(_evntCode: EventCode, _paramA: Int? = null, _paramB: Int? = null, _paramC: Int? = null) {
    private val evntId: Int
    private val evntName: String
    private val evntSeverity: Int
    private val evntParamA = _paramA
    private val evntParamB = _paramB
    private val evntParamC = _paramC
    private val evntDate: LocalDateTime = LocalDateTime.now()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    init{
        when(_evntCode){
            EventCode.GPS_NO_COMMUNICATION -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
            }
            EventCode.GPS_INTERFERENCE -> {
                assert(_paramA != null)
                assert(_paramB != null)
                assert(_paramC == null)
            }
            EventCode.GPS_NO_FIX -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
            }
            EventCode.GPS_NO_FIX_PERSISTENT -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
            }
            EventCode.GPS_FROM_MOCK_PROVIDER -> {
                assert(_paramA == null)
                assert(_paramB == null)
                assert(_paramC == null)
            }
        }
        evntId = _evntCode.num
        evntName = _evntCode.mean
        evntSeverity = _evntCode.error.num
    }

    override fun toString(): String {
        val dateParsed = evntDate.format(formatter)
        val sep = MyFileUtils.SEP
        return "$evntId$sep" +
                "\"$evntName\"$sep" +
                "$evntSeverity$sep" +
                "${evntParamA?:""}$sep" +
                "${evntParamB?:""}$sep" +
                "${evntParamC?:""}$sep" +
                "\"$dateParsed\""
    }
}