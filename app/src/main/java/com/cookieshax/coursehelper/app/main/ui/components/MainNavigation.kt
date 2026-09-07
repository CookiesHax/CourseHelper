package com.cookieshax.coursehelper.app.main.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.app.main.MainTab
import com.cookieshax.coursehelper.app.main.ui.items.RailHorizontalItem
import com.cookieshax.coursehelper.ui.items.IcTags

@Composable
fun MainNavigationRail(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    isRailExpanded: Boolean,
    onToggleRail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val railWidth by animateDpAsState(
        targetValue = if (isRailExpanded) 200.dp else 80.dp,
        label = "rail_width_animation"
    )

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth),
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = onToggleRail) {
                    Icon(Icons.Default.Menu, contentDescription = "切换菜单模式")
                }
            }
        }
    ) {
        Spacer(Modifier.height(8.dp))
        AnimatedContent(
            targetState = isRailExpanded,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            label = "rail_content_transition"
        ) { expanded ->
            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RailHorizontalItem(
                        selected = selectedTab == MainTab.COURSE,
                        onClick = { onTabSelected(MainTab.COURSE) },
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = "课程"
                    )
                    RailHorizontalItem(
                        selected = selectedTab == MainTab.ACCOUNT,
                        onClick = { onTabSelected(MainTab.ACCOUNT) },
                        icon = Icons.Default.Person,
                        label = "账号"
                    )

                    Spacer(Modifier.height(8.dp))

                    RailHorizontalItem(
                        selected = selectedTab == MainTab.TAGS,
                        onClick = { onTabSelected(MainTab.TAGS) },
                        icon = IcTags,
                        label = "标签管理"
                    )
                    RailHorizontalItem(
                        selected = selectedTab == MainTab.SETTINGS,
                        onClick = { onTabSelected(MainTab.SETTINGS) },
                        icon = Icons.Default.Settings,
                        label = "系统设置"
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NavigationRailItem(
                        selected = selectedTab == MainTab.COURSE,
                        onClick = { onTabSelected(MainTab.COURSE) },
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null
                            )
                        },
                        label = { Text("课程") },
                        alwaysShowLabel = true
                    )
                    NavigationRailItem(
                        selected = selectedTab == MainTab.ACCOUNT,
                        onClick = { onTabSelected(MainTab.ACCOUNT) },
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        label = { Text("账号") },
                        alwaysShowLabel = true
                    )

                    NavigationRailItem(
                        selected = selectedTab == MainTab.TAGS,
                        onClick = { onTabSelected(MainTab.TAGS) },
                        icon = { Icon(IcTags, contentDescription = null) },
                        label = { Text("标签") },
                        alwaysShowLabel = true
                    )
                    NavigationRailItem(
                        selected = selectedTab == MainTab.SETTINGS,
                        onClick = { onTabSelected(MainTab.SETTINGS) },
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null
                            )
                        },
                        label = { Text("设置") },
                        alwaysShowLabel = true
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = selectedTab == MainTab.COURSE,
            onClick = { onTabSelected(MainTab.COURSE) },
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null
                )
            },
            label = { Text("课程") }
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.ACCOUNT,
            onClick = { onTabSelected(MainTab.ACCOUNT) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("账号") }
        )
    }
}
