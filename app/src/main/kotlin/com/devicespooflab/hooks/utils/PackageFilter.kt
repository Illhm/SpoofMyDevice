package com.devicespooflab.hooks.utils

object PackageFilter {
    private val systemPackages = listOf("android", "com.android.systemui", "com.android.phone")

    fun isSystemProcess(packageName: String): Boolean {
        return packageName == "android" || packageName == "system_server"
    }

    fun isPhoneProcess(packageName: String): Boolean {
        return packageName == "com.android.phone"
    }

    fun isTargetApp(packageName: String): Boolean {
        // Here we could implement more complex logic.
        // For now, assume any package that isn't empty is a target.
        return packageName.isNotEmpty()
    }
}
