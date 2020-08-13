package com.aliya.permission;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

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
    static boolean requestPermissions(Context context,
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

    public static boolean startActivityForResult(Context context,
                                                 Intent intent, int requestCode,
                                                 OnActivityResultCallback callback) {
        return startActivityForResult(context, intent, requestCode, null, callback);
    }

    public static boolean startActivityForResult(Context context,
                                                 Intent intent, int requestCode, Bundle options,
                                                 OnActivityResultCallback callback) {
        if (context instanceof Activity) {
            Activity activity = ((Activity) context);
            FragmentManager manager = activity.getFragmentManager();
            InnerResultFragment resultFragment;
            Fragment fragmentByTag = manager.findFragmentByTag(FRAGMENT_TAG);
            if (fragmentByTag instanceof InnerResultFragment) {
                resultFragment = (InnerResultFragment) fragmentByTag;
            } else {
                resultFragment = new InnerResultFragment();
                manager.beginTransaction().add(resultFragment, FRAGMENT_TAG).commitAllowingStateLoss();
            }
            resultFragment.startActivityForResult(intent, requestCode, options, callback);
            return true;
        }
        return false;
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

        private List<IntentConfig> mWaitingStartActivities;
        private List<CallbackParams> mCallbackParams = new ArrayList<>(1);

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

            if (mWaitingStartActivities != null) {
                for (IntentConfig config : mWaitingStartActivities) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        startActivityForResult(config.intent, config.requestCode, config.options);
                    } else {
                        startActivityForResult(config.intent, config.requestCode);
                    }
                }
                mWaitingStartActivities = null; // onCreate 之后不再使用，置空。
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

        public void startActivityForResult(Intent intent, int requestCode, Bundle options,
                                           OnActivityResultCallback callback) {
            if (callback != null)
                mCallbackParams.add(new CallbackParams(requestCode, callback));
            if (isAdded()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    startActivityForResult(intent, requestCode, options);
                } else {
                    startActivityForResult(intent, requestCode);
                }
            } else {
                if (mWaitingStartActivities == null) {
                    mWaitingStartActivities = new ArrayList<>(1);
                }
                mWaitingStartActivities.add(new IntentConfig(intent, requestCode, options));
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            int performedIndex = -1;
            for (int i = mCallbackParams.size() - 1; i >= 0; i--) {
                CallbackParams callbackParams = mCallbackParams.get(i);
                if (callbackParams.requestCode == requestCode) {
                    callbackParams.callback.onActivityResult(requestCode, resultCode, data);
                    performedIndex = i;
                    break;
                }
            }
            if (performedIndex > -1) { // 完成之后删除 Callback
                mCallbackParams.remove(performedIndex);
            }
        }

        private static class IntentConfig {
            Intent intent;
            int requestCode;
            Bundle options;

            public IntentConfig(Intent intent, int requestCode, Bundle options) {
                this.intent = intent;
                this.requestCode = requestCode;
                this.options = options;
            }
        }

        private static class CallbackParams {
            int requestCode;
            OnActivityResultCallback callback;

            public CallbackParams(int requestCode, OnActivityResultCallback callback) {
                this.requestCode = requestCode;
                this.callback = callback;
            }
        }
    }

    public interface OnPermissionsResultCallback {
        void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
    }

    public interface OnActivityResultCallback {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }
}
