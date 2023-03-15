package com.iterable.iterableapi.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public final class BrandUtils {

    private BrandUtils() {
    }

    public static boolean isFireTV(PackageManager packageManager) {
        String amazonFireTvHardware = "amazon.hardware.fire_tv";
        String amazonModel = Build.MODEL;
        if (amazonModel.matches("AFTN") || packageManager.hasSystemFeature(amazonFireTvHardware)) {
            return true;
        } else {
            return false;
        }
    }
}
