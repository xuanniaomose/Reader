package com.xuanniao.reader.tools;

import android.app.ActivityManager;
import android.content.*;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.ui.ChapterActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.BIND_AUTO_CREATE;

public class ReadControl {
    private static final String Tag = "ReadControl";
    private ChapterActivity chapterActivity;
    static List<String> paragraphList;
    SharedPreferences sp;
    TTSService ttsService;
    int ttsState = 0; //-1出错，0没有TTS，1正在读，2暂停;
    private ServiceConnection conn;
    static int nowReading = 0;
    static float speed, pitch;
    String bookName, chapterTitle;

    public ReadControl(ChapterActivity activity) {
        chapterActivity = activity;
        sp = PreferenceManager.getDefaultSharedPreferences(chapterActivity);
    }

    public void connect(ChapterActivity chapterActivity) {
        if (conn == null) conn = new TTSServiceConn();
        Intent serviceIntent = new Intent(chapterActivity, TTSService.class);
        serviceIntent.putExtra("isContinuousRead", sp.getBoolean("isContinuousRead", false));
        chapterActivity.bindService(serviceIntent, conn, BIND_AUTO_CREATE);
    }

    /**
     * 判断服务是否在运行
     * @param serviceName
     * @return
     * 服务名称为全路径 例如com.ghost.WidgetUpdateService
     */
    public boolean isServiceRunning(String serviceName) {
        ActivityManager manager = (ActivityManager) chapterActivity.getSystemService(ACTIVITY_SERVICE);
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
        speed = (float) sp.getInt("tts_speed", 25) / 10;
        pitch = (float) sp.getInt("tts_pitch", 10) / 10;
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(chapterActivity, TTSService.class);
        serviceIntent.putStringArrayListExtra("paragraphList", new ArrayList<>(paragraphList));
        serviceIntent.putExtra("continuousRead", sp.getBoolean("continuousRead", false));
        chapterActivity.startService(serviceIntent);
    }

    public final class TTSServiceConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ttsService = ((TTSService.LocalBinder) binder).getService();
            ttsService.addCallback((TTSService.Callback) chapterActivity);
            nowReading = (ttsService != null)? ttsService.getParagraphNum() : 0;
            Log.i(Tag, "ttsState:" + (ttsService != null));
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(Tag, "ttsState:" + (ttsService != null));
            ttsService = null;
        }
    }

    public void ttsAction(String action, Integer index) {
        if (!ttsService.isParagraphListExist()) {
            Log.d(Tag, "段落列表：" + paragraphList);
            if (paragraphList != null && !paragraphList.isEmpty())
                startTTSService(paragraphList);
            action = (Objects.equals(action, Constants.ACTION_READ))?
                    Constants.ACTION_PARAGRAPH : action;
            index = (index == null)? nowReading : index;
        }
        Log.d(Tag, "action:" + action);
        if (paragraphList != null && !paragraphList.isEmpty()) {
            ttsService.setTitle(bookName, chapterTitle);
            Intent intent = new Intent();
            intent.setAction(action);
            intent.setPackage("com.xuanniao.reader");
            Log.d(Tag, "列表项：" + index);
            if (index != null && index >= 0) {
                intent.putExtra("paragraphNum", index);
            } else if (index != null){
                intent.putExtra("logNum", index);
            }
            chapterActivity.sendBroadcast(intent);
        } else {
            Toast.makeText(chapterActivity, "当前播放列表为空", Toast.LENGTH_SHORT).show();
        }
    }

    public void setTitle(String bookName, String chapterTitle) {
        Log.d(Tag, "设置title");
        Log.d(Tag, "bookName:" + bookName + " chapterTitle:" + chapterTitle);
        this.bookName = bookName;
        this.chapterTitle = chapterTitle;
    }

    public void setParagraphList(List<String> list) {
        paragraphList = list;
        if (ttsService != null) ttsService.setParagraphList(new ArrayList<>(list));
    }

    public TTSService getTTSService() {
        return ttsService;
    }

    public int getTTSState() {
        return ttsState;
    }
}
