package com.aliya.permission.simple;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.aliya.permission.Permission;
import com.aliya.permission.PermissionCallback;
import com.aliya.permission.PermissionManager;
import com.aliya.permission.simple.utils.T;

import java.util.List;

public class PermissionSimpleActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_simple);

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
            public void onGranted(boolean isAlreadyDef) {
                T.showShort(getBaseContext(), "通过： " + (isAlreadyDef ? "二次" : "首次"));
            }

            @Override
            public void onDenied(List<String> neverAskPermissions) {
                T.showShort(getBaseContext(), "拒绝： " + neverAskPermissions);
            }

            @Override
            public void onElse(List<String> deniedPermissions, List<String> neverAskPermissions) {

            }

        }, Permission.CAMERA);
    }

    private void requestStorage() {
        PermissionManager.request(this, new PermissionCallback() {

            @Override
            public void onGranted(boolean isAlreadyDef) {
                T.showShort(getBaseContext(), "通过： " + (isAlreadyDef ? "二次" : "首次"));
            }

            @Override
            public void onDenied(List<String> neverAskPermissions) {
                T.showShort(getBaseContext(), "拒绝： " + neverAskPermissions);
            }

            @Override
            public void onElse(List<String> deniedPermissions, List<String> neverAskPermissions) {

            }

        }, Permission.STORAGE_READE, Permission.STORAGE_WRITE);
    }

}
