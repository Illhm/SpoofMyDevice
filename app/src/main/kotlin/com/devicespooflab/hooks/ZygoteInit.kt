package com.devicespooflab.hooks

import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge

class ZygoteInit : IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        XposedBridge.log("DeviceSpoofLab Hooks Zygote Initialized")
    }
}
