package com.aliya.permission.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.aliya.permission.Permission;
import com.aliya.permission.PermissionCallback;
import com.aliya.permission.PermissionManager;
import com.aliya.permission.sample.utils.T;

import java.util.List;

public class PermissionSampleActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_sample);

        findViewById(R.id.tv_request_storage).setOnClickListener(this);
        findViewById(R.id.tv_request_camera).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_request_storage:
                requestStorage();
                break;
            case R.id.tv_request_camera:
                requestCamera();
                break;
        }
    }

    private void requestCamera() {
        PermissionManager.request(this, new PermissionCallback() {

            @Override
            public void onGranted(boolean isAlready) {
                T.showShort(getBaseContext(), "通过： " + (isAlready ? "二次" : "首次"));
            }

            @Override
            public void onDenied(List<String> deniedPermissions, List<String> neverAskPermissions) {
                T.showShort(getBaseContext(), "拒绝： " + neverAskPermissions);
            }

        }, Permission.CAMERA);
    }

    private void requestStorage() {
        PermissionManager.request(this, new PermissionCallback() {

            @Override
            public void onGranted(boolean isAlready) {
                T.showShort(getBaseContext(), "通过： " + (isAlready ? "二次" : "首次"));
            }

            @Override
            public void onDenied(List<String> deniedPermissions, List<String> neverAskPermissions) {
                T.showShort(getBaseContext(), "拒绝： " + neverAskPermissions);
            }

        }, Permission.STORAGE_READ, Permission.STORAGE_WRITE);
    }

}
