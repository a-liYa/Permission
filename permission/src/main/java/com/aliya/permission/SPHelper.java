package com.aliya.permission;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.lang.ref.WeakReference;
import java.util.Set;


/**
 * 轻量级数据存储助手
 *
 * @author a_liYa
 * @date 2016-3-28 下午9:06:16
 */
public class SPHelper {

    /**
     * 被拒绝权限（不再询问）
     */
    private static final String PERMISSION_NEVER_ASK = "permission_never_ask_sets";

    private static WeakReference<SPHelper> sSPHelperWeak;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private SPHelper(Context context) {
        sharedPreferences = context
                .getSharedPreferences(PERMISSION_NEVER_ASK, Activity.MODE_PRIVATE);
    }

    public static SPHelper get(Context context) {
        SPHelper spHelper;
        if (sSPHelperWeak == null || (spHelper = sSPHelperWeak.get()) == null) {
            sSPHelperWeak = new WeakReference<>(spHelper = new SPHelper(context));
        }
        return spHelper;
    }

    public SharedPreferences.Editor getSharedPreferencesEditor() {
        return sharedPreferences.edit();
    }

    public boolean hasKey(String key) {
        if (sharedPreferences != null) {
            return sharedPreferences.contains(key);
        }
        return false;
    }

    /**
     * 保存数据 需要手动commit
     *
     * @param key   关键字
     * @param value 值
     * @param <T>   泛型可为：int、float、boolean、String、long、Set<String>
     */
    public <T> SPHelper put(String key, T value) {
        if (editor == null) {
            editor = getSharedPreferencesEditor();
        }
        if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Set) {
            editor.putStringSet(key, (Set<String>) value);
        }

        return this;
    }

    /**
     * 提交 异步
     */
    public SPHelper commit() {
        commit(true);
        return this;
    }

    /**
     * 提交
     *
     * @param isAsync true 表示异步， false 表示同步
     * @return true 成功， false 失败， 异步永远返回false
     */
    public boolean commit(boolean isAsync) {
        boolean result = false;
        if (editor != null) {
            if (isAsync) {
                editor.apply();
            } else {
                result = editor.commit();
            }
            editor = null;
        }
        return result;
    }

    public String get(String key, String defValue) {
        return sharedPreferences.getString(key, defValue);
    }

    public long get(String key, long defValue) {
        return sharedPreferences.getLong(key, defValue);
    }

    public int get(String key, int defValue) {
        return sharedPreferences.getInt(key, defValue);
    }

    public float get(String key, float defValue) {
        return sharedPreferences.getFloat(key, defValue);
    }

    public boolean get(String key, boolean defValue) {
        return sharedPreferences.getBoolean(key, defValue);
    }

    public Set<String> get(String key, Set<String> defValue) {
        return sharedPreferences.getStringSet(key, defValue);
    }

    /**
     * 清空数据 Key
     */
    public boolean clear() {
        if (editor == null) {
            editor = getSharedPreferencesEditor();
        }
        return editor.clear().commit();
    }

}
