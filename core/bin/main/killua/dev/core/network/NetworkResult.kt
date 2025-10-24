package killua.dev.core.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/**
 * @param T 成功时的数据类型。
 * @param E 失败时的错误类型。
 */
sealed class NetworkResult<out T, out E> {
    data class Success<T>(val data: T) : NetworkResult<T, Nothing>()
    data class Error<E>(val error: E) : NetworkResult<Nothing, E>()
}

sealed class NetworkError {
    data class HttpError(val code: Int, val message: String) : NetworkError()
    data class SerializationError(val message: String?) : NetworkError()
    data class GenericError(val cause: Throwable) : NetworkError()
}

suspend inline fun <reified T> HttpResponse.toNetworkResult(): NetworkResult<T, NetworkError> {
    return when {
        status.isSuccess() -> {
            try {
                NetworkResult.Success(body<T>())
            } catch (e: Exception) {
                NetworkResult.Error(NetworkError.SerializationError(e.message))
            }
        }
        else -> {
            NetworkResult.Error(NetworkError.HttpError(status.value, status.description))
        }
    }
}