package com.devicespooflab.hooks.utils

import de.robv.android.xposed.XposedHelpers

object ReflectionHelper {
    fun setStaticObjectFieldSafe(targetClass: Class<*>, fieldName: String, value: Any?) {
        try {
            if (value == null) return
            val field = XposedHelpers.findFieldIfExists(targetClass, fieldName)
            if (field != null) {
                field.isAccessible = true
                field.set(null, value)
            }
        } catch (ignored: Throwable) {}
    }
}
