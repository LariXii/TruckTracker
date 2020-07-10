package axxes.prototype.trucktracker.model

enum class EventError(val num: Int) {
    FATAL_ERROR(5),
    ERROR (4),
    ERROR_CONTRACTUAL (3),
    WARNING (2),
    INFORMATION (1),
    DEBUG (0)
}