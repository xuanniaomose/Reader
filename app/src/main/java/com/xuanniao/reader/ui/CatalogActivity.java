package com.xuanniao.reader.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.*;

import java.util.ArrayList;

public class CatalogActivity extends AppCompatActivity {
    static final String Tag = "CatalogActivity";
    TextView tv_bookName;
    ListView lv_catalog;
    CatalogAdapter catalogAdapter;
    SharedPreferences sp;
    BookDB bdb;
    public static Handler handler_catalog;
    CatalogItem catalogItem;
    String bookName, bookCode;
    int isLocal, platformID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_catalog);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reader), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 获取网络权限
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // 获取读写权限
        Authorize A = new Authorize();
        A.checkReadWritePermission(this);
        init();
        buttonSet();
    }

    private void init() {
        Intent intentGet = getIntent();
        isLocal = intentGet.getIntExtra("isLocal", 0);
        platformID = intentGet.getIntExtra("platformID", -1);
        bookName = intentGet.getStringExtra("bookName");
        bookCode = intentGet.getStringExtra("bookCode");
        tv_bookName = findViewById(R.id.tv_bookName);
        if (bookName != null && !bookName.isEmpty()) {
            tv_bookName.setText(bookName);
        }
        Log.d(Tag, "intentGet.isLocal:" + isLocal);
        Log.d(Tag, "intentGet.bookName:" + bookName);
        Log.d(Tag, "intentGet.bookCode:" + bookCode);
        Log.d(Tag, "intentGet.platformID:" + platformID);
        lv_catalog = findViewById(R.id.lv_catalog);
        handler_catalog = setCatalogHandler();
        if (bookName != null) {
            catalogItem = FileTools.loadLocalCatalog(this, bookName);
        }
        if (catalogItem == null || catalogItem.getChapterCodeList() == null || catalogItem.getChapterCodeList().isEmpty()) {
            Log.d(Tag, "填充目录列表");
            Intent intent = new Intent(CatalogActivity.this, BookGet.class);
            intent.putExtra("isLocal", isLocal);
            intent.putExtra("platformID", platformID);
            intent.putExtra("bookName", bookName);
            intent.putExtra("bookCode", bookCode);
            startService(intent);
        } else {
            setCatalog(catalogItem);
        }
    }

    private void buttonSet() {
        Button btn_setting = findViewById(R.id.btn_catalogSetting);
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, SettingActivity.class);
                startActivity(intent);
//                Intent intent = new Intent(CatalogActivity.this, BookGet.class);
//                intent.putExtra("isLocal", 1);
//                intent.putExtra("bookName", bookName);
//                intent.putExtra("bookCode", bookCode);
//                startService(intent);
            }
        });
        Button btn_file = findViewById(R.id.btn_file);
        btn_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Tag, "点击了:");
            }
        });
    }

    private Handler setCatalogHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    catalogItem = (CatalogItem) msg.obj;
                    setCatalog(catalogItem);
                } else if (msg.what == 2) {
                    Toast.makeText(CatalogActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CatalogActivity.this, "程序BUG", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void setCatalog(CatalogItem catalogItem) {
        if (catalogItem.getBookName() == null || catalogItem.getBookName().isEmpty()) return;
        bookName = catalogItem.getBookName();
        tv_bookName.setText(bookName);
        catalogAdapter = new CatalogAdapter(CatalogActivity.this, catalogItem);
        lv_catalog.setAdapter(catalogAdapter);
        lv_catalog.setOnItemClickListener((adapterView, view, i, l) -> {
            String title = catalogItem.getChapterTitle(i);
            String chapterCode = catalogItem.getChapterCode(i);
            Log.d(Tag, "chapterCode:" + chapterCode + " | title:" + title + " | chapterNum:" + (i+1));
            startReader((i + 1), chapterCode, title);
        });
        bdb = BookDB.getInstance(this, Constants.DB_BOOK);
        BookItem bookItem = bdb.queryByFieldItem(Constants.TAB_BOOK, "bookCode", bookCode);
        int catalogNumMark = bookItem.getBookMark() / 10000;
        if (catalogNumMark >= 0 && catalogNumMark < catalogItem.getChapterCodeList().size()) {
            lv_catalog.setSelection(catalogNumMark - 2);
        }
    }

    private void startReader(int chapterNum, String chapterCode, String chapterTitle) {
        Intent intent = new Intent(CatalogActivity.this, ChapterActivity.class);
        Log.d(Tag, String.valueOf(bookCode));
        intent.putExtra("isLocal", isLocal);
        intent.putExtra("platformID", platformID);
        intent.putExtra("bookName", bookName);
        intent.putExtra("bookCode", bookCode);
        Log.d(Tag, "startReader isLocal:" + isLocal);
        Log.d(Tag, "startReader bookName:" + bookName);
        Log.d(Tag, "startReader bookCode:" + bookCode);
        Log.d(Tag, "startReader platformID:" + platformID);
        intent.putExtra("chapterNum", chapterNum);
        intent.putExtra("chapterCode", chapterCode);
        intent.putExtra("chapterTitle", chapterTitle);
        intent.putStringArrayListExtra("chapterCodeList",
                new ArrayList<>(catalogItem.getChapterCodeList()));
        intent.putStringArrayListExtra("chapterTitleList",
                new ArrayList<>(catalogItem.getChapterTitleList()));
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(CatalogActivity.this, BookActivity.class);
        startActivity(intent);
    }
}
