# Permission
Android 权限申请工具类，一行代码即可申请权限

```
PermissionManager.request(activity, new PermissionCallback() {

    /**
     * 全部授予
     * @param isAlreadyDef 申请之前已全部默认授权
     */
    @Override
    public void onGranted(boolean isAlreadyDef) {
    }

    /**
     * 全部拒绝 包括不再询问权限
     * @param neverAskPermissions 被拒绝(不再询问)权限集合
     */
    @Override
    public void onDenied(@Nullable List<String> neverAskPermissions) {
    }

    /**
     * 其他情况
     * @param deniedPermissions   被拒绝权限集合(包括不再询问)
     * @param neverAskPermissions 被拒绝(不再询问)权限集合
     */
    @Override
    public void onElse(@NonNull List<String> deniedPermissions, @Nullable List<String> neverAskPermissions) {
    }

}, Permission.CAMERA);
```

## 依赖
```
dependencies {
    implementation 'com.aliya:permission:0.0.1'
}
```

## 请求权限 - 重载方法

```

boolean request(Context context, PermissionCallback callback, String... permissions);       // (1)

boolean request(Activity activity, PermissionCallback callback, String... permissions);     // (2)

boolean request(Context context, PermissionCallback callback, Permission... permissions);   // (3)

boolean request(Activity activity, PermissionCallback callback, Permission... permissions); // (4)

```

注意：
```
所有危险权限均已在枚举类Permission中声明，危险权限建议使用方法(3)(4)申请，普通权限因没有在Permission中声明，所以必须使用方法(1)(2)申请；
```

## 申请单个权限

callback 可实现自AbsPermissionCallback，因为不存在 onElse() 情况；

```
new AbsPermissionCallback() {

    @Override
    public void onGranted(boolean isAlreadyDef) {
    }

    @Override
    public void onDenied(List<String> neverAskPermissions) {
    }
}
```

## Callback 详解

1. onGranted(boolean isAlreadyDef)

    全部授权（包括申请多个权限），isAlreadyDef == true，表示申请之前已经被授权；授权权限集合即为请求权限的集合。


2. onDenied(@Nullable List<String> neverAskPermissions)

    全部拒绝（包括不再询问），neverAskPermissions **可能为null**，表示勾选不再询问的权限集合；拒绝权限集合（包括不再询问）即为请求权限的集合。


3. onElse(@NonNull List<String> deniedPermissions, @Nullable List<String> neverAskPermissions)

    部分授权，部分拒绝，单权限申请不存在此情况；deniedPermissions 被拒绝权限的集合（包括不再询问）；授权权限集合 = 请求权限集合 - 拒绝权限集合；


## 其他问题

### 1. 什么时候需要手动进入应用权限设置页 ？

同时满足以下两个条件：
> 1、请求权限之前判断: `shouldShowRequestPermissionRationale() == false`；

> 2、请求权限拒绝之后判断: `shouldShowRequestPermissionRationale() == false`;

示例：

```
PermissionManager.startSettingIntent();
```


### 2. Android 8.0 安装 Apk 所需权限

此权限 Manifest.permission.REQUEST_INSTALL_PACKAGES 是普通权限，added in API level 23；

manifest配置：

`<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`

 **当主动申请该权限时，永远返回拒绝（这里比较特殊）** 可通过 context.getPackageManager().canRequestPackageInstalls() 来判断使用具备该权限；