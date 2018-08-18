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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 动态权限申请工具类
 *
 * @author a_liYa
 * @date 16/7/21.
 */
public class PermissionManager {
    /**
     * 被拒绝权限（不再询问）
     */
    private static final String PERMISSION_NEVER_ASK = "permission_never_ask_sets";

    private volatile static PermissionManager mInstance;

    private static Context sContext;

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    public static PermissionManager get() {
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
    private final Set<String> mManifestPermissions;

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
    public boolean request(Activity activity,
                           PermissionCallback
                                   callback, Permission... permissions) {
        if (permissions == null) // 没有申请的权限 return true
            return true;

        if (activity == null || callback == null) {
            return false;
        }

        OpEntity opEntity = new OpEntity(callback);
        // 6.0版本以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Set<String> neverAskPerms;
            for (Permission permission : permissions) { // 权限分类：已授权、已拒绝（不再询问）、待申请

                // 检查申请的权限是否在 AndroidManifest.xml 中
                if (mManifestPermissions.contains(permission.getPermission())) {
                    //判断权限是否被授予
                    if (checkPermission(permission.getPermission())) {
                        opEntity.addGrantedPerm(permission.getPermission());
                    } else {
                        neverAskPerms = SPHelper.get(sContext)
                                .get(PERMISSION_NEVER_ASK, Collections.EMPTY_SET);
                        if (neverAskPerms.contains(permission.getPermission())) {
                            //  没有被用户禁止弹窗提示
                            opEntity.addNeverAskPerms(permission.getPermission());
                        } else {
                            opEntity.addWaitPerms(permission.getPermission());
                        }
                    }
                } else {
                    opEntity.addNeverAskPerms(permission.getPermission());
                }
            }

            // 处理分类权限
            if (opEntity.getGrantedPerms().size() == permissions.length) {
                callback.onGranted(true);
                return true;
            } else {
                if (opEntity.getWaitPerms().isEmpty()) {
                    if (opEntity.getGrantedPerms().isEmpty()) { // 全部是已拒绝（不再询问）
                        callback.onDenied(opEntity.getNeverAskPerms());
                    } else { // 其他情况：部分拒绝、部分已授权
                        callback.onElse(opEntity.getDeniedPerms(), opEntity.getNeverAskPerms());
                    }
                } else {
                    mRequestCaches.put(opEntity.getRequestCode(), opEntity);
                    RequestHelper.getPermissionOperate(activity)
                            .exeRequestPermissions(
                                    opEntity.getWaitPermsArray(), opEntity.getRequestCode());
                    opEntity.getWaitPerms().clear(); // 清空待申请权限
                }
            }
        } else {
            callback.onGranted(true);
            return true;
        }

        return false;
    }

    /**
     * 权限申请结果处理
     *
     * @param requestCode  请求码
     * @param permissions  申请权限集合 {@link Permission}
     * @param grantResults 申请结果集合
     */
    public static void onRequestPermissionsResult(int requestCode, String[]
            permissions, int[] grantResults, PermissionOperate showRationale) {

        OpEntity opEntity = get().mRequestCaches.get(requestCode);
        if (opEntity != null) {
            get().mRequestCaches.remove(requestCode);

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {  // 权限被授予
                    opEntity.addGrantedPerm(permissions[i]);
                } else {  // 权限被拒绝 1、拒绝 2、拒绝并不再询问
                    if (!showRationale.exeShouldShowRequestPermissionRationale((permissions[i]))) {
                        SPHelper spHelper = SPHelper.get(sContext);
                        Set<String> neverAskPerms = spHelper.get(PERMISSION_NEVER_ASK,
                                new HashSet<String>());
                        neverAskPerms.add(permissions[i]);
                        spHelper.put(PERMISSION_NEVER_ASK, neverAskPerms);
                        opEntity.addNeverAskPerms(permissions[i]);
                    } else {
                        opEntity.addDeniedPerm(permissions[i]);
                    }
                }
            }

            PermissionCallback callBack = opEntity.getCallBack();
            if (callBack == null) return;

            if (opEntity.getDeniedPerms().isEmpty()) {
                callBack.onGranted(false);
            } else if (opEntity.getGrantedPerms().isEmpty()) {
                callBack.onDenied(opEntity.getNeverAskPerms());
            } else {
                callBack.onElse(opEntity.getDeniedPerms(), opEntity.getNeverAskPerms());
            }
        }
    }

    /**
     * 检查权限是否已经授权, 此方法有必要验证是否存在
     *
     * @param permission
     * @return true: 已经授权
     */
    public static boolean checkPermission(String permission) {
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


    /**
     * 获取Manifest静态注册的权限
     */
    private synchronized Set<String> getManifestPermissions() {

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
            if (permissions != null) {
                manifestPermissions = new HashSet<>(permissions.length);
                for (String perm : permissions) {
                    manifestPermissions.add(perm);
                }
            }
        }

        if (manifestPermissions == null) manifestPermissions = new HashSet<>(0);

        return manifestPermissions;
    }

    /**
     * 用来存储权限相关数据
     *
     * @author a_liYa
     * @date 2016/9/18 11:08.
     */
    private static class OpEntity implements Serializable {
        /**
         * 授权权限集合
         */
        private List<String> mGrantedPerms;
        /**
         * 拒绝权限集合 包括：不再询问权限
         */
        private List<String> mDeniedPerms;
        /**
         * 不再询问权限集合
         */
        private List<String> mNeverAskPerms;
        /**
         * 待申请权限集合
         */
        private List<String> mWaitPerms;

        private PermissionCallback mCallBack;

        private int requestCode;

        private static int count = 0;

        public OpEntity(PermissionCallback callBack) {
            mCallBack = callBack;
            mGrantedPerms = new ArrayList<>();
            mDeniedPerms = new ArrayList<>();
            mNeverAskPerms = new ArrayList<>();
            mWaitPerms = new ArrayList<>();
            requestCode = count++;
            if (count > 0x0000ffff) {
                count = 0;
            }
        }

        public PermissionCallback getCallBack() {
            return mCallBack;
        }

        public void setCallBack(PermissionCallback callBack) {
            mCallBack = callBack;
        }

        public List<String> getGrantedPerms() {
            return mGrantedPerms;
        }

        public void setGrantedPerms(List<String> grantedPerms) {
            mGrantedPerms = grantedPerms;
        }

        public void addGrantedPerm(String grantedPerm) {
            mGrantedPerms.add(grantedPerm);
        }

        public List<String> getDeniedPerms() {
            return mDeniedPerms;
        }

        public void addDeniedPerm(String deniedPerm) {
            mDeniedPerms.add(deniedPerm);
        }

        public void setDeniedPerms(List<String> deniedPerms) {
            mDeniedPerms = deniedPerms;
        }

        public List<String> getNeverAskPerms() {
            return mNeverAskPerms;
        }

        public void addNeverAskPerms(String neverAskPerm) {
            mNeverAskPerms.add(neverAskPerm);
            mDeniedPerms.add(neverAskPerm);
        }

        public List<String> getWaitPerms() {
            return mWaitPerms;
        }

        public void addWaitPerms(String requestPerm) {
            mWaitPerms.add(requestPerm);
        }

        public String[] getWaitPermsArray() {
            return mWaitPerms.toArray(new String[mWaitPerms.size()]);
        }

        public int getRequestCode() {
            return requestCode;
        }
    }

}
