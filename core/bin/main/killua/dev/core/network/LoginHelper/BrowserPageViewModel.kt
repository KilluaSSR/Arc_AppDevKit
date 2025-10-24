package killua.dev.core.network.LoginHelper

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import killua.dev.base.Data.account.CookieInfo
import killua.dev.base.Data.account.PlatformConfig
import killua.dev.core.viewmodel.BaseViewModel
import killua.dev.core.viewmodel.UIEffect
import killua.dev.core.viewmodel.UIIntent
import killua.dev.core.viewmodel.UIState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface BrowserUIIntent : UIIntent {
    data class OnCookieChanged(val cookies: String?) : BrowserUIIntent
}
data class BrowserUIState(
    val url: String = "",
    val isLoading: Boolean = true
) : UIState
interface BrowserUIEffect : UIEffect {
    data object NavigateUp : BrowserUIEffect
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    val platformConfig: PlatformConfig,
    private val cookieRepository: CookieRepository
) : BaseViewModel<BrowserUIIntent, BrowserUIState, BrowserUIEffect>(BrowserUIState()) {
    private val _effectFlow = MutableSharedFlow<BrowserUIEffect>()
    val effectFlow: SharedFlow<BrowserUIEffect> = _effectFlow.asSharedFlow()
    init {
       viewModelScope.launch {
           updateState { it.copy(url = platformConfig.loginUrl, isLoading = false) }
       }
    }

    override suspend fun onEvent(state: BrowserUIState, intent: BrowserUIIntent) {
        when (intent) {
            is BrowserUIIntent.OnCookieChanged -> {
                handleCookieCheck(intent.cookies)
            }
        }
    }

    private suspend fun handleCookieCheck(cookies: String?) {
        if (cookies == null) return

        for (group in platformConfig.cookieRuleGroups) {
            val matchResults = mutableListOf<CookieInfo>()

            for (rule in group.rules) {
                val matchResult = rule.pattern.toRegex().find(cookies)
                if (matchResult != null) {
                    val matchValue = matchResult.groupValues[1]
                    matchResults.add(CookieInfo(key = rule.name, value = matchValue))
                }
            }

            val isLoginSuccess = if (group.matchOne) {
                matchResults.isNotEmpty()
            } else {
                matchResults.size == group.rules.size
            }

            if (isLoginSuccess) {
                matchResults.forEach { cookieInfo ->
                    cookieRepository.saveCookie(cookieInfo)
                }

                _effectFlow.emit(BrowserUIEffect.NavigateUp)
                break
            }
        }
    }
}