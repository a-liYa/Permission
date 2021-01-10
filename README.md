# Permission
Android 权限申请工具类，一行代码即可申请权限

**支持 Androidx**

## 依赖
[![License](https://img.shields.io/badge/License-Apache%202.0-337ab7.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/a-liya/maven/permission/images/download.svg)](https://bintray.com/a-liya/maven/permission/_latestVersion)
[![MinSdk](https://img.shields.io/badge/%20MinSdk%20-%2016%20-f0ad4e.svg)](https://android-arsenal.com/api?level=12)

```
dependencies {
    implementation 'com.aliya:permission:1.0.0'
}
```

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
     * 拒绝(至少一个权限拒绝)
     *
     * @param deniedPermissions   被拒绝权限集合(包括不再询问)
     * @param neverAskPermissions 被拒绝不再询问权限集合
     */
    void onDenied(@NonNull List<String> deniedPermissions, @Nullable List<String> neverAskPermissions) {
    }

}, Permission.CAMERA);
```

> 优势

对比 Google 官方 [easypermisssions](https://github.com/googlesamples/easypermissions) 精简下面方法的实现
```
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // Forward results to EasyPermissions
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
}
```
## Android O(8.0) 行为变更
针对Android 8.0 开发的应用（targetSdk >= 26）  

1. 在 Android 8.0 之前，如果应用在运行时请求权限并且被授予该权限，系统会错误地将属于同一权限组并且在清单中注册的其他权限也一起授予应用。  
2. 对于针对 Android 8.0 的应用，此行为已被纠正。系统只会授予应用明确请求的权限。然而，一旦用户为应用授予某个权限，则所有后续对该权限组中权限的请求都将被自动批准。

建议：**同组权限一起申请**。当我们申请权限时。申请同组的多个权限时，也只会弹出一次申请框。所以不如一起申请。

## 请求权限 - 重载方法

```

boolean request(Context context, PermissionCallback callback, String... permissions);       // (1)

boolean request(Activity activity, PermissionCallback callback, String... permissions);     // (2)

boolean request(Context context, PermissionCallback callback, Permission... permissions);   // (3)

boolean request(Activity activity, PermissionCallback callback, Permission... permissions); // (4)

```

注意：
```
所有危险权限均已在枚举类Permission中声明，危险权限建议使用方法(3)(4)申请，普通权限因没有在Permission中声明，所以只能使用方法(1)(2)申请；
```

## Callback 详解

### 1. onGranted(`boolean isAlreadyDef`)

    全部授权（包括申请多个权限），isAlreadyDef == true，表示申请之前已经被授权；授权权限集合即为请求权限的集合。

### 2. onDenied(`@NonNull List<String> deniedPermissions, @Nullable List<String> neverAskPermissions`)

    部分授权，部分拒绝，单权限申请不存在此情况；deniedPermissions 被拒绝权限的集合（包括不再询问）；授权权限集合 = 请求权限集合 - 拒绝权限集合；


## 其他问题

### 1. 什么时候需要手动进入应用权限设置页 ？

同时满足以下两个条件：
> 1、请求权限之前判断: `shouldShowRequestPermissionRationale() == false`；
>
> 2、请求权限拒绝之后判断: `shouldShowRequestPermissionRationale() == false`;

示例：

```
final boolean before = PermissionManager.shouldShowRequestPermissionRationale(context, Permission.LOCATION_COARSE.getPermission());
PermissionManager.request(this, new AbsPermissionCallback() {

    @Override
    public void onGranted(boolean isAlready) {}

    @Override
    public void onDenied() {
        boolean after = PermissionManager.shouldShowRequestPermissionRationale(NeverAskActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (!before && !after) {
            // 此处应Dialog提醒
            startActivityForResult(PermissionManager.getSettingIntent(context), PERMISSION_REQUEST_CODE);
        }
     }

}, Permission.LOCATION_COARSE);


@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PERMISSION_REQUEST_CODE) {
        if (PermissionManager.checkPermission(getApplication(), Permission.LOCATION_COARSE.getPermission())) {
            Log.e("TAG", "手动授权成功");
        } else {
            Log.e("TAG", "手动授权失败");
        }
    }
}
```


### 2. Android 8.0 安装 Apk 所需权限

此权限 Manifest.permission.REQUEST_INSTALL_PACKAGES 是普通权限，added in API level 23；

manifest配置：

`<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`

 **当主动申请该权限时，永远返回拒绝（这里比较特殊）；** 可通过 context.getPackageManager().canRequestPackageInstalls() 来判断是否具备该权限；


### 3. Android 9.0 前台服务权限

针对 Android P 或更高平台开发的应用必须请求 Manifest.permission.FOREGROUND_SERVICE 权限才能使用前台服务。 Manifest.permission.FOREGROUND_SERVICE 属于普通级别请求，因此提出请求后，系统会自动授予。


### 4. 待优化

优化 ApplicationContext 的获取

减少 context 参数
eg：`PermissionManager#checkPermission(context, permission); PermissionManager#getSettingIntent(context);`

参考 http://kaedea.com/2017/04/09/android/global-accessing-context/#more
