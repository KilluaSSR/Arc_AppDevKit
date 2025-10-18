package killua.dev.core.network.Arch

import killua.dev.core.network.MappableTo
import killua.dev.core.network.NetworkError
import killua.dev.core.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 标记接口，表示这是一个Repository
interface Repository

abstract class BaseRepository {

    /**
     * 一个辅助函数，用于执行网络请求并将其转换为 Flow<Result<DomainModel>>
     * @param apiCall suspend函数，用于实际的网络调用，返回 NetworkResult
     * @param mapper 可选的转换函数，用于将 DTO 转换为 Domain Model
     */
    protected fun <Dto, Domain> safeApiCall(
        apiCall: suspend () -> NetworkResult<Dto, NetworkError>,
        mapper: (Dto) -> Domain
    ): Flow<Result<Domain>> = flow {
        when (val result = apiCall()) {
            is NetworkResult.Success -> {
                try {
                    val domainData = mapper(result.data)
                    emit(Result.success(domainData))
                } catch (e: Exception) {
                    // 转换失败
                    emit(Result.failure(e))
                }
            }
            is NetworkResult.Error -> {
                emit(Result.failure(Exception("Network Error: ${result.error}")))
            }
        }
    }

    /**
     * 如果DTO本身就可以通过MappableTo转换为Domain
     */
    protected fun <Dto : MappableTo<Domain>, Domain> safeApiCall(
        apiCall: suspend () -> NetworkResult<Dto, NetworkError>
    ): Flow<Result<Domain>> = safeApiCall(apiCall) { it.toDomain() }
}