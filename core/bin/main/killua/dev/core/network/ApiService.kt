package killua.dev.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiService @Inject constructor(private val httpClient: HttpClient) {

    suspend fun execute(
        endpoint: String,
        builder: HttpRequestBuilder.() -> Unit = {}
    ): Result<HttpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(httpClient.request(endpoint, builder))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}