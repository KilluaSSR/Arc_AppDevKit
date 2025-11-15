package killua.dev.base.ui.Components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import killua.dev.base.ui.Tokens.SizeTokens
@Composable
fun LoadingLinear(
    @StringRes text: Int
){
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(text),
                modifier = Modifier.alpha(0.3f)
            )
            Spacer(modifier =  Modifier.size(SizeTokens.Level16))
            LinearProgressIndicator(
                modifier = Modifier
                    .size(width = SizeTokens.Level128, height = SizeTokens.Level8)
            )
        }
    }
}