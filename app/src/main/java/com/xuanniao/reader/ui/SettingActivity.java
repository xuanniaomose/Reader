package com.xuanniao.reader.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.Constants;

public class SettingActivity extends AppCompatActivity {
    static private String Tag = "SettingActivity";
    private SharedPreferences sp = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // 每个应用有一个默认的偏好文件preferences.xml，使用getDefaultSharedPreferences获取
        sp = PreferenceManager.getDefaultSharedPreferences(this);
    }

}
