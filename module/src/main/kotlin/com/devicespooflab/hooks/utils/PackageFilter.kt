package com.devicespooflab.hooks.utils

object PackageFilter {
    const val SYSTEM_SERVER = "android"
    const val PHONE = "com.android.phone"

    fun isSystemProcess(packageName: String): Boolean {
        return packageName == SYSTEM_SERVER || packageName == PHONE
    }

    // In a real scenario, this would check if the app is selected in a GUI
    // For this module, we will assume all packages are targeted unless specified
    fun isTargetApp(packageName: String): Boolean {
        return !isSystemProcess(packageName) && packageName != "com.spoofmydevice"
    }
}
