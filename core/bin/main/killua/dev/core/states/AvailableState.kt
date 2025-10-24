package killua.dev.core.states

sealed class AvailableState<T> {
    data class Available<T>(val something: T) : AvailableState<T>()
    data class Unavailable(val reason: String? = null) : AvailableState<Nothing>()
}