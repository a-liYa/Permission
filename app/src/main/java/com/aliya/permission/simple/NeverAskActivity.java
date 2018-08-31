package com.aliya.permission.simple;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.aliya.permission.Permission;
import com.aliya.permission.PermissionManager;
import com.aliya.permission.abs.AbsPermissionCallback;

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
            public void onDenied(List<String> neverAskPermissions) {
                boolean after = PermissionManager.shouldShowRequestPermissionRationale
                        (NeverAskActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (!before && !after) {
                    // 此处应该Dialog提醒
                    PermissionManager.startSettingIntent();
                }
            }

        }, Permission.LOCATION_COARSE);
    }

}
