package com.aliya.permission;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/**
 * SettingIntents
 *
 * @author a_liYa
 * @date 2020/11/13 15:51.
 */
public final class SettingIntents {

    /**
     * 获取应用设置页面的 Intent
     *
     * @param context Any of the context
     * @return intent
     */
    public static Intent getAppDetailsIntent(Context context) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.getPackageName()));
    }

    /**
     * 在其他应用上层显示应用的权限授权 设置页
     *
     * @param context Any of the context
     * @return intent
     */
    public static Intent getOverlayPermissionIntent(Context context) {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
    }
}
