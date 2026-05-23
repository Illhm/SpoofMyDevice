package com.devicespooflab.hooks.security;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Process;

import androidx.annotation.NonNull;

public final class CallerVerifier {

    public static final String BRIDGE_PERMISSION = "com.spoofmydevice.permission.CONFIG_BRIDGE";

    private CallerVerifier() {
    }

    public static void enforceTrustedCaller(@NonNull Context context) {
        final int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid()) {
            return;
        }

        PackageManager pm = context.getPackageManager();
        String[] packages = pm.getPackagesForUid(callingUid);
        if (packages == null || packages.length == 0) {
            throw new SecurityException("No package mapped to calling UID=" + callingUid);
        }

        boolean hasPermission = false;
        boolean signatureMatch = false;
        String selfPackage = context.getPackageName();

        for (String pkg : packages) {
            if (pm.checkPermission(BRIDGE_PERMISSION, pkg) == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;
            }
            if (pm.checkSignatures(selfPackage, pkg) == PackageManager.SIGNATURE_MATCH) {
                signatureMatch = true;
            }
            if (hasPermission && signatureMatch) {
                break;
            }
        }

        if (!hasPermission) {
            throw new SecurityException("Calling UID=" + callingUid + " missing signature permission");
        }
        if (!signatureMatch) {
            throw new SecurityException("Calling UID=" + callingUid + " signature mismatch");
        }
    }
}
