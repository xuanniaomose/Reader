package com.xuanniao.reader.ui;

import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.xuanniao.reader.R;
import com.xuanniao.reader.getter.CatalogGetter;
import com.xuanniao.reader.getter.FileTools;
import com.xuanniao.reader.item.BookDB;
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.item.CatalogItem;
import com.xuanniao.reader.tools.*;

import java.util.ArrayList;
import java.util.List;

public class CatalogActivity extends AppCompatActivity {
    static final String Tag = "CatalogActivity";
    TextView tv_bookName;
    ListView lv_catalog;
    CatalogAdapter catalogAdapter;
    BookDB bdb;
    public static Handler handler_catalog;
    List<String> catalogPageCodeList;
    CatalogItem catalogItem;
    BookItem bookItem;
    String bookName, bookCode;
    int platformID;
    boolean isLocal;

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
        isLocal = intentGet.getBooleanExtra("isLocal", false);
        platformID = intentGet.getIntExtra("platformID", -1);
        bookItem = (BookItem) intentGet.getSerializableExtra("bookItem");
        if (bookItem == null)
            Toast.makeText(this, "无法打开", Toast.LENGTH_SHORT).show();
        tv_bookName = findViewById(R.id.tv_bookName);
        if (bookName != null && !bookName.isEmpty()) {
            tv_bookName.setText(bookName);
        }
        bookCode = bookItem.getBookCode();
//        Log.d(Tag, "intentGet.isLocal:" + isLocal);
//        Log.d(Tag, "intentGet.bookName:" + bookName);
//        Log.d(Tag, "intentGet.bookCode:" + bookCode);
//        Log.d(Tag, "intentGet.platformID:" + platformID);
        lv_catalog = findViewById(R.id.lv_catalog);
        handler_catalog = setCatalogHandler();
        if (bookName != null) {
            catalogItem = FileTools.loadLocalCatalog(this, bookName);
        }
        if (catalogItem == null || catalogItem.getChapterCodeList() == null || catalogItem.getChapterCodeList().isEmpty()) {
            Log.d(Tag, "填充目录列表");
            Intent intent = new Intent(CatalogActivity.this, CatalogGetter.class);
            intent.putExtra("isLocal", isLocal);
            intent.putExtra("platformID", platformID);
            intent.putExtra("bookItem", bookItem);
            startService(intent);
        } else {
            setCatalog(catalogItem);
        }
    }

    private void buttonSet() {
        Button btn_setting = findViewById(R.id.btn_catalogSetting);
        btn_setting.setOnClickListener(view -> {
            Intent intent = new Intent(CatalogActivity.this, SettingActivity.class);
            startActivity(intent);
        });
        Button btn_file = findViewById(R.id.btn_file);
        btn_file.setOnClickListener(view -> Log.d(Tag, "点击了:"));
    }

    private Handler setCatalogHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if (msg.arg2 > 0) {
                        // 是分好几页显示的目录
                        int part = msg.arg2 / 100;
                        int total = msg.arg2 % 100;
                        Log.d(Tag, "part:" + part + " | total:" + total);
                        if (part == 1) {
                            catalogItem = (CatalogItem) msg.obj;
                            setCatalog(catalogItem);
                            FileTools.newBook(CatalogActivity.this, bookItem, catalogItem);
                        } else {
                            int start = catalogItem.getChapterCodeList().size();
                            CatalogItem item = (CatalogItem) msg.obj;
                            List<String> titleList = item.getChapterTitleList();
                            List<String> codeList = item.getChapterCodeList();
                            catalogItem.appendTitleList(titleList);
                            Log.d(Tag, "part titleList:" + titleList.get(0));
                            catalogItem.appendCodeList(codeList);
                            setCatalog(catalogItem);
                            boolean b = FileTools.catalogAppend(CatalogActivity.this, start, item);
                            Log.d(Tag, "目录追加:" + b);
                        }
                        if (part < total) {
                            if (catalogPageCodeList == null)
                                catalogPageCodeList = catalogItem.getCatalogPageCodeList();
                            String pageCode = catalogPageCodeList.get(part);
                            Log.d(Tag, "nextPart:" + (part + 1) + " | pageCode:" + pageCode);
                            Intent intent = new Intent(CatalogActivity.this, CatalogGetter.class);
                            intent.putExtra("isLocal", isLocal);
                            intent.putExtra("platformID", platformID);
                            intent.putExtra("bookItem", bookItem);
                            intent.putExtra("part", (part + 1) * 100 + total);
                            intent.putExtra("pageCode", pageCode);
                            startService(intent);
                        }
                    } else {
                        catalogItem = (CatalogItem) msg.obj;
                        setCatalog(catalogItem);
                        if (!isLocal) {
                            Log.d(Tag, "新建书目");
                            FileTools.newBook(CatalogActivity.this, bookItem, catalogItem);
                        }
                    }
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
        if (bookItem != null) {
            int catalogNumMark = bookItem.getBookMark() / 10000;
            if (catalogNumMark >= 0 && catalogNumMark < catalogItem.getChapterCodeList().size()) {
                lv_catalog.setSelection(catalogNumMark - 2);
            }
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
