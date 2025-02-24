package com.xuanniao.reader.tools;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import org.json.JSONObject;

import java.io.*;
import java.util.Objects;

public class Export extends Service {
    Context mContext = Export.this;
    String TAG = "Export";
    private static final String documentsPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand方法被调用!");
        int type = intent.getIntExtra("dbType", 1);
        toDB(documentsPath, type);
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean toDB(String newPathName, int type) {
        String dbName = (type == 1) ?  Constants.DB_PLATFORM : Constants.DB_BOOK;
        try {
            String privatePath = getApplicationContext().getDatabasePath(dbName).toString();
            Log.d("数据库路径", privatePath);
            File privateFile = new File(privatePath);
            if (!privateFile.exists()) {
                Log.e("copyFile", "copyFile:  privateFile not exist.");
                return false;
            } else if (!privateFile.isFile()) {
                Log.e("copyFile", "copyFile:  privateFile not file.");
                return false;
            } else if (!privateFile.canRead()) {
                Log.e("copyFile", "copyFile:  privateFile cannot read.");
                return false;
            }
            FileInputStream fis = new FileInputStream(privateFile);
            FileOutputStream fos = new FileOutputStream(newPathName + dbName + ".db");
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fis.read(buffer))) {
                fos.write(buffer, 0, byteRead);
            }
            fis.close();
            fos.flush();
            fos.close();
            Toast.makeText(mContext, "导出完毕！", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void toCSV(Cursor c, String fileName) {
        int rowCount = 0;
        int colCount = 0;
        FileWriter fw;
        BufferedWriter bfw;
        File sdCardDir = Environment.getExternalStorageDirectory();
        File saveFile = new File(sdCardDir, fileName);
        try {
            rowCount = c.getCount();
            colCount = c.getColumnCount();
            fw = new FileWriter(saveFile);
            bfw = new BufferedWriter(fw);
            if (rowCount > 0) {
                c.moveToFirst();
                // 写入表头
                for (int i = 0; i < colCount; i++) {
                    if (i != colCount - 1)
                        bfw.write(c.getColumnName(i) + ',');
                    else
                        bfw.write(c.getColumnName(i));
                }
                // 写好表头后换行
                bfw.newLine();
                // 写入数据
                for (int i = 0; i < rowCount; i++) {
                    c.moveToPosition(i);
//                    Toast.makeText(mContext, "正在导出第"+(i+1)+"条", Toast.LENGTH_SHORT).show();
                    Log.v("导出数据", "正在导出第" + (i + 1) + "条");
                    for (int j = 0; j < colCount; j++) {
                        if (j != colCount - 1)
                            bfw.write(c.getString(j) + ',');
                        else
                            bfw.write(c.getString(j));
                    }
                    // 写好每条记录后换行
                    bfw.newLine();
                }
            }
            // 将缓存数据写入文件
            bfw.flush();
            // 释放缓存
            bfw.close();
//            Toast.makeText(mContext, "导出完毕！", Toast.LENGTH_SHORT).show();
            Log.v("导出数据", "导出完毕！");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
    }


    // 调用方法
//    Cursor c = helper.rawQuery("select * from test", null);
//    ExportToCSV(c, "test.csv");
}