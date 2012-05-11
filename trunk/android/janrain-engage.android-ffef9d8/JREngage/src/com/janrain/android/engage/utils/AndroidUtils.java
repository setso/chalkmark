/*
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Copyright (c) 2011, Janrain, Inc.
 *
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *  * Neither the name of the Janrain, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

package com.janrain.android.engage.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import com.janrain.android.engage.JREngage;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @internal
 *
 * @class AndroidUtils
 **/

public class AndroidUtils {
    public static final String TAG = AndroidUtils.class.getSimpleName();
    private AndroidUtils() {}

    public static boolean isSmallNormalOrLargeScreen() {
        int screenConfig = getScreenSize();

        // Galaxy Tab 7" (the first one) reports SCREENLAYOUT_SIZE_NORMAL
        // Motorola Xoom reports SCREENLAYOUT_SIZE_XLARGE
        // Nexus S reports SCREENLAYOUT_SIZE_NORMAL

        return screenConfig == Configuration.SCREENLAYOUT_SIZE_NORMAL ||
                screenConfig == Configuration.SCREENLAYOUT_SIZE_SMALL ||
                screenConfig == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isCupcake() {
        return Build.VERSION.RELEASE.startsWith("1.5");
    }

    private static int getAndroidSdkInt() {
        try {
            return Build.VERSION.class.getField("SDK_INT").getInt(null);
        } catch (NoSuchFieldException e) {
            // Must be Cupcake
            return 3;
        } catch (IllegalAccessException e) {
            // Not expected
            throw new RuntimeException(e);
        }
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static final int SDK_INT = getAndroidSdkInt();

    public static ApplicationInfo getApplicationInfo() {
        String packageName = JREngage.getActivity().getPackageName();
        try {
            return JREngage.getActivity().getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static int scaleDipToPixels(int dip) {
        Context c = JREngage.getActivity();
        final float scale = c.getResources().getDisplayMetrics().density;
        return (int) (((float) dip) * scale);
    }

    private static int getScreenSize() {

        int screenConfig = JREngage.getActivity().getResources().getConfiguration().screenLayout;
        screenConfig &= Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenConfig;
    }

    public static boolean isXlarge() {
        return (getScreenSize() == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    public static boolean isLandscape() {
        return JREngage.getActivity().getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
    }

    //public static int getScreenWidth() {
    //    DisplayMetrics metrics = new DisplayMetrics();
    //    JREngage.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    //    metrics.get
    //}
}
