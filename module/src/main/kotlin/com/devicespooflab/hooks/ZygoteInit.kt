package com.devicespooflab.hooks

import de.robv.android.xposed.IXposedHookZygoteInit

class ZygoteInit : IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        // Pre-load logic if necessary, typically modules register basic stuff here
    }
}
