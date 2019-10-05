package com.aliya.permission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用来存储权限相关数据
 *
 * @author a_liYa
 * @date 2016/9/18 11:08.
 */
class OpEntity implements Serializable {

    private static int sCode = 0;   // 用来生成 requestCode

    List<String> grantedPermissions;   // 授权权限集合
    List<String> deniedPermissions;    // 拒绝权限集合 包括：不再询问权限
    List<String> neverAskPermissions;  // 不再询问权限集合
    List<String> waitPermissions;      // 待申请权限集合

    PermissionCallback callback;

    int requestCode;

    OpEntity(PermissionCallback callback) {
        this.callback = callback;
        requestCode = obtainRequestCode();
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
        if (waitPermissions != null) {
            return waitPermissions.toArray(new String[waitPermissions.size()]);
        }
        return null;
    }

    /**
     * 生成 request code
     *
     * @return request code
     */
    private static int obtainRequestCode() {
        // see android.support.v4.app.BaseFragmentActivityApi14#checkForValidRequestCode(int)
        if ((sCode & 0xffff0000) != 0) sCode = 0;

        return sCode++;
    }
}
