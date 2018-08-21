# Permission
Android 权限申请 工具类，一行代码即可申请权限

### 什么时候需要手动进入应用权限设置页 ？

同时满足以下两个条件：
> 1、请求权限之前判断: `shouldShowRequestPermissionRationale() == false`；

> 2、请求权限拒绝之后判断: `shouldShowRequestPermissionRationale() == false`;