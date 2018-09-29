package com.aliya.permission;

import android.support.annotation.NonNull;
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
     * @param isAlready 申请之前已全部默认授权
     */
    void onGranted(boolean isAlready);

    /**
     * 拒绝(至少一个权限拒绝)
     *
     * @param deniedPermissions   被拒绝权限集合(包括不再询问)
     * @param neverAskPermissions 被拒绝不再询问权限集合
     */
    void onDenied(@NonNull List<String> deniedPermissions, @Nullable List<String>
            neverAskPermissions);

}
