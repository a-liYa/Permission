package com.aliya.permission.simple;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.aliya.permission.Permission;
import com.aliya.permission.PermissionCallback;
import com.aliya.permission.PermissionManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionManager.get().request(this, new PermissionCallback() {
            @Override
            public void onGranted(boolean isAlreadyDef) {

            }

            @Override
            public void onDenied(List<String> neverAskPerms) {

            }

            @Override
            public void onElse(List<String> deniedPerms, List<String> neverAskPerms) {

            }

        }, Permission.STORAGE_READE, Permission.STORAGE_WRITE);

    }

    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        return super.shouldShowRequestPermissionRationale(permission);
    }

}
