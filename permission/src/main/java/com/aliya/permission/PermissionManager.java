package com.aliya.permission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.SparseArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.aliya.permission.PermissionManager.OpEntity.equalsSize;

/**
 * 动态权限申请工具类
 *
 * @author a_liYa
 * @date 2016/7/21 22:22.
 */
public class PermissionManager {
    /**
     * 被拒绝权限（不再询问）
     */
    private static final String PERMISSION_NEVER_ASK = "permission_never_ask_sets";

    private volatile static PermissionManager mInstance;

    private static Context sContext;

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

    private final SparseArray<OpEntity> mRequestCaches;
    private Set<String> mManifestPermissions;

    // 私有构造方法
    private PermissionManager() {
        mRequestCaches = new SparseArray<>();
        mManifestPermissions = getManifestPermissions();
    }

//        ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, PERMISSION_REQUEST_CODE);

    /**
     * 动态申请权限
     * <p/>
     * 注：所申请权限必须在Manifest中静态注册，否则可能崩溃
     *
     * @param activity    activity
     * @param callback    回调
     * @param permissions 权限数组
     * @return true：默认之前已经全部授权
     */
    public static boolean request(
            Activity activity, PermissionCallback callback, Permission... permissions) {

        initContext(activity);

        if (permissions == null) // 没有申请的权限 return true
            return true;

        if (activity == null || callback == null) {
            return false;
        }

        OpEntity opEntity = new OpEntity(callback);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Set<String> neverAskPermissions = null;
            for (Permission permission : permissions) { // 权限分类：已授权、已拒绝（不再询问）、待申请

                // 检查申请的权限是否在 AndroidManifest.xml 中
                if (_get().mManifestPermissions.contains(permission.getPermission())) {
                    //判断权限是否被授予
                    if (checkPermission(permission.getPermission())) {
                        opEntity.addGrantedPermission(permission.getPermission());
                    } else {
                        if (neverAskPermissions == null) {
                            neverAskPermissions = getNeverAskPermissions();
                        }
                        if (neverAskPermissions.contains(permission.getPermission())) {
                            //  没有被用户禁止弹窗提示
                            opEntity.addNeverAskPermission(permission.getPermission());
                        } else {
                            opEntity.addWaitPermission(permission.getPermission());
                        }
                    }
                } else {
                    opEntity.addNeverAskPermission(permission.getPermission());
                }
            }

            // 处理 分类权限
            if (equalsSize(opEntity.grantedPermissions, permissions.length)) {
                callback.onGranted(true);
                return true;
            } else {
                if (equalsSize(opEntity.waitPermissions, 0)) { // 待申请权限 == 0
                    if (equalsSize(opEntity.grantedPermissions, 0)) {
                        // 待申请 == 0 && 已授权 == 0
                        callback.onDenied(opEntity.neverAskPermissions);
                    } else { // 其他情况：部分拒绝、部分已授权
                        callback.onElse(opEntity.deniedPermissions, opEntity.neverAskPermissions);
                    }
                } else {
                    _get().requestPermission(activity, opEntity);
                }
            }
        } else {
            callback.onGranted(true);
            return true;
        }

        return false;
    }

    private static Set getNeverAskPermissions() {
        return SPHelper.get(sContext).get(PERMISSION_NEVER_ASK, Collections.EMPTY_SET);
    }

    /**
     * 权限申请结果处理
     *
     * @param requestCode  请求码
     * @param permissions  申请权限集合 {@link Permission}
     * @param grantResults 申请结果集合
     */
    public static void onRequestPermissionResult(int requestCode, String[]
            permissions, int[] grantResults, PermissionOperate showRationale) {

        OpEntity opEntity = _get().mRequestCaches.get(requestCode);
        if (opEntity != null) {
            _get().mRequestCaches.remove(requestCode);

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {  // 权限被授予
                    opEntity.addGrantedPermission(permissions[i]);
                } else {  // 权限被拒绝 1、拒绝 2、拒绝并不再询问
                    if (!showRationale.exeShouldShowRequestPermissionRationale((permissions[i]))) {
                        SPHelper spHelper = SPHelper.get(sContext);
                        Set<String> neverAskPerms = spHelper.get(PERMISSION_NEVER_ASK,
                                new HashSet<String>());
                        neverAskPerms.add(permissions[i]);
                        spHelper.put(PERMISSION_NEVER_ASK, neverAskPerms);
                        opEntity.addNeverAskPermission(permissions[i]);
                    } else {
                        opEntity.addDeniedPermission(permissions[i]);
                    }
                }
            }

            if (opEntity.callback == null) return;

            if (equalsSize(opEntity.deniedPermissions, 0)) {
                opEntity.callback.onGranted(false);
            } else if (equalsSize(opEntity.grantedPermissions, 0)) {
                opEntity.callback.onDenied(opEntity.neverAskPermissions);
            } else {
                opEntity.callback.onElse(opEntity.deniedPermissions, opEntity.neverAskPermissions);
            }
        }
    }

    /**
     * 检查权限是否已经授权, 此方法有必要验证是否存在
     *
     * @param permission
     * @return true: 已经授权
     */
    static boolean checkPermission(String permission) {
        boolean result = true;
        int targetSdkVersion = 0;
        Context ctx = sContext;
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                result = ContextCompat.checkSelfPermission(ctx, permission)
                        == PackageManager.PERMISSION_GRANTED;
            } else { // 若 targetSdkVersion < 23 且运行在M版本应用有上面代码检查会一直返回 PERMISSION_GRANTED
                result = PermissionChecker.checkSelfPermission(ctx, permission)
                        == PermissionChecker.PERMISSION_GRANTED;
            }
        }
        return result;
    }

    private static void initContext(Context context) {
        if (sContext == null && context != null) {
            sContext = context.getApplicationContext();
        }
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


    /**
     * 用来存储权限相关数据
     *
     * @author a_liYa
     * @date 2016/9/18 11:08.
     */
    static class OpEntity implements Serializable {

        List<String> grantedPermissions;   // 授权权限集合
        List<String> deniedPermissions;    // 拒绝权限集合 包括：不再询问权限
        List<String> neverAskPermissions;  // 不再询问权限集合
        List<String> waitPermissions;      // 待申请权限集合

        PermissionCallback callback;

        int requestCode;

        static int count = 0;   // 用来生成 requestCode

        OpEntity(PermissionCallback callback) {
            this.callback = callback;

            requestCode = count++;
            if (count > 0x0000ffff) {
                count = 0;
            }
        }

        void addGrantedPermission(String permission) {
            if (grantedPermissions == null) grantedPermissions = new ArrayList<>();

            grantedPermissions.add(permission);
        }

        void addDeniedPermission(String permission) {
            if (deniedPermissions == null) deniedPermissions = new ArrayList<>();

            deniedPermissions.add(permission);
        }

        void addNeverAskPermission(String permission) {
            if (neverAskPermissions == null) neverAskPermissions = new ArrayList<>();

            neverAskPermissions.add(permission);
            addDeniedPermission(permission);
        }

        void addWaitPermission(String permission) {
            if (waitPermissions == null) waitPermissions = new ArrayList<>();

            waitPermissions.add(permission);
        }

        String[] getWaitPermsArray() {
            return waitPermissions.toArray(new String[waitPermissions.size()]);
        }

        public static boolean equalsSize(List list, int size) {
            return (list != null ? list.size() : 0) == size;
        }

    }

}
