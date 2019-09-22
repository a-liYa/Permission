package com.aliya.permission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.aliya.permission.RequestHelper.findActivity;

/**
 * 动态权限申请工具类
 * see android.support.v4.app.ActivityCompat#requestPermissions(Activity, String[], int)
 *
 * @author a_liYa
 * @date 2016/7/21 22:22.
 */
public class PermissionManager {

    private static final int EMPTY = 0;

    private volatile static PermissionManager mInstance;

    private static Context sContext;

    /**
     * see /build.gradle文件 属性android.buildTypes.(release/debug)#debuggable true/false 来决定
     */
    static boolean sDebuggable = false;

    private static PermissionManager _get() {
        if (mInstance == null) {
            synchronized (PermissionManager.class) {
                if (mInstance == null) {
                    mInstance = new PermissionManager();
                }
            }
        }
        return mInstance;
    }

    private Set<String> mManifestPermissions;
    private final SparseArray<OpEntity> mRequestCaches;

    private PermissionManager() {
        mRequestCaches = new SparseArray<>();
    }

    /**
     * 动态权限申请
     *
     * @param context     Should be include activity.
     * @param callback    回调
     * @param permissions 权限集(字符串)
     * @return true：权限申请之前已全部允许
     * @see #request(Activity, PermissionCallback, String...)
     */
    public static boolean request(
            Context context, PermissionCallback callback, String... permissions) {
        return request(findActivity(context), callback, permissions);
    }

    /**
     * @see #request(Activity, PermissionCallback, Permission[], String[], boolean)
     */
    public static boolean request(
            Activity activity, PermissionCallback callback, String... permissions) {
        return request(activity, callback, null, permissions, false);
    }

    /**
     * 动态权限申请
     *
     * @param activityContext Should be include activity.
     * @param callback        回调
     * @param permissions     权限集
     * @return true：权限申请之前已全部允许
     * @see #request(Activity, PermissionCallback, String...)
     */
    public static boolean request(
            Context activityContext, PermissionCallback callback, Permission... permissions) {
        return request(findActivity(activityContext), callback, permissions);
    }

    /**
     * @see #request(Activity, PermissionCallback, Permission[], String[], boolean)
     */
    public static boolean request(
            Activity activity, PermissionCallback callback, Permission... permissions) {
        return request(activity, callback, permissions, null, false);
    }

    public static boolean request(
            Context activityContext, PermissionCallback callback, Permission.Group... groups) {
        Permission[] permissions = null;
        for (Permission.Group group : groups) {
            Permission[] groupPermissions = group.getGroup();
            if (permissions == null) {
                permissions = groupPermissions;
            } else {
                // 数组扩容并原封拷贝
                permissions = Arrays.copyOf(permissions,
                        permissions.length + groupPermissions.length);
                // 拷贝 groupPermissions 至 permissions 扩容部分
                System.arraycopy(groupPermissions, 0, permissions,
                        permissions.length - groupPermissions.length,
                        groupPermissions.length);
            }
        }
        return request(activityContext, callback, permissions);
    }

    /**
     * 动态申请权限
     * <p>
     * 注：所申请权限必须在Manifest中静态注册，否则可能崩溃
     *
     * @param activity          Activity
     * @param callback          回调
     * @param permissions       权限集
     * @param permissionStrings 权限集(字符串)
     * @return true：权限申请之前已全部允许
     */
    private static boolean request(Activity activity, PermissionCallback callback,
                                   Permission[] permissions, String[] permissionStrings,
                                   boolean isOpenSetting) {

        initContext(activity);

        int length = EMPTY;
        if (permissions != null) length += permissions.length;
        if (permissionStrings != null) length += permissionStrings.length;

        // 没有申请的权限
        if (length == EMPTY) return true;

        if (activity == null) {
            if (sDebuggable) throw new IllegalArgumentException("Activity shouldn't be null.");

            return false;
        }

        OpEntity opEntity = new OpEntity(callback, isOpenSetting);

        // 权限分类：已授权、待申请
        {
            if (permissions != null) {
                for (Permission permission : permissions) {
                    assortPermission(opEntity, permission.getPermission());
                }
            }

            if (permissionStrings != null) {
                for (String permission : permissionStrings) {
                    assortPermission(opEntity, permission);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 处理 分类权限
            if (equalsSize(opEntity.grantedPermissions, length)) {
                if (callback != null) callback.onGranted(true);
                return true;
            } else {
                if (equalsSize(opEntity.waitPermissions, EMPTY)) { // 待申请权限 == 0
                    if (callback != null) {
                        callback.onDenied(opEntity.deniedPermissions, opEntity.neverAskPermissions);
                    }
                } else {
                    _get().requestPermission(activity, opEntity);
                }
            }
        } else {
            return dispatchCallback(opEntity);
        }
        return false;
    }

    public static boolean requestOpenSetting(
            Context activityContext, PermissionCallback callback, Permission[] permissions) {
        return request(findActivity(activityContext), callback, permissions, null, true);
    }

    private static boolean dispatchCallback(OpEntity opEntity) {
        final boolean granted = equalsSize(opEntity.deniedPermissions, EMPTY);

        if (opEntity.callback != null) {
            if (granted) {
                opEntity.callback.onGranted(Build.VERSION.SDK_INT < Build.VERSION_CODES.M);
            } else {
                opEntity.callback
                        .onDenied(opEntity.deniedPermissions, opEntity.neverAskPermissions);
            }
        }

        return granted;
    }

    /**
     * 权限申请结果处理
     *
     * @param requestCode  请求码
     * @param permissions  申请权限集合 {@link Permission}
     * @param grantResults 申请结果集合
     */
    static void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults,
                                          PermissionOperate showRationale) {

        OpEntity opEntity = _get().mRequestCaches.get(requestCode);
        if (opEntity != null) {
            _get().mRequestCaches.remove(requestCode);

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {  // 权限被授予
                    opEntity.addGrantedPermission(permissions[i]);
                } else { // 权限被拒绝
                    if (!showRationale.exeShouldShowRequestPermissionRationale((permissions[i]))) {
                        // 1、拒绝且不再询问
                        opEntity.addNeverAskPermission(permissions[i]);
                    } else {
                        // 2、拒绝
                        opEntity.addDeniedPermission(permissions[i]);
                    }
                }
            }

            dispatchCallback(opEntity);
        }
    }

    /**
     * @param activityContext Should be include activity.
     * @param permission      权限名称
     * @return true : 应该向用户解释权限用途
     * @see Activity#shouldShowRequestPermissionRationale(String)
     */
    public static boolean shouldShowRequestPermissionRationale(Context activityContext,
                                                               @NonNull String permission) {
        initContext(activityContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Activity activity = findActivity(activityContext);
            if (activity != null) {
                return activity.shouldShowRequestPermissionRationale(permission);
            }
        }
        return false;
    }


    /**
     * 权限区分归类
     *
     * @param opEntity   .
     * @param permission 权限名称
     */
    static void assortPermission(OpEntity opEntity, String permission) {
        // 判断权限是否被授予
        if (checkPermission(sContext, permission)) {
            opEntity.addGrantedPermission(permission);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                opEntity.addWaitPermission(permission);
            } else {
                opEntity.addNeverAskPermission(permission);
            }
        }
    }

    /**
     * 检查权限是否已经全部授权
     *
     * @param context     A any context
     * @param permissions 权限集合
     * @return true: 已全部授权
     */
    public static boolean checkPermission(Context context, String... permissions) {
        for (String permission : permissions) {
            // 对比 PermissionChecker.checkSelfPermission(sContext, permission)
            if (context.checkPermission(permission, android.os.Process.myPid(), Process.myUid()) !=
                    PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    public static boolean checkPermission(Context context, Permission... permissions) {
        for (Permission permission : permissions) {
            if (context.checkPermission(permission.getPermission(), android.os.Process.myPid(),
                    Process.myUid()) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * 获取应用设置页面的 Intent
     *
     * @param context A any context
     * @return intent
     */
    public static Intent getSettingIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    public static boolean containsForManifest(Context context, String permission) {
        return _get().manifestContains(context, permission);
    }

    static void initContext(Context context) {
        if (sContext == null && context != null) {
            sContext = context.getApplicationContext();
            try {
                sDebuggable =
                        (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            } catch (Exception e) {
                sDebuggable = false;
            }
        }
    }

    private boolean manifestContains(Context context, String permission) {
        if (mManifestPermissions == null) {
            initContext(context);
            mManifestPermissions = getManifestPermissions();
        }
        return mManifestPermissions.contains(permission);
    }

    private Set<String> getManifestPermissions() {
        Set<String> manifestPermissions = null;
        PackageInfo packageInfo = null;
        try {
            packageInfo = sContext.getPackageManager().getPackageInfo(
                    sContext.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            String[] permissions = packageInfo.requestedPermissions;
            if (permissions != null && permissions.length != 0) {
                manifestPermissions = new HashSet<>(Arrays.asList(permissions));
            }
        }

        return manifestPermissions != null ? manifestPermissions : new HashSet<String>(0);
    }

    private void requestPermission(Activity activity, OpEntity opEntity) {
        mRequestCaches.put(opEntity.requestCode, opEntity);
        PermissionOperate operate = RequestHelper.getPermissionOperate(activity);
        operate.exeRequestPermissions(opEntity.getWaitPermsArray(), opEntity.requestCode);
        opEntity.waitPermissions = null;
    }

    static boolean equalsSize(List list, int size) {
        return (list != null ? list.size() : 0) == size;
    }
}
