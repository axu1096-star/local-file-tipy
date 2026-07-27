package com.example.filebox.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.filebox.domain.Category
import com.example.filebox.ui.detail.FileDetailScreen
import com.example.filebox.ui.home.HomeScreen
import com.example.filebox.ui.library.LibraryScreen
import com.example.filebox.ui.settings.SettingsScreen
import com.example.filebox.ui.tags.TagBrowseScreen
import com.example.filebox.ui.tags.TagsScreen
import com.example.filebox.ui.tools.MarqueeToolScreen
import com.example.filebox.ui.tools.ToolsScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library/{category}"
    const val LIBRARY_UNTAGGED = "library/untagged"
    const val LIBRARY_BY_TAG = "libraryTag/{tagId}"
    const val DETAIL = "detail/{fileId}"
    const val SEARCH = "search"
    const val TAGS = "tags"
    const val TAG_BROWSE = "tagBrowse/{tagId}"
    const val TOOLS = "tools"
    const val TOOLS_MARQUEE = "tools/marquee"
    const val SETTINGS = "settings"

    fun library(category: Category) = "library/${category.name}"
    fun libraryByTag(tagId: Long) = "libraryTag/$tagId"
    fun tagBrowse(tagId: Long) = "tagBrowse/$tagId"
    fun detail(fileId: Long) = "detail/$fileId"
}

@Composable
fun FileboxNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenCategory = { nav.navigate(Routes.library(it)) },
                onOpenFile = { nav.navigate(Routes.detail(it)) },
                onOpenTags = { nav.navigate(Routes.TAGS) },
                onOpenTag = { nav.navigate(Routes.tagBrowse(it)) },
                onOpenUntagged = { nav.navigate(Routes.LIBRARY_UNTAGGED) },
                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                onOpenTools = { nav.navigate(Routes.TOOLS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.LIBRARY,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { entry ->
            val cat = runCatching {
                Category.valueOf(entry.arguments?.getString("category") ?: "OTHER")
            }.getOrDefault(Category.OTHER)
            LibraryScreen(
                filter = LibraryFilter.OfCategory(cat),
                onOpenFile = { nav.navigate(Routes.detail(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.LIBRARY_UNTAGGED) {
            LibraryScreen(
                filter = LibraryFilter.Untagged,
                onOpenFile = { nav.navigate(Routes.detail(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.SEARCH) {
            LibraryScreen(
                filter = LibraryFilter.All,
                onOpenFile = { nav.navigate(Routes.detail(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            route = Routes.LIBRARY_BY_TAG,
            arguments = listOf(navArgument("tagId") { type = NavType.LongType })
        ) { entry ->
            val tagId = entry.arguments?.getLong("tagId") ?: 0L
            LibraryScreen(
                filter = LibraryFilter.OfTag(tagId),
                onOpenFile = { nav.navigate(Routes.detail(it)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("fileId") ?: 0L
            FileDetailScreen(fileId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.TAGS) {
            TagsScreen(
                onBack = { nav.popBackStack() },
                onOpenBrowse = { nav.navigate(Routes.tagBrowse(it)) }
            )
        }
        composable(
            route = Routes.TAG_BROWSE,
            arguments = listOf(navArgument("tagId") { type = NavType.LongType })
        ) {
            TagBrowseScreen(
                onBack = { nav.popBackStack() },
                onOpenChildTag = { nav.navigate(Routes.tagBrowse(it)) },
                onOpenFile = { nav.navigate(Routes.detail(it)) }
            )
        }
        composable(Routes.TOOLS) {
            ToolsScreen(
                onBack = { nav.popBackStack() },
                onOpenMarquee = { nav.navigate(Routes.TOOLS_MARQUEE) }
            )
        }
        composable(Routes.TOOLS_MARQUEE) {
            MarqueeToolScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}

sealed interface LibraryFilter {
    data class OfCategory(val category: Category) : LibraryFilter
    data class OfTag(val tagId: Long) : LibraryFilter
    data object Untagged : LibraryFilter
    data object All : LibraryFilter
}
