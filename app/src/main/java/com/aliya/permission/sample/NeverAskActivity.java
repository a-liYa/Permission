package com.aliya.permission.sample;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.aliya.permission.Permission;
import com.aliya.permission.PermissionManager;
import com.aliya.permission.ResultHelper;
import com.aliya.permission.SettingIntents;
import com.aliya.permission.abs.AbsPermissionCallback;
import com.aliya.permission.sample.utils.T;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 不再询问 - 测试页
 *
 * @author a_liYa
 * @date 2018/8/28 上午10:43.
 */
public class NeverAskActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_never_ask);

        findViewById(R.id.tv_location).setOnClickListener(this);
        findViewById(R.id.tv_overlay).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_location:
                requestLocationPermission();
                break;
            case R.id.tv_overlay:
                openOverlayPermission();
                break;
        }
    }

    private void openOverlayPermission() {
        final String permission = Manifest.permission.SYSTEM_ALERT_WINDOW;
        if (!PermissionManager.checkPermission(this, permission)) {
            ResultHelper.startActivityForResult(this,
                    SettingIntents.getOverlayPermissionIntent(this),
                    101, new ResultHelper.OnActivityResultCallback() {
                        @Override
                        public void onActivityResult(int requestCode, int resultCode, Intent data) {
                            boolean result =
                                    PermissionManager.checkPermission(getBaseContext(), permission);

                            T.showShort(NeverAskActivity.this, result ? "通过" : "拒绝");
                        }
                    });
        } else {
            T.showShort(NeverAskActivity.this,  "显示在上层权限已允许");
        }
    }

    private void requestLocationPermission() {

        final boolean before = PermissionManager.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        PermissionManager.request(this, new AbsPermissionCallback() {

            @Override
            public void onGranted(boolean isAlready) {
                T.showShort(NeverAskActivity.this, "权限允许");
            }

            @Override
            public void onDenied(List<String> deniedPermissions, List<String> neverAskPermissions) {
                boolean after = PermissionManager.shouldShowRequestPermissionRationale
                        (NeverAskActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (!before && !after) {
                    // 此处应该Dialog提醒
                    // 此处未使用 ResultHelper, 防止权限关闭引起页面重启，回调失效
                    startActivityForResult(SettingIntents.getAppDetailsIntent(getApplication()),
                            100);
                }
            }

        }, Permission.LOCATION_COARSE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (PermissionManager.checkPermission(getApplication(),
                    Permission.LOCATION_COARSE.getPermission())) {
                T.showShort(this, "手动授权成功");
            } else {
                T.showShort(this, "手动授权失败");
            }
        }
    }
}
