package com.stand.sounder_app.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stand.sounder_app.ui.screens.detail.DetailScreen
import com.stand.sounder_app.ui.screens.edit.EditScreen
import com.stand.sounder_app.ui.screens.personal.PersonalResourceScreen
import com.stand.sounder_app.ui.screens.search.SearchScreen
import com.stand.sounder_app.ui.screens.shop.ShopScreen
import com.stand.sounder_app.ui.screens.settings.SettingsScreen
import com.stand.sounder_app.ui.screens.submissions.SubmissionsScreen
import com.stand.sounder_app.ui.screens.submissions.SubmissionFormScreen
import com.stand.sounder_app.ui.screens.tasks.TaskManagerScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.PersonalResource.route,
        modifier = modifier
    ) {
        // 底部导航 Tab
        composable(Screen.PersonalResource.route) {
            PersonalResourceScreen(
                onResourceClick = { resourceId ->
                    navController.navigate(Screen.Detail.createRoute(resourceId, "local"))
                },
                onEditResource = { resourceId ->
                    navController.navigate(Screen.Edit.createRoute(resourceId))
                },
                onGoToShop = {
                    navController.navigate(Screen.Shop.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEdit = { resourceId ->
                    navController.navigate(Screen.Edit.createRoute(resourceId))
                }
            )
        }

        composable(Screen.Shop.route) {
            ShopScreen(
                onResourceClick = { resourceId, isInstalled ->
                    val mode = if (isInstalled) "local" else "cloud"
                    navController.navigate(Screen.Detail.createRoute(resourceId, mode))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // 投稿列表
        composable(Screen.Submissions.route) {
            SubmissionsScreen(
                onNewSubmission = {
                    navController.navigate(Screen.SubmissionForm.route)
                }
            )
        }

        // 新建投稿（子页面，从右滑入）
        composable(
            route = Screen.SubmissionForm.route,
            enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
            exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth / 3 } },
            popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth / 3 } },
            popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
        ) {
            SubmissionFormScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 任务管理（后台播放监控）
        composable(Screen.Tasks.route) {
            TaskManagerScreen(bottomBarVisible = true)
        }

        // 子页面（翻页过渡：从右滑入，向左滑出）
        composable(
            route = Screen.Detail.route,
            enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
            exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth / 3 } },
            popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth / 3 } },
            popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } },
            arguments = listOf(
                navArgument("resourceId") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            val mode = backStackEntry.arguments?.getString("mode") ?: "local"
            DetailScreen(
                resourceId = resourceId,
                mode = mode,
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.Edit.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.Edit.route,
            enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
            exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth / 3 } },
            popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth / 3 } },
            popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } },
            arguments = listOf(
                navArgument("resourceId") { type = NavType.StringType },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "edit"
                }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: return@composable
            EditScreen(
                resourceId = resourceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.Search.route,
            enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
            exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth / 3 } },
            popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth / 3 } },
            popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } }
        ) {
            SearchScreen(
                onResourceClick = { resourceId ->
                    navController.navigate(Screen.Detail.createRoute(resourceId, "cloud"))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
