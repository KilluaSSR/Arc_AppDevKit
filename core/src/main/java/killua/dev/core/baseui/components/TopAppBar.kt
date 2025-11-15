package killua.dev.base.ui.Components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import killua.dev.core.baseui.components.ArrowBackButton
import killua.dev.core.utils.LocalDrawerController
import killua.dev.core.utils.LocalNavController
import killua.dev.core.utils.maybePopBackStack


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavHostController,
    @StringRes title: Int,
    enableNavIcon: Boolean = true,
    extraIcons: @Composable () -> Unit = {},
    showMoreIcon: Boolean = true,
    showMoreOnClick: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(stringResource(title)) },
        navigationIcon = {
            if(enableNavIcon){
                ArrowBackButton(){
                    navController.popBackStack()
                }
            }
        },
        actions = {
            extraIcons()
            if(showMoreIcon){
                IconButton(
                    onClick = showMoreOnClick
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        null
                    )
                    content()
                }
            }
        }
    )
}



@ExperimentalMaterial3Api
@Composable
fun SecondaryLargeTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    @StringRes title: Int,
    leftActions: TopAppBarLeftActions = TopAppBarLeftActions.MENU,
    actions: @Composable RowScope.() -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val navController = LocalNavController.current!!
    val openDrawer = LocalDrawerController.current
    LargeTopAppBar(
        title = { Text(text = stringResource(title)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            when(leftActions){
                TopAppBarLeftActions.BACK -> {
                    ArrowBackButton {
                        if (onBackClick != null) onBackClick.invoke()
                        else navController.maybePopBackStack()
                    }
                }
                TopAppBarLeftActions.MENU -> {
                    IconButton(
                        onClick = {
                            openDrawer?.invoke()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation drawer",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                TopAppBarLeftActions.NONE -> {}
            }
        },
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    @StringRes title: Int,
    leftActions: TopAppBarLeftActions = TopAppBarLeftActions.MENU,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CoreMainTopBar(
        titleRes = title,
        titleString = null,
        leftActions = leftActions,
        onBackClick = onBackClick,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopStringBar(
    title: String,
    leftActions: TopAppBarLeftActions = TopAppBarLeftActions.MENU,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CoreMainTopBar(
        titleRes = null,
        titleString = title,
        leftActions = leftActions,
        onBackClick = onBackClick,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoreMainTopBar(
    @StringRes titleRes: Int? = null,
    titleString: String? = null,
    leftActions: TopAppBarLeftActions = TopAppBarLeftActions.MENU,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val openDrawer = LocalDrawerController.current
    val navController = LocalNavController.current

    // Determine the title text
    val titleText = when {
        titleRes != null -> stringResource(id = titleRes)
        titleString != null -> titleString
        else -> "" // Should not happen if one of the entry points is used
    }

    TopAppBar(
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        },
        navigationIcon = {
            when(leftActions){
                TopAppBarLeftActions.BACK -> {
                    ArrowBackButton {
                        if (onBackClick != null) onBackClick.invoke()
                        else navController?.maybePopBackStack()
                    }
                }
                TopAppBarLeftActions.MENU -> {
                    IconButton(
                        onClick = {
                            openDrawer?.invoke()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation drawer",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                TopAppBarLeftActions.NONE -> {}
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
enum class TopAppBarLeftActions{
    BACK,
    MENU,
    NONE
}