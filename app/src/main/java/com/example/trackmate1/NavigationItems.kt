package com.example.trackmate1

sealed class NavigationItems(val route: String) {

    object Map : NavigationItems("map")
    object Profile : NavigationItems("profile")
    object Search: NavigationItems("search")

}