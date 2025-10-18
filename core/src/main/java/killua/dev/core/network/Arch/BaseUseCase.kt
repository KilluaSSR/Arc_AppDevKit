package killua.dev.core.network.Arch

import kotlinx.coroutines.flow.Flow

/**
 * 一个通用的UseCase接口，P是参数(Parameters)，R是结果(Result)
 */
interface UseCase<in P, out R> {
    operator fun invoke(params: P): R
}

/**
 * 用于返回Flow的UseCase
 */
interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}