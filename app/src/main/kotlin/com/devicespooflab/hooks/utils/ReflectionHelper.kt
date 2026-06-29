package com.devicespooflab.hooks.utils

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedBridge

object ReflectionHelper {
    fun setStaticObjectFieldSafe(clazz: Class<*>, fieldName: String, value: Any?) {
        if (value == null) return
        try {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value)
        } catch (e: NoSuchFieldError) {
            XposedBridge.log("ReflectionHelper: Field $fieldName not found in ${clazz.name}")
        } catch (e: Exception) {
            XposedBridge.log("ReflectionHelper: Error setting field $fieldName in ${clazz.name}: ${e.message}")
        }
    }
}
