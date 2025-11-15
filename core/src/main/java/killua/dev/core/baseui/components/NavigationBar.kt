package killua.dev.base.ui.Components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import killua.dev.base.ui.Tokens.SizeTokens
import killua.dev.core.baseui.navigation.NavbarEssentials
import killua.dev.core.utils.LocalNavController
import killua.dev.core.utils.navigateSingle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerNavigationBase(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable (drawerState: DrawerState) -> Unit)? = null,
    sidebarHorizontalItems: List<NavbarEssentials>? = null,
    sidebarVerticalItems: List<NavbarEssentials>,
    startDestinationRoute: String,
    content: @Composable (openDrawer: () -> Unit) -> Unit
){
    val navController = LocalNavController.current!!
    val selectedDestination = rememberSaveable { mutableStateOf(startDestinationRoute) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val openDrawer: () -> Unit = {
        scope.launch {
            drawerState.open()
        }
        Unit
    }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(SizeTokens.Level152 + SizeTokens.Level128),
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {

                if (leadingContent != null) {
                    leadingContent(drawerState)
                }

                if (sidebarHorizontalItems != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = SizeTokens.Level8),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sidebarHorizontalItems.forEachIndexed { index, item->
                            IconButton(onClick = {
                                selectedDestination.value = item.route
                                navController.navigateSingle(item.route)
                                scope.launch { drawerState.close() }
                            }) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.route,
                                    tint = if (selectedDestination.value == item.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if(index != sidebarHorizontalItems.lastIndex){
                                VerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(8.dp),
                                    thickness = DividerDefaults.Thickness, color = DividerDefaults.color
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = SizeTokens.Level16),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                }

                Spacer(modifier = Modifier.height(SizeTokens.Level8))
                
                sidebarVerticalItems.forEach { destination ->
                    NavigationDrawerItem(
                        label = { 
                            Text(
                                text = stringResource(id = destination.description),
                                style = MaterialTheme.typography.bodyLarge
                            ) 
                        },
                        selected = selectedDestination.value == destination.route,
                        onClick = {
                            selectedDestination.value = destination.route
                            navController.navigateSingle(destination.route)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon, 
                                contentDescription = destination.route
                            )
                        },
                        modifier = Modifier.padding(
                            horizontal = SizeTokens.Level12
                        ),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ){
        content(openDrawer)
    }
}