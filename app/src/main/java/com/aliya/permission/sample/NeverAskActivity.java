package com.aliya.permission.sample;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.aliya.permission.Permission;
import com.aliya.permission.PermissionManager;
import com.aliya.permission.abs.AbsPermissionCallback;
import com.aliya.permission.sample.utils.T;

import java.util.List;

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

        Log.e("TAG", "onCreate: " + getClass().getSimpleName());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_location:
                requestLocationPermission();
                break;
        }
    }

    private void requestLocationPermission() {

        final boolean before = PermissionManager.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        PermissionManager.request(this, new AbsPermissionCallback() {

            @Override
            public void onGranted(boolean isAlready) {

            }

            @Override
            public void onDenied(List<String> deniedPermissions, List<String> neverAskPermissions) {
                boolean after = PermissionManager.shouldShowRequestPermissionRationale
                        (NeverAskActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (!before && !after) {
                    // 此处应该Dialog提醒
                    startActivityForResult(PermissionManager.getSettingIntent(getApplication()), 100);
                }
            }

        }, Permission.LOCATION_COARSE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("TAG", "onActivityResult: " + getClass().getSimpleName() + " - " + requestCode);
        if (requestCode == 100) {
            if (PermissionManager.checkPermission(getApplication(), Permission.LOCATION_COARSE.getPermission())) {
                T.showShort(this, "手动授权成功");
            } else {
                T.showShort(this, "手动授权失败");
            }
        }
    }
}
