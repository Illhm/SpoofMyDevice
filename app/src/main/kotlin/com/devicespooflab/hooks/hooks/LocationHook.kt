package com.devicespooflab.hooks.hooks

import android.location.Location
import android.location.LocationManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class LocationHook : HookModule {
    override val priority = 20
    override val targetPackages = listOf("all")

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // Location (6 param) — Prioritas P1

        try {
            val locationClass = Location::class.java

            XposedHelpers.findAndHookMethod(locationClass, "getLatitude", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    params["geo_latitude"]?.toDoubleOrNull()?.let { param.result = it }
                }
            })

            XposedHelpers.findAndHookMethod(locationClass, "getLongitude", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    params["geo_longitude"]?.toDoubleOrNull()?.let { param.result = it }
                }
            })

            XposedHelpers.findAndHookMethod(locationClass, "getAltitude", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    params["geo_altitude"]?.toDoubleOrNull()?.let { param.result = it }
                }
            })

            XposedHelpers.findAndHookMethod(locationClass, "getAccuracy", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    params["geo_accuracy"]?.toFloatOrNull()?.let { param.result = it }
                }
            })

            XposedHelpers.findAndHookMethod(locationClass, "getSpeed", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    params["geo_speed"]?.toFloatOrNull()?.let { param.result = it }
                }
            })

            XposedHelpers.findAndHookMethod(locationClass, "getBearing", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    params["geo_bearing"]?.toFloatOrNull()?.let { param.result = it }
                }
            })

            // Also mock LocationManager.getLastKnownLocation
            val locationManagerClass = LocationManager::class.java
            XposedHelpers.findAndHookMethod(
                locationManagerClass, "getLastKnownLocation", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        var loc = param.result as? Location
                        if (loc == null) {
                            loc = Location(param.args[0] as String)
                            param.result = loc
                        }

                        // Apply spoofed values to the returned Location object directly
                        // just in case they don't call getters (e.g. read directly if accessible somehow, though usually they use getters)
                        params["geo_latitude"]?.toDoubleOrNull()?.let { loc.latitude = it }
                        params["geo_longitude"]?.toDoubleOrNull()?.let { loc.longitude = it }
                        params["geo_altitude"]?.toDoubleOrNull()?.let { loc.altitude = it }
                        params["geo_accuracy"]?.toFloatOrNull()?.let { loc.accuracy = it }
                        params["geo_speed"]?.toFloatOrNull()?.let { loc.speed = it }
                        params["geo_bearing"]?.toFloatOrNull()?.let { loc.bearing = it }
                    }
                }
            )

        } catch (e: Exception) {}

        return true
    }
}
