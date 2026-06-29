package com.devicespooflab.hooks.utils

object PackageFilter {
    fun isSystemProcess(packageName: String): Boolean =
        packageName == "android" || packageName == "system_server"

    fun isPhoneProcess(packageName: String): Boolean =
        packageName == "com.android.phone"
}
