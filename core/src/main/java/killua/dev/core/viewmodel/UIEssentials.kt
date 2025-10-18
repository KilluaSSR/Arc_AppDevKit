package killua.dev.core.viewmodel

interface UIIntent
interface UIState
interface UIEffect

interface IBaseViewModel<I : UIIntent, S : UIState, E : UIEffect> {
    suspend fun onEvent(state: S, intent: I)
    suspend fun onEffect(effect: E)
}