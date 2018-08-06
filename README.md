# tversionupdate

简单的封装了app更新操作，支持下载速度和百分比，后台跟新，锁屏显示更新进度，wifi环境判断和提醒。
页面进度弹框采用系统默认，如需要改造，可阅读代码自行改造。


## 简单使用

需要权限（自行处理动态权限申请）：
```
    <!-- 访问网络状态-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <!-- 外置存储存取权限 -->
    <permission android:name="android.permission.WRITE_MEDIA_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

代码说明：
```
	/**
     * 兼容7.0，这个路径要跟xml目录下的路径一致
     */
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "your dir" + File.separator;
	
	/**
     * 用版本code来作为文件名，可以在应用启动的时候检查目录并删除不需要的apk文件
	 * 不要使用后缀
     */
    public static final String APK_NAME = "temp_v" + BuildConfig.VERSION_CODE;

	//更新配置
	VersionUpdate update = new VersionUpdate();
        update.fileDir = ROOT_PATH;
        update.fileName = APK_NAME ;
        update.iconRes = R.mipmap.ic_launcher;//锁屏页面，通知栏显示的图标，一般使用app应用图标
        update.content = "发现新的版本，需要马上更新";
        update.type = VersionUpdate.Type.MUSTUPDATE;//强更还是非强更
        update.url = "http://guawa-v3.oss-cn-shenzhen.aliyuncs.com/common/1533195088000APK.apk";
	
	//开始更新
	VersionUpdateHelper.getInstance()
                .with(this)//context
                .version(update)
				//是否检查重复下载，注意开启此功能，会根据APK_NAME来检查，如果APK_NAME命名是写死的，则更新会失效，建议采用版本code来命名
                .setCheckLoad(true)
				//下载结果监听
                .resultListener(new VersionUpdateHelper.OnUpdateResultListener() {
                    @Override
                    public void onSuccess(String filePath)
                    {
                        File file = new File(filePath);
                        if(file.exists())
                        {
                            installApk(file,MainActivity.this);
                        }
                    }

                    @Override
                    public void onError(int code, String message)
                    {
                        Toast.makeText(MainActivity.this,"error:" + message,Toast.LENGTH_SHORT).show();
                    }
                })
                .cancelListener(new VersionUpdateHelper.OnMustUpdateCancelListener() {
                    @Override
                    public void onCancel()
                    {
                        //取消强制更新，退出应用
                        finish();
                    }
                })
                .startUpdateVersion();
```

## 版本更新逻辑
apk版本在哪里检查更新，强制更新还是非强制更新等，这些跟后端的设计有很大的关系。
有的在app启动的时候去检查更新，有的每个页面都检查更新，这里我采用如下逻辑：
1.app启动时候（或者首页），自动检测一次是否更新
2.每次请求接口都带上版本code，后台比对后，返回是否必须强更（非强更不做此逻辑），根据后台返回的最新版url下载

## apk安装逻辑
采用外存储来保存apk文件，对于安装完成后是否会删除apk文件，不同机型不一致，
所以我们要考虑安装完成后删除文件，否则占用用户的存储空间，使用如下逻辑：
将下载完成的新版本apk根据旧版本code来标识命名，当app启动的时候检查当前app的版本code，
并将apk的下载目录下的不等于该code的文件删除。两种情况：
1.app最新版本，则说明安装成功，此时code比旧版本的code大，删除下载目录下的apk文件
2.app安装未成功，此时code和旧版本code一致，不删除，如果apk文件下载完成，则安装，未完成则继续下载

## 安装APK

解决在Android7.0之后的版本出现 android.os.FileUriExposedException 的问题：

第一步：在AndroidManifest.xml清单文件中注册provider

```
<provider
    android:name="android.support.v4.content.FileProvider"
    android:authorities="com.threetree.ttversionupdate.installapk"
    android:grantUriPermissions="true"
    android:exported="false">
    <!--元数据-->
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_path" />
</provider>
```

**注意**：
1. exported:必须为false 
2. grantUriPermissions:true，表示授予 URI 临时访问权限。 
3. authorities 组件标识，都以包名开头,避免和其它应用发生冲突。

第二步: 指定共享文件的目录,需要在res文件夹中新建xml目录,并且创建file_path

```
<resources xmlns:android="http://schemas.android.com/apk/res/android">
    <paths>
        <external-path path="" name="apk"/>
    </paths>
</resources>
```

上述代码中path=”“，是有特殊意义的，它表示根目录，name是子目录
也就是说你可以向其它的应用共享根目录及其子目录下任何一个文件了

第三步：使用FileProvider

```	
	//安装apk
    private void installApk(File file, Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(Build.VERSION.SDK_INT>=24) { //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            Uri apkUri =
                    FileProvider.getUriForFile(context, "com.threetree.ttversionupdate.installapk", file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        }else{
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }
```

## 删除apk文件（根据个人需求）

```
	/**
     * 删除多余的apk文件
     */
    private void removeApkFile()
    {
        File file = new File(ROOT_PATH);
        if(!file.exists())
        {
            return;
        }
        if(!file.isDirectory())
        {
            return;
        }
        File[] files = file.listFiles();
        if(files == null || files.length <= 0)
        {
            return;
        }
        for (File f:files)
        {
            try
            {
                String name = getFileNameNoEx(f.getName());
                if(!APK_NAME.equals(name))
                {
                    f.delete();
                }
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /*
    * Java文件操作 获取不带扩展名的文件名
    *
    */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }
```

