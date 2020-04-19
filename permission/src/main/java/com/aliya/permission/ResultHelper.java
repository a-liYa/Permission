package com.aliya.permission;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.SparseArray;

/**
 * 通过设置 Callback 实现 Activity#onRequestPermissionsResult(int, String[], String[]) 回调
 *
 * @author a_liYa
 * @date 2018/8/18 09:55.
 */
public final class ResultHelper {

    private static final String FRAGMENT_TAG = "result_fragment_tag";

    public static Activity getActivityByContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        if (PermissionManager.sDebuggable) {
            throw new IllegalArgumentException(context + " should be include activity");
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean requestPermissions(Context context,
                                             String[] permissions, int requestCode,
                                             OnPermissionsResultCallback callback) {
        final Activity activity = getActivityByContext(context);
        if (activity == null) return false;

        FragmentManager manager = activity.getFragmentManager();
        Fragment fragmentByTag = manager.findFragmentByTag(FRAGMENT_TAG);
        InnerResultFragment requestFragment;
        if (fragmentByTag instanceof InnerResultFragment) {
            requestFragment = (InnerResultFragment) fragmentByTag;
        } else {
            requestFragment = new InnerResultFragment();
            manager.beginTransaction().add(requestFragment, FRAGMENT_TAG).commitAllowingStateLoss();
        }
        requestFragment.requestPermissionsForResult(permissions, requestCode, callback);
        return true;
    }

    /**
     * 通过 Fragment 协助实现
     *
     * @author a_liYa
     * @date 2018/8/17 18:38.
     */
    public static class InnerResultFragment extends Fragment {

        private SparseArray<String[]> mWaitingRequestPermissions;
        private SparseArray<OnPermissionsResultCallback> mPermissionsResultCallbacks;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (mWaitingRequestPermissions != null) {
                for (int i = 0; i < mWaitingRequestPermissions.size(); i++) {
                    int key = mWaitingRequestPermissions.keyAt(i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(mWaitingRequestPermissions.get(key), key);
                    }
                }
                mWaitingRequestPermissions = null;
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (mPermissionsResultCallbacks != null) {
                OnPermissionsResultCallback callback = mPermissionsResultCallbacks.get(requestCode);
                if (callback != null) {
                    callback.onPermissionsResult(requestCode, permissions, grantResults);
                    mPermissionsResultCallbacks.remove(requestCode); // 回调之后删除 Callback
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void requestPermissionsForResult(@NonNull String[] permissions, int requestCode,
                                                OnPermissionsResultCallback callback) {
            if (mPermissionsResultCallbacks == null)
                mPermissionsResultCallbacks = new SparseArray<>(1);
            if (callback != null)
                mPermissionsResultCallbacks.put(requestCode, callback);
            if (isAdded()) {
                requestPermissions(permissions, requestCode);
            } else { // 此时 Fragment not attached to Activity
                if (mWaitingRequestPermissions == null) {
                    mWaitingRequestPermissions = new SparseArray<>(1);
                }
                mWaitingRequestPermissions.put(requestCode, permissions);
            }
        }
    }

    public interface OnPermissionsResultCallback {
        void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
    }

}
