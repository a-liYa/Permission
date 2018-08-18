package com.aliya.permission.simple;

import android.app.Application;

import com.aliya.permission.PermissionManager;

/**
 * App
 *
 * @author a_liYa
 * @date 2018/8/18 10:22.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PermissionManager.init(this);
    }

}
