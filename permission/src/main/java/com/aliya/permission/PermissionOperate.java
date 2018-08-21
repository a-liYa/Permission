package com.aliya.permission;

import android.support.annotation.NonNull;

/**
 * 动态权限申请操作相关接口定义
 *
 * @author a_liYa
 * @date 2016/9/17 21:37.
 */
interface PermissionOperate {

    /**
     * 申请权限
     *
     * @param permissions 权限数组
     * @param requestCode 请求Code
     * @see android.app.Activity#requestPermissions(String[], int)
     * @see android.app.Fragment#requestPermissions(String[], int)
     */
    void exeRequestPermissions(@NonNull String[] permissions, int requestCode);

    /**
     * 告诉我们是否应该向用户展示自定义UI解释请求权限的原因
     * 1、请求权限之前，返回false；
     * 2、请求权限被允许之后，返回false；
     * 3、请求权限被拒绝之后，返回true；
     * 4、请求权限被拒绝(且勾选不在询问)之后，返回false；
     * <p>
     * 代理Activity、Fragment#shouldShowRequestPermissionRationale 功能
     *
     * @param permission 权限
     * @return true会弹窗提醒，false不在显示提醒
     * @see android.app.Activity#shouldShowRequestPermissionRationale(String)
     * @see android.app.Fragment#shouldShowRequestPermissionRationale(String)
     */
    boolean exeShouldShowRequestPermissionRationale(@NonNull String permission);

}
