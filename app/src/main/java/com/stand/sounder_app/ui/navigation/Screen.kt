package com.stand.sounder_app.ui.navigation

import com.stand.sounder_app.R

/** 底部导航 Tab —— 参考 docs/02-module-architecture.md（我的资源/商店/投稿/任务管理/设置） */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val iconActive: Int,
    val iconInactive: Int
) {
    data object PersonalResource : BottomNavItem(
        route = "personal_resource",
        title = "我的资源",
        iconActive = R.drawable.sidebar_manage_active,
        iconInactive = R.drawable.sidebar_manage_close
    )

    data object Shop : BottomNavItem(
        route = "shop",
        title = "商店",
        iconActive = R.drawable.sidebar_shop_active,
        iconInactive = R.drawable.sidebar_shop_close
    )

    data object Submissions : BottomNavItem(
        route = "submissions",
        title = "投稿",
        iconActive = R.drawable.sidebar_plan_active,
        iconInactive = R.drawable.sidebar_plan_close
    )

    data object Tasks : BottomNavItem(
        route = "tasks",
        title = "任务管理",
        iconActive = R.drawable.sidebar_runner_active,
        iconInactive = R.drawable.sidebar_runner_close
    )

    data object Settings : BottomNavItem(
        route = "settings",
        title = "设置",
        iconActive = R.drawable.sidebar_settings_active,
        iconInactive = R.drawable.sidebar_settings_close
    )

    companion object {
        val items = listOf(PersonalResource, Shop, Submissions, Tasks, Settings)
    }
}

/** 所有路由 */
sealed class Screen(val route: String) {
    data object PersonalResource : Screen("personal_resource")
    data object Shop : Screen("shop")
    data object Submissions : Screen("submissions")
    data object Tasks : Screen("tasks")
    data object Settings : Screen("settings")
    data object Detail : Screen("detail/{resourceId}/{mode}") {
        fun createRoute(resourceId: String, mode: String = "local") = "detail/$resourceId/$mode"
    }
    data object Edit : Screen("edit/{resourceId}?mode={mode}") {
        fun createRoute(resourceId: String, mode: String = "edit") = "edit/$resourceId?mode=$mode"
    }
    data object Search : Screen("search")
    data object SubmissionForm : Screen("submission_form")
}
