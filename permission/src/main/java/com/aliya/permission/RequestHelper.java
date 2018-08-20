package com.aliya.permission;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.SparseArray;

/**
 * 帮助 {@link RequestPermissionFragment}  获取或添加到Activity
 *
 * @author a_liYa
 * @date 2018/8/18 09:55.
 */
final class RequestHelper {

    private static final String FRAGMENT_TAG = "request_fragment_tag";

    public static PermissionOperate getPermissionOperate(Activity activity) {
        FragmentManager manager = activity.getFragmentManager();
        Fragment fragmentByTag = manager.findFragmentByTag(FRAGMENT_TAG);
        RequestPermissionFragment requestFragment;
        if (fragmentByTag instanceof PermissionOperate) {
            requestFragment = (RequestPermissionFragment) fragmentByTag;
        } else {
            requestFragment = new RequestPermissionFragment();
            manager.beginTransaction().add(requestFragment, FRAGMENT_TAG).commitAllowingStateLoss();
        }
        return requestFragment;
    }

    /**
     * 帮助请求权限 - Fragment
     *
     * @author a_liYa
     * @date 2018/8/17 18:38.
     */
    public static class RequestPermissionFragment extends Fragment implements PermissionOperate {

        private SparseArray<String[]> mWaitingArray;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (mWaitingArray != null) {
                for (int i = 0; i < mWaitingArray.size(); i++) {
                    int key = mWaitingArray.keyAt(i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(mWaitingArray.get(key), key);
                    }
                }
                mWaitingArray = null;
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            PermissionManager.onRequestPermissionResult(requestCode, permissions, grantResults, this);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void exeRequestPermissions(@NonNull String[] permissions, int requestCode) {
            if (getHost() == null) { // 此时 Fragment not attached to Activity
                if (mWaitingArray == null) {
                    mWaitingArray = new SparseArray<>(1);
                }
                mWaitingArray.put(requestCode, permissions);
            } else {
                requestPermissions(permissions, requestCode);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean exeShouldShowRequestPermissionRationale(@NonNull String permission) {
            return shouldShowRequestPermissionRationale(permission);
        }

    }
    
}
