package killua.dev.core.network.LoginHelper
import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import killua.dev.base.Data.account.PlatformConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun BrowserPage(
    @StringRes title: Int,
    onLoginSuccess: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val viewModel: BrowserViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effectFlow.collect{ effect ->
            when (effect) {
                is BrowserUIEffect.NavigateUp -> {
                    onLoginSuccess()
                }
            }
        }
    }

    BrowserContent(
        title = title,
        uiState = uiState,
        onCookieChanged = { cookies ->
            viewModel.emitIntentOnIO(BrowserUIIntent.OnCookieChanged(cookies))
        },
        onNavigateUp = onNavigateUp,
        platformConfig = viewModel.platformConfig
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserContent(
    @StringRes title: Int,
    uiState: BrowserUIState,
    platformConfig: PlatformConfig,
    onCookieChanged: (String?) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = context.getString(title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                BrowserView(
                    url = uiState.url,
                    cookieDomain = platformConfig.cookieDomain,
                    onCookieChanged = onCookieChanged,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserView(
    url: String,
    cookieDomain: String,
    onCookieChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                }
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        },
        modifier = modifier
    )

    LaunchedEffect(cookieDomain, onCookieChanged) {
        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
        }

        try {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                val cookies = withContext(Dispatchers.IO) {
                    cookieManager.getCookie(cookieDomain)
                }
                onCookieChanged(cookies)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }
}