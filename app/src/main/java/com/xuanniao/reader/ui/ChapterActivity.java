package com.xuanniao.reader.ui;

import android.app.*;
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
import com.xuanniao.reader.ui.bar.ReadBarFragment;
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
    Button btn_last, btn_next, btn_before, btn_ahead, btn_pause, btn_exit;
    ImageButton btn_back, btn_catalog, btn_reading, btn_color;
    public static Handler handler_paragraph;
    static List<String> paragraphList;
    ArrayList<String> chapterCodeList;
    ArrayList<String> chapterTitleList;
    ChapterAdapter chapterAdapter;
    BookDB bdb;
    TTSService mTTSService;
    private ServiceConnection conn;
    SharedPreferences sp;
    String bookName, bookCode, chapterTitle, chapterCode;
    static int barState = 0; // 0没有弹出栏，1目录栏，2朗读栏，3颜色栏
    static int ttsState = 0; //-1出错，0没有TTS，1正在读，2暂停
    static int nowReading = 0, chapterNum, bookMark;
    static float speed, pitch;
    int isLocal, platformID;
    private NotificationManager mManager;
    private Notification notificationOriginal;
    private Notification notificationCustom;


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
        mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        createNotification();
//        mManager.notify(1, notificationOriginal);
        conn = new TTSServiceConn();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        speed = (float) sp.getInt("tts_speed", 10) / 10;
        pitch = (float) sp.getInt("tts_pitch", 10) / 10;
//        mTTSService = new TTSService(this, speed, pitch);
        Intent serviceIntent = new Intent(this, TTSService.class);
        serviceIntent.putExtra("tts_speed", speed);
        serviceIntent.putExtra("tts_pitch", pitch);
        serviceIntent.putExtra("continuousRead", sp.getBoolean("continuousRead", false));
        bindService(serviceIntent, conn, BIND_AUTO_CREATE);
    }


    /**
     * 判断服务是否在运行
     * @param context
     * @param serviceName
     * @return
     * 服务名称为全路径 例如com.ghost.WidgetUpdateService
     */
    public boolean isServiceRunning(Context context,String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /** 开始tts服务并传输数据 变更列表后需要保证通知到各个组件再播放 */
    public void startTTSService(List<String> paragraphList) {
        Log.d(Tag, "push service paragraphList: " + paragraphList);
//        speed = (float) sp.getInt("tts_speed", 10) / 10;
//        pitch = (float) sp.getInt("tts_pitch", 10) / 10;
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(ChapterActivity.this, TTSService.class);
//        serviceIntent.putExtra("tts_speed", speed);
//        serviceIntent.putExtra("tts_pitch", pitch);
        serviceIntent.putStringArrayListExtra("paragraphList", new ArrayList<>(paragraphList));
        serviceIntent.putExtra("continuousRead", sp.getBoolean("continuousRead", false));
        startService(serviceIntent);
    }

    public final class TTSServiceConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mTTSService = ((TTSService.LocalBinder) binder).getService();
            mTTSService.addCallback((TTSService.Callback) ChapterActivity.this);
            nowReading = (mTTSService != null)? mTTSService.getParagraphNum() : 0;
            Log.i(Tag, "ttsState:" + (mTTSService != null));
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(Tag, "ttsState:" + (mTTSService != null));
            mTTSService = null;
        }
    }

    private void init() {
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
//        Log.d(Tag, "chapterCodeList:" + chapterCodeList);
        bdb = BookDB.getInstance(ChapterActivity.this, Constants.DB_BOOK);
        BookItem bookItem = bdb.queryByFieldItem(Constants.TAB_BOOK, "bookCode", bookCode);
        if (bookItem != null) {
            bookMark = bookItem.getBookMark();
        }

        tv_title = findViewById(R.id.tv_title);
        tv_title.setText(chapterTitle);
        Log.d(Tag, "bookCode:" + bookCode);
        Log.d(Tag, "bookName:" + bookName);
        Log.d(Tag, "chapterCode:" + chapterCode);
        Log.d(Tag, "chapterNum:" + chapterNum);
        lv_article = findViewById(R.id.lv_article);
        lv_article.setOnItemClickListener((adapterView, view, i, l) -> {
            if (ttsState == 1) {
                ttsAction(Constants.ACTION_PARAGRAPH, i);
            }
        });
        handler_paragraph = setParagraphHandler();
        if (paragraphList == null || paragraphList.isEmpty()) {
            Log.d(Tag, "页面初始化获取文本");
            turnPageTo(chapterNum);
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
//        btn_settings = findViewById(R.id.btn_settings);
//        btn_settings.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(ChapterActivity.this, SettingActivity.class);
//                startActivity(intent);
//            }
//        });
        btn_reading = findViewById(R.id.btn_reading);
        btn_reading.setOnClickListener(view -> {
            if (ttsState == 1) {
                ttsAction(Constants.ACTION_STOP, null);
            } else {
                Log.d(Tag, "按下了朗读");
                ttsAction(Constants.ACTION_PARAGRAPH, nowReading);
            }
        });

        btn_color = findViewById(R.id.btn_color);
        btn_color.setOnClickListener(view -> {
            if (barState != 3) controlAction(3);
        });

        btn_last = findViewById(R.id.btn_last);
        btn_last.setOnClickListener(view -> {
            if (chapterNum - 1 > 0) {
                if (mTTSService != null) ttsAction(Constants.ACTION_STOP, null);
                turnPageTo(chapterNum - 1);
            } else {
                Toast.makeText(ChapterActivity.this, "没有上一章了", Toast.LENGTH_SHORT).show();
            }
        });
        btn_next = findViewById(R.id.btn_next);
        btn_next.setOnClickListener(view -> {
            if (chapterNum + 1 <= chapterCodeList.size()) {
                turnPageTo(chapterNum + 1);
            } else {
                Toast.makeText(ChapterActivity.this, "没有下一章了", Toast.LENGTH_SHORT).show();
            }
        });

        btn_before = findViewById(R.id.btn_rewind);
        btn_before.setOnClickListener(view -> ttsAction(Constants.ACTION_PREVIOUS, null));
        btn_ahead = findViewById(R.id.btn_forward);
        btn_ahead.setOnClickListener(view -> ttsAction(Constants.ACTION_NEXT, null));
        btn_pause = findViewById(R.id.btn_pause);
        btn_pause.setOnClickListener(view -> {
            if (ttsState == 1) {
                ttsAction(Constants.ACTION_PAUSE, null);
            } else if (ttsState == 2) {
                ttsAction(Constants.ACTION_PARAGRAPH, nowReading);
            }
        });
        btn_exit = findViewById(R.id.btn_exit);
        btn_exit.setOnClickListener(view -> {
            if (mTTSService != null) ttsAction(Constants.ACTION_STOP, null);
        });
    }

    private void turnPageTo(int targetNum) {
        if (mTTSService != null) ttsAction(Constants.ACTION_STOP, null);
        Intent intent = new Intent(ChapterActivity.this, BookGet.class);
        intent.putExtra("isLocal", isLocal);
        intent.putExtra("platformID", platformID);
        intent.putExtra("bookName", bookName);
        intent.putExtra("bookCode", bookCode);
        intent.putExtra("chapterNum", targetNum);
        intent.putExtra("chapterCode", chapterCodeList.get(targetNum - 1));
        intent.putExtra("chapterTitle", chapterTitleList.get(targetNum - 1));
        startService(intent);
    }

    @Override
    public void onTTSChange(Message msg) {
        switch (msg.what) {
            case Constants.MSG_PROGRESS:
                nowReading = msg.arg1;
                Log.d(Tag, "nowReading:" + nowReading);
                chapterAdapter.setHighlight(nowReading);
                // TODO: 优化高亮显示在中间位置的方法
                lv_article.setSelection(nowReading - 2);
                if (ttsState != 1) {
                    ttsState = 1;
                    btn_pause.setBackground(getDrawable(R.drawable.ic_pause));
                    ll_tts.setVisibility(View.VISIBLE);
                    ll_setting.setVisibility(View.GONE);
//                    getSupportFragmentManager().beginTransaction()
//                            .replace(this.findViewById(R.id.ll_top).getId(),
//                                    ReadBarFragment.newInstance())
//                            .commitNow();
                }
                break;
            case Constants.MSG_PAUSE:
                Log.d(Tag, "暂停");
                nowReading = msg.arg1;
                ttsState = 2;
                btn_pause.setBackground(getDrawable(R.drawable.ic_play));
                break;
            case Constants.MSG_STOP:
                ttsState = 2;
//                btn_reading.setText("朗读");
                chapterAdapter.setHighlight(-1);
                ll_tts.setVisibility(View.GONE);
                ll_setting.setVisibility(View.VISIBLE);
                break;
            case Constants.MSG_NEXT:
                ttsState = 2;
                chapterAdapter.setHighlight(-1);
                ll_tts.setVisibility(View.GONE);
                ll_setting.setVisibility(View.VISIBLE);

                turnPageTo(chapterNum + 1);
                break;
            case Constants.MSG_CLOSE:
                if (ttsState != 0) {
                    ttsState = 0;
                    nowReading = 0;
//                    btn_reading.setText("朗读");
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

    public TTSService getTTSService() {
        if (mTTSService != null) {
            return mTTSService;
        } else {
            return null;
        }
    }

    public void ttsAction(String action, Integer index) {
        if (!mTTSService.getParagraphListExist()) {
            Log.d(Tag, "段落列表：" + paragraphList);
            if (paragraphList != null && !paragraphList.isEmpty())
                this.startTTSService(paragraphList);
            action = (Objects.equals(action, Constants.ACTION_READ))?
                    Constants.ACTION_PARAGRAPH : action;
            index = nowReading;
            Log.d(Tag, "action:" + action);
        }
        if (paragraphList != null && !paragraphList.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(action);
            intent.setPackage("com.xuanniao.reader");
            Log.d(Tag, "列表项：" + index);
            if (index != null) {
                intent.putExtra("paragraphNum", index);
                intent.putExtra("tts_pitch", pitch);
                intent.putExtra("tts_speed", speed);
                intent.putExtra(Constants.ACTION_PARAGRAPH, index);
            }
            sendBroadcast(intent);
        } else {
            Toast.makeText(ChapterActivity.this,
                    "当前播放列表为空", Toast.LENGTH_SHORT).show();
        }
    }

    private void controlAction(int action) {
        if (action == 1) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(this.findViewById(R.id.bottomBar_container).getId(),
//                            TTSBarFragment.newInstance())
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
            public void handleMessage(Message msg_w) {
                if (msg_w.what == 1) {
                    ChapterItem chapterItem = (ChapterItem) msg_w.obj;
                    if (!chapterItem.getChapter().isEmpty()) {
                        String title = chapterItem.getTitle();
                        if (title != null && !title.isEmpty() && !Objects.equals(chapterTitle, title)) {
                            chapterTitle = title;
                            tv_title.setText(chapterTitle);
                        }
                        if (chapterItem.getChapter() != null && !chapterItem.getChapter().isEmpty()) {
                            paragraphList = chapterItem.getChapter();
                            if ((bookMark / 10000) == chapterNum) {
                                nowReading = bookMark % 10000;
                            } else {
                                nowReading = 0;
                            }
                            chapterAdapter = new ChapterAdapter(ChapterActivity.this, paragraphList,
                                    Tools.getIntColor(ChapterActivity.this, "textColor"));
                            lv_article.setAdapter(chapterAdapter);
                            mTTSService.setParagraphList(new ArrayList<>(paragraphList));
                            // 使用书签跳转到上次读的自然段
                            if (nowReading > -1 && nowReading < paragraphList.size()) {
                                lv_article.setSelection(Math.max(nowReading - 2, 0));
                            }
                        }
                        chapterNum = chapterItem.getChapterNum();
                        chapterCode = chapterItem.getChapterCode();
                        addChapterReadAndSaved();

                        // 如果自动连读是开的
                        boolean continuousRead = sp.getBoolean("continuousRead", false);
                        if (continuousRead && (bookMark / 10000) != chapterNum) {
                            ttsAction(Constants.ACTION_PARAGRAPH, nowReading);
                        }
                    }
                } else if (msg_w.what == 2) {
                    Toast.makeText(ChapterActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                } else if (msg_w.what == 3) {
                    ChapterItem chapterItem = (ChapterItem) msg_w.obj;
                    Intent startIntent = new Intent(ChapterActivity.this, BookGet.class);
                    startIntent.putExtra("isLocal", 0);
                    startIntent.putExtra("platformID", platformID);
                    startIntent.putExtra("bookName", bookName);
                    startIntent.putExtra("bookCode", bookCode);
                    int chapterNum = chapterItem.getChapterNum();
                    Log.d(Tag, "chapterNum:" + chapterNum);
                    startIntent.putExtra("chapterNum", chapterNum);
                    startIntent.putExtra("chapterCode", chapterCodeList.get(chapterNum - 1));
                    startIntent.putExtra("chapterTitle", chapterTitleList.get(chapterNum - 1));
                    startService(startIntent);
                } else {
                    Toast.makeText(ChapterActivity.this, "程序BUG", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public void setTextColor(int color) {
        tv_title.setTextColor(color);
        chapterAdapter.setTextColor(color);
    }

    public void setTextSize(int size) {
        chapterAdapter.setTextSize(size);
    }

    public void setReadBackground(int color) {
        getWindow().getDecorView().setBackgroundColor(color);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mTTSService != null) ttsAction(Constants.ACTION_CLOSE, null);
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