package com.xuanniao.reader.ui;

import android.content.*;
import android.os.*;

import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.*;
import com.xuanniao.reader.ui.bar.ColorBarFragment;
import com.xuanniao.reader.ui.bar.TTSBarFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class ChapterActivity extends AppCompatActivity implements TTSService.Callback {
    String Tag = "ChapterActivity";
    TextView tv_title;
    ListView lv_article;
    LinearLayout ll_tts, ll_setting;
    Button btn_last, btn_next;
    ImageButton btn_back, btn_catalog, btn_read, btn_color,
            btn_exit, btn_rewind, btn_pause, btn_forward, btn_reader;
    public static Handler handler_paragraph, handler_ttsOK;
    static List<String> paragraphList;
    ArrayList<String> chapterCodeList;
    ArrayList<String> chapterTitleList;
    ChapterAdapter chapterAdapter;
    BookDB bdb;
    ReadControl readControl;
    SharedPreferences sp;
    String bookName, bookCode, chapterTitle, chapterCode;
    static int barState = 0; // 0没有弹出栏，1目录栏，2朗读栏，3颜色栏
    static int ttsState = 0; //-1出错，0没有TTS，1正在读，2暂停
    static int nowReading = 0, chapterNum, bookMark;
    int isLocal, platformID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chapter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reader), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        buttonSet();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void init() {
        readControl = new ReadControl(this);
        Intent intent = getIntent();
        isLocal = intent.getIntExtra("isLocal", 0);
        platformID = intent.getIntExtra("platformID", -1);
        bookName = intent.getStringExtra("bookName");
        bookCode = intent.getStringExtra("bookCode");
        chapterCode = intent.getStringExtra("chapterCode");
        chapterNum = intent.getIntExtra("chapterNum", -1);
        chapterTitle = intent.getStringExtra("chapterTitle");
        //TODO: 以下两表建议查db获取，以优化性能
//        BookDB bdb = BookDB.getInstance(this, Constants.DB_BOOK);
        chapterCodeList = intent.getStringArrayListExtra("chapterCodeList");
        chapterTitleList = intent.getStringArrayListExtra("chapterTitleList");
        bdb = BookDB.getInstance(ChapterActivity.this, Constants.DB_BOOK);
        BookItem bookItem = bdb.queryByFieldItem(Constants.TAB_BOOK, "bookCode", bookCode);
        if (bookItem != null) {
            bookMark = bookItem.getBookMark();
        }
        tv_title = findViewById(R.id.tv_title);
        tv_title.setText(chapterTitle);
//        Log.d(Tag, "bookCode:" + bookCode);
//        Log.d(Tag, "bookName:" + bookName);
//        Log.d(Tag, "chapterCode:" + chapterCode);
//        Log.d(Tag, "chapterNum:" + chapterNum);
        lv_article = findViewById(R.id.lv_article);
        lv_article.setOnItemClickListener((adapterView, view, i, l) -> {
            if (ttsState == 1) {
                readControl.ttsAction(Constants.ACTION_PARAGRAPH, i);
            }
        });
        handler_paragraph = setParagraphHandler();
        if (paragraphList == null || paragraphList.isEmpty()) {
            Log.d(Tag, "页面初始化获取文本");
            turnPageTo(chapterNum, isLocal, true);
        } else {
            tv_title.setText(chapterTitle);
            chapterAdapter = new ChapterAdapter(ChapterActivity.this,
                    paragraphList, Tools.getIntColor(this, "textColor"));
            lv_article.setAdapter(chapterAdapter);
        }
        addChapterReadAndSaved();
    }

    private void buttonSet() {
        ll_tts = findViewById(R.id.ll_tts);
        ll_setting = findViewById(R.id.ll_setting);

        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        btn_read = findViewById(R.id.btn_read);
        btn_read.setOnClickListener(view -> {
            if (ttsState == 1) {
                readControl.ttsAction(Constants.ACTION_STOP, null);
            } else {
                Log.d(Tag, "按下了朗读");
                ll_setting.setVisibility(View.GONE);
                ll_tts.setVisibility(View.VISIBLE);
                readControl.connect(ChapterActivity.this);
                readControl.setParagraphList(paragraphList);
                handler_ttsOK = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == 1) readControl.ttsAction(
                                Constants.ACTION_PARAGRAPH, nowReading);
                    }
                };
            }
        });

        btn_color = findViewById(R.id.btn_color);
        btn_color.setOnClickListener(view -> {
            if (barState != 3) controlAction(3);
        });

        btn_last = findViewById(R.id.btn_last);
        btn_last.setOnClickListener(view -> {
            if (chapterNum - 1 > 0) {
                if (readControl.getTTSService() != null) {
                    readControl.ttsAction(Constants.ACTION_STOP, null);
                }
                turnPageTo(chapterNum - 1, isLocal, true);
            } else {
                Toast.makeText(ChapterActivity.this, "没有上一章了", Toast.LENGTH_SHORT).show();
            }
        });
        btn_next = findViewById(R.id.btn_next);
        btn_next.setOnClickListener(view -> {
            if (chapterNum + 1 <= chapterCodeList.size()) {
                turnPageTo(chapterNum + 1, isLocal, true);
            } else {
                Toast.makeText(ChapterActivity.this, "没有下一章了", Toast.LENGTH_SHORT).show();
            }
        });

        btn_rewind = findViewById(R.id.btn_rewind);
        btn_rewind.setOnClickListener(view ->
                readControl.ttsAction(Constants.ACTION_PREVIOUS, null));
        btn_forward = findViewById(R.id.btn_forward);
        btn_forward.setOnClickListener(view ->
                readControl.ttsAction(Constants.ACTION_NEXT, null));
        btn_pause = findViewById(R.id.btn_pause);
        btn_pause.setOnClickListener(view -> {
            if (ttsState == 1) {
                readControl.ttsAction(Constants.ACTION_PAUSE, null);
            } else if (ttsState == 2) {
                readControl.ttsAction(Constants.ACTION_READ, null);
            }
        });
        btn_exit = findViewById(R.id.btn_exit);
        btn_exit.setOnClickListener(view -> {
            if (readControl.getTTSService() != null)
                readControl.ttsAction(Constants.ACTION_STOP, null);
        });
        btn_reader = findViewById(R.id.btn_reader);
        btn_reader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (barState != 2) controlAction(2);
            }
        });
    }

    private void turnPageTo(int targetNum, int isLocal, boolean isManual) {
        if (readControl.getTTSService() != null) {
            readControl.ttsAction(Constants.ACTION_STOP, null);
        }
        Intent intent = new Intent(ChapterActivity.this, BookGet.class);
        intent.putExtra("isManual", isManual);
        intent.putExtra("isLocal", isLocal);
        intent.putExtra("platformID", platformID);
        intent.putExtra("bookName", bookName);
        intent.putExtra("bookCode", bookCode);
        intent.putExtra("chapterNum", targetNum);
        intent.putExtra("chapterCode", chapterCodeList.get(targetNum - 1));
        intent.putExtra("chapterTitle", chapterTitleList.get(targetNum - 1));
        try {
            startService(intent);
        } catch (IllegalStateException e) {
            readControl.ttsAction(Constants.ACTION_TEXT, -1);
        }
    }

    @Override
    public void onTTSChange(Message msg) {
        switch (msg.what) {
            case Constants.MSG_PROGRESS:
                nowReading = msg.arg1;
//                Log.d(Tag, "nowReading:" + nowReading);
                chapterAdapter.setHighlight(nowReading);
                // TODO: 优化高亮显示在中间位置的方法
                lv_article.setSelection(nowReading - 2);
                if (ttsState != 1) {
                    ttsState = 1;
                    btn_pause.setImageResource(R.drawable.ic_pause);
                }
                break;
            case Constants.MSG_PAUSE:
                Log.d(Tag, "暂停");
                nowReading = msg.arg1;
                ttsState = 2;
                btn_pause.setImageResource(R.drawable.ic_play);
                break;
            case Constants.MSG_STOP:
                ttsState = 2;
                chapterAdapter.setHighlight(-1);
                ll_tts.setVisibility(View.GONE);
                ll_setting.setVisibility(View.VISIBLE);
                break;
            case Constants.MSG_NEXT:
                ttsState = 2;
                chapterAdapter.setHighlight(-1);
                ll_tts.setVisibility(View.GONE);
                ll_setting.setVisibility(View.VISIBLE);
                turnPageTo(chapterNum + 1, isLocal, false);
                break;
            case Constants.MSG_CLOSE:
                if (ttsState != 0) {
                    ttsState = 0;
                    nowReading = 0;
                    chapterAdapter.setHighlight(-1);
                    ll_tts.setVisibility(View.GONE);
                    ll_setting.setVisibility(View.VISIBLE);
                    this.finish();
                }
                break;
//                Toast.makeText(ChapterActivity.this,
//                        "tts出错", Toast.LENGTH_SHORT).show();
        }
    }

    public ReadControl getReadControl() {
        return readControl;
    }

    private void controlAction(int action) {
        if (action == 1) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(this.findViewById(R.id.bottomBar_container).getId(),
//                            CatalogFragment.newInstance())
//                    .commitNow();
        } else if (action == 2) {
            getSupportFragmentManager().beginTransaction()
                    .replace(this.findViewById(R.id.bottomBar_container).getId(),
                            TTSBarFragment.newInstance())
                    .commitNow();
        } else if (action == 3) {
            getSupportFragmentManager().beginTransaction()
                    .replace(this.findViewById(R.id.bottomBar_container).getId(),
                            ColorBarFragment.newInstance())
                    .commitNow();
        }
    }

    private void addChapterReadAndSaved() {
        if (chapterNum == -1) return;
        // 使用TreeSet排序去重
        List<Integer> readList = bdb.queryFieldWithCode(
                Constants.TAB_BOOK, bookCode, "chapterRead");
        if (readList != null) {
            TreeSet<Integer> readSet = new TreeSet<>(readList);
            readSet.add(chapterNum);
            Log.d(Tag, "readSet:" + readSet);
            List<Integer> rList = new ArrayList<>(readSet);
            bdb.updateItem(Constants.TAB_BOOK, bookCode,
                    "chapterRead", rList, null, null);
        }
        List<Integer> savedList = bdb.queryFieldWithCode(
                Constants.TAB_BOOK, bookCode, "chapterSaved");
        if (savedList != null) {
            TreeSet<Integer> savedSet = new TreeSet<>(savedList);
            savedSet.add(chapterNum);
            Log.d(Tag, "savedList:" + savedSet);
            List<Integer> sList = new ArrayList<>(savedSet);
            bdb.updateItem(Constants.TAB_BOOK, bookCode,
                    "chapterSaved", sList, null, null);
        }
    }

    public Handler setParagraphHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                boolean isManual = (msg.arg2 == 1)? true : false;
                if (msg.what == 1) {
                    ChapterItem chapterItem = (ChapterItem) msg.obj;
                    if (!chapterItem.getChapter().isEmpty()) setChapter(chapterItem, isManual);
                } else if (msg.what == 2) {
                    Toast.makeText(ChapterActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                } else if (msg.what == 3) {
                    Log.d(Tag, "isManual:" + isManual);
                    ChapterItem chapterItem = (ChapterItem) msg.obj;
                    int chapterNum = chapterItem.getChapterNum();
                    Log.d(Tag, "chapterNum:" + chapterNum);
                    turnPageTo(chapterNum, 0, isManual);
                } else {
                    Toast.makeText(ChapterActivity.this, "程序BUG", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void setChapter(ChapterItem chapterItem, boolean isManual) {
        String title = chapterItem.getTitle();
        if (title != null && !title.isEmpty() && !Objects.equals(chapterTitle, title)) {
            chapterTitle = title;
            tv_title.setText(chapterTitle);
        }
        readControl.setTitle(bookName, chapterTitle);
        if (chapterItem.getChapter() != null && !chapterItem.getChapter().isEmpty()) {
            paragraphList = chapterItem.getChapter();
            nowReading = ((bookMark / 10000) == chapterNum)? bookMark % 10000 : 0;
            // 设置正文颜色
            int textColor = sp.getInt("textColor", Tools.getIntColor(
                    ChapterActivity.this, "textColor"));
            chapterAdapter = new ChapterAdapter(ChapterActivity.this, paragraphList, textColor);
            lv_article.setAdapter(chapterAdapter);
            // 设置背景颜色
            int readBackgroundColor = sp.getInt("readBackgroundColor", -1);
            getWindow().getDecorView().setBackgroundColor(readBackgroundColor);
            readControl.setParagraphList(new ArrayList<>(paragraphList));
            // 使用书签跳转到上次读的自然段
            if (nowReading > -1 && nowReading < paragraphList.size()) {
                lv_article.setSelection(Math.max(nowReading - 2, 0));
            }
        }
        chapterNum = chapterItem.getChapterNum();
        chapterCode = chapterItem.getChapterCode();
        addChapterReadAndSaved();
        // 如果自动连读是开的
        boolean isContinuousRead = sp.getBoolean("isContinuousRead", false);
        if (isContinuousRead && !isManual) {
            readControl.ttsAction(Constants.ACTION_PARAGRAPH, nowReading);
            ll_setting.setVisibility(View.GONE);
            ll_tts.setVisibility(View.VISIBLE);
        }
    }

    public void setTextColor(int color) {
        tv_title.setTextColor(color);
        chapterAdapter.setTextColor(color);
        sp.edit().putInt("textColor", color).apply();
    }

    public void setTextSize(int size) {
        chapterAdapter.setTextSize(size);
    }

    public void setReadBackground(int color) {
        getWindow().getDecorView().setBackgroundColor(color);
        sp.edit().putInt("readBackgroundColor", color).apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (readControl.getTTSService() != null) {
            readControl.ttsAction(Constants.ACTION_CLOSE, null);
        }
        bdb.updateItem(Constants.TAB_BOOK, bookCode, "bookMark",
                null, null, (chapterNum * 10000 + nowReading));
        Log.d(Tag, "bookMark:" + (chapterNum * 10000 + nowReading));
        paragraphList = null;
        Intent intent = new Intent(ChapterActivity.this, CatalogActivity.class);
        intent.putExtra("isLocal", isLocal);
        intent.putExtra("platformID", platformID);
        intent.putExtra("bookName", bookName);
        intent.putExtra("bookCode", bookCode);
        startActivity(intent);
    }
}