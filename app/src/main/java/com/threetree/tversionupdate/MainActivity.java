package com.threetree.tversionupdate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.threetree.ttversionupdate.*;
import com.threetree.ttversionupdate.BuildConfig;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    /**
     * 兼容7.0，这个路径要跟xml目录下的路径一致
     */
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "apk" + File.separator;

    /**
     * 用版本code来作为文件名，可以在应用启动的时候检查目录并删除不需要的apk文件
     */
    public static final String APK_NAME = "temp_v" + BuildConfig.VERSION_CODE;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        removeApkFile();
        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                update();
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        VersionUpdateHelper.getInstance().stopUpdateVersion();
    }

    private void update()
    {
        VersionUpdate update = new VersionUpdate();
        update.fileDir = ROOT_PATH;
        update.fileName = APK_NAME ;
        update.iconRes = R.mipmap.ic_launcher;
        update.content = "发现新的版本，需要马上更新";
        update.type = VersionUpdate.Type.MUSTUPDATE;
        update.url = "http://guawa-v3.oss-cn-shenzhen.aliyuncs.com/common/1533195088000APK.apk";

        VersionUpdateHelper.getInstance()
                .with(this)
                .version(update)
                .setCheckLoad(true)//不重复下载
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

    }

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
}
