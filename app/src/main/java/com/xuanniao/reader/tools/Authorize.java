package com.xuanniao.reader.tools;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class Authorize extends Fragment {
    //MainActivity的activity对象
    public Activity context;
    @Override
    public void onStart() {
        super.onStart();
        checkReadWritePermission(context);
    }

    static Boolean isRefuse = true;
    public Boolean checkReadWritePermission(Context context) {
        boolean isGranted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
            }
            if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
            }
            Log.i("读写权限获取"," ： "+isGranted);
            if (!isGranted) {
                ActivityCompat.requestPermissions((Activity) context,new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        102);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isGranted) {// android 11 且 没有被授权
            // 先判断有没有权限
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                ActivityResultLauncher<String> mGetContent = registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        new ActivityResultCallback<Uri>() {
                            @Override
                            public void onActivityResult(Uri requestCode) {
                                // 检查是否有权限
                                if (Environment.isExternalStorageManager()) {
                                    // 授权成功
                                    isRefuse = false;
                                } else {
                                    // 授权失败
                                    isRefuse = true;
                                    Toast.makeText(context, "不授权无法使用本软件", Toast.LENGTH_SHORT).show();
                                    Intent home=new Intent(Intent.ACTION_MAIN);
                                    home.addCategory(Intent.CATEGORY_HOME);
                                    startActivity(home);
                                }
                            }
                        });
            }
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            ActivityCompat.requestPermissions(this.context,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    0);
//        }
        return isGranted;
    }

    public static Boolean checkCalenderPermission(Context context) {
        Boolean flag;
        if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED)&&(ActivityCompat.checkSelfPermission(context,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)) {
            flag = false;
        }else{
            flag = true;
        }
        return  flag;
    }

    public static Boolean checkNotificationPermission(Context context) {
        Boolean flag;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            flag = false;
        }else{
            flag = true;
        }
        return  flag;
    }

    public static Boolean checkCanDrawOverlay(Context context) {
        // 检查锁屏显示权限是否已授权
        if (!Settings.canDrawOverlays(context)) {
            // 构建一个AlertDialog，向用户解释为什么需要锁屏显示权限
//            AlertDialog.Builder builder = new AlertDialog.Builder(context);
//            builder.setTitle("权限申请");
//            builder.setMessage("锁屏显示权限允许应用在设备锁屏状态下显示一些信息，是否授权？");
//            builder.setPositiveButton("授权", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    // 启动系统设置界面，让用户授权锁屏显示权限
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                    intent.setData(Uri.parse("package:" + context.getPackageName()));
//                    context.startActivity(intent);
//                }
//            });
//            builder.setNegativeButton("取消", null);
//            builder.show();
            return false;
        } else {
            // 已授权，可以进行锁屏显示操作
            return true;
        }
    }
}
