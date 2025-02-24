package com.xuanniao.reader.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.*;
import com.xuanniao.reader.ui.book.MainPagesAdapter;
import com.xuanniao.reader.ui.book.PlatformItem;

import java.util.List;
import java.util.Objects;

public class BookActivity extends FragmentActivity {
    static final String Tag = "BookActivity";
    MainPagesAdapter mainPagesAdapter;
    ListView lv_book;
    ViewPager viewPager;
    BookDB bdb;
    PlatformDB pdb;
    List<BookItem> localBookList;
    List<PlatformItem> platformList;
    TextView tv_appName;
    BookAdapter bookAdapter;
    SharedPreferences sp, spConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reader), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 获取读写权限
        Authorize A = new Authorize();
        A.checkReadWritePermission(this);
        init();
        buttonSet();
        guide();
    }

    private void init() {
        // 如果sp文件里有有效的上次的阅读记录，那么直接打开阅读界面
        sp = getSharedPreferences(Constants.SP_BOOK, Context.MODE_PRIVATE);
        spConfig = PreferenceManager.getDefaultSharedPreferences(this);
        bdb = BookDB.getInstance(this, Constants.DB_BOOK);
        pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
        localBookList = bdb.queryAll(Constants.TAB_BOOK);
        loadPlatformList();

        mainPagesAdapter = new MainPagesAdapter(getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(mainPagesAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                spConfig.edit().putInt("mainPageNum", i).apply();
            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

    private void buttonSet() {
//        Button btn_setting = findViewById(R.id.btn_setting);
//        btn_setting.setOnClickListener(view -> {
//            Intent intent = new Intent(BookActivity.this, SettingActivity.class);
//            startService(intent);
//        });
//        Button btn_search = findViewById(R.id.btn_search);
//        btn_search.setOnClickListener(view -> {
//            Intent intent = new Intent(BookActivity.this, SearchActivity.class);
//            startService(intent);
//        });
//        Button btn_local = findViewById(R.id.btn_local);
//        btn_local.setOnClickListener(view -> {
//            Intent intent = new Intent(BookActivity.this, LocalActivity.class);
//            startService(intent);
//        });
//        Button btn_platform = findViewById(R.id.btn_platform);
//        btn_platform.setOnClickListener(view -> {
//            Intent intent = new Intent(BookActivity.this, PlatformActivity.class);
//            startService(intent);
//        });
    }

    private void guide() {
        List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
        if (platformList == null || platformList.isEmpty() || platformList.get(0).getPlatformName() == null) {
            showNeedPlatformWindow();
        } else {
            String topUriString = spConfig.getString("file_authorize", "");
            if (topUriString.isEmpty()) {
                showNeedAuthorizeWindow();
            } else {
                int mainPageNum = spConfig.getInt("mainPageNum", 0);
                viewPager.setCurrentItem(mainPageNum);
            }
        }
    }

    public void loadPlatformList() {
        List<PlatformItem> list = pdb.queryAll(Constants.TAB_PLATFORM);
        Log.d(Tag, "list:" + list);
        if (list != null && !list.isEmpty()) {
            this.platformList = list;
        }
    }

    public List<PlatformItem> getPlatformList() {
        return platformList;
    }

    public void searchForResultList(PlatformItem item, String s) {
        Intent intent = new Intent(BookActivity.this, BookGet.class);
        intent.putExtra("platformItem", item);
        intent.putExtra("BookName", s);
        startService(intent);
    }

    public void openBook(int isLocal, String platformName, String bookName, String bookCode) {
        Intent intent = new Intent(BookActivity.this, CatalogActivity.class);
        intent.putExtra("isLocal", isLocal);
        int platformID = -1;
        for (PlatformItem platformItem : platformList) {
            if (Objects.equals(platformItem.getPlatformName(), platformName)) {
                platformID = platformItem.getID();
            }
        }
        if (platformID != -1) {
            intent.putExtra("platformID", platformID);
        } else {
            Toast.makeText(this, "缺失平台信息，无法打开书目", Toast.LENGTH_SHORT).show();
        }
        intent.putExtra("bookName", bookName);
        intent.putExtra("bookCode", bookCode);
        startActivity(intent);
    }

    // 点击空白区域隐藏键盘 .
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (BookActivity.this.getCurrentFocus() != null) {
                if (BookActivity.this.getCurrentFocus().getWindowToken() != null) {
                    invisibleKeyboard();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    // 隐藏键盘
    public void invisibleKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(BookActivity.this.getCurrentFocus()
                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void showNeedPlatformWindow() {
        AlertDialog dialog = new AlertDialog.Builder(BookActivity.this)
                //.setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("平台信息缺失")//设置对话框的标题
                .setMessage("请加载json文件")//设置对话框的内容
                //设置对话框的按钮
                .setPositiveButton("前往加载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewPager.setCurrentItem(2);
                        Toast.makeText(BookActivity.this,
                                "请使用json文件导入平台信息", Toast.LENGTH_LONG).show();
                    }
                }).create();
        dialog.show();
    }

    private void showNeedAuthorizeWindow() {
        AlertDialog dialog = new AlertDialog.Builder(BookActivity.this)
                //.setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("没有设置存储目录")//设置对话框的标题
                .setMessage("请进入设置页面选择文件夹并授权")//设置对话框的内容
                //设置对话框的按钮
                .setPositiveButton("前往授权", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(BookActivity.this, SettingActivity.class);
                        startActivity(intent);
                        Toast.makeText(BookActivity.this,
                                "点击存储设置中的‘点击进行授权’,然后直接点击下方'使用此文件夹'", Toast.LENGTH_LONG).show();
                    }
                }).create();
        dialog.show();
    }
}
