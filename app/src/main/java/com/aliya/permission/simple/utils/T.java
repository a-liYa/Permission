package com.aliya.permission.simple.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Toast封装
 *
 * @author a_liYa
 * @date 2018/8/18 17:09.
 */
public class T {

    public static void showShort(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}
