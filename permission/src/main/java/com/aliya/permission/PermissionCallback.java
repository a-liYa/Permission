package com.aliya.permission;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * 权限动态申请回调监听
 *
 * @author a_liYa
 * @date 2016/9/18 10:55.
 */
public interface PermissionCallback {
    /**
     * 全部授予
     *
     * @param isAlreadyDef 申请之前已全部默认授权
     */
    void onGranted(boolean isAlreadyDef);

    /**
     * 全部拒绝 包括不再询问权限
     *
     * @param neverAskPermissions 被拒绝(不再询问)权限集合
     */
    void onDenied(@Nullable List<String> neverAskPermissions);

    /**
     * 其他情况
     *
     * @param deniedPermissions   被拒绝权限集合(包括不再询问)
     * @param neverAskPermissions 被拒绝(不再询问)权限集合
     */
    void onElse(@Nullable List<String> deniedPermissions, @Nullable List<String> neverAskPermissions);
}
