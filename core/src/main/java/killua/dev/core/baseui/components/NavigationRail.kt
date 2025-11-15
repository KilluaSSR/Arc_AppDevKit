package killua.dev.base.ui.Components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import killua.dev.core.baseui.navigation.NavbarEssentials
import killua.dev.core.utils.LocalNavController
import killua.dev.core.utils.navigateSingle

@Composable
fun NavigationRailBase(
    modifier: Modifier = Modifier,
    sidebarItems: List<NavbarEssentials>,
    startDestinationRoute: String
){
    val navController = LocalNavController.current!!
    val startIndex = sidebarItems.indexOfFirst { it.route == startDestinationRoute }
        .takeIf { it >= 0 } ?: 0
    val selectedDestination by rememberSaveable { mutableIntStateOf(startIndex) }
    Scaffold(modifier = modifier) { contentPadding->
        NavigationRail(
            modifier = Modifier.padding(contentPadding)
        ) {
            sidebarItems.forEachIndexed { index, destination ->
                NavigationRailItem(
                    selected = selectedDestination == index,
                    onClick = {
                        navController.navigateSingle(route = destination.route)
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.route
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = destination.description)
                        )
                    }
                )
            }
        }
    }
}