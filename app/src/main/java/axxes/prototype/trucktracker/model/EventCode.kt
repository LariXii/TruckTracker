package axxes.prototype.trucktracker.model

enum class EventCode(val num: Int, val mean: String, val error: EventError) {
    GPS_NO_COMMUNICATION(11000,"GPS no communication", EventError.FATAL_ERROR),
    GPS_INTERFERENCE(11001,"GPS interference", EventError.INFORMATION),
    GPS_NO_FIX(11003,"GPS no fix", EventError.WARNING),
    GPS_NO_FIX_PERSISTENT(11004,"GPS no fix persistent", EventError.ERROR),
    GPS_FROM_MOCK_PROVIDER(11005,"GPS location from mock provider", EventError.ERROR)
}