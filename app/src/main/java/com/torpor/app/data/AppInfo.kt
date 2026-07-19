package com.torpor.app.data

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?,
    var isRestricted: Boolean = false,
    var isLocked: Boolean = false
)
