package com.xuanniao.reader.tools;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.R;
import com.xuanniao.reader.ui.ChapterActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.app.Notification.VISIBILITY_SECRET;

public class TTSService extends Service {
    static String Tag = "TTSService";
    Context mContext;
    private static TextToSpeech localTTS = null;
    private TTSBroadReceiver receiver;
    float speed, pitch;
    boolean isContinuous;
    String bookName, chapterTitle;
    static ArrayList<String> paragraphList;
    static int paragraphNum = 0, ttsState = 0; //-1出错，0没有TTS，1正在读，2暂停
    private List<Callback> callbackList;  // 回调接口的集合
    private NotificationManagerCompat notificationManager;

    public TTSService() {}

    public TTSService(Context context) {
        this.mContext = context;
    }

    @Override
    public IBinder onBind(Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        speed = (float) sp.getInt("tts_speed", 255) / 10;
        pitch = (float) sp.getInt("tts_pitch", 10) / 10;
        isContinuous = intent.getBooleanExtra("isContinuousRead", false);
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        mContext = this;
        Log.i(Tag, "onCreate: TTSService");
        callbackList = new ArrayList<Callback>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_PARAGRAPH);
        intentFilter.addAction(Constants.ACTION_PAUSE);
        intentFilter.addAction(Constants.ACTION_READ);
        intentFilter.addAction(Constants.ACTION_STOP);
        intentFilter.addAction(Constants.ACTION_NEXT);
        intentFilter.addAction(Constants.ACTION_PREVIOUS);
        intentFilter.addAction(Constants.ACTION_CLOSE);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.setPriority(1000);
        if (receiver == null) {
            receiver = new TTSBroadReceiver();
        }
        ContextCompat.registerReceiver(getApplicationContext(),
                receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        if (localTTS == null) localTTS = new TextToSpeech(
                mContext.getApplicationContext(), new TTSOnInitListener());

        notificationManager = NotificationManagerCompat.from(this);
        createNotificationChannel();
        setLockScreen();
        super.onCreate();
    }

    public void setTitle(String bookName, String chapterTitle) {
        Log.d(Tag, "bookName:" + bookName);
        this.bookName = bookName;
        this.chapterTitle = chapterTitle;
    }

    public void setParagraphList(ArrayList<String> list) {
        paragraphList = list;
        paragraphNum = 0;
    }

    public void setSpeechRate(float speed) {
        localTTS.setSpeechRate(speed);
    }

    public void setPitch(float pitch) {
        localTTS.setPitch(pitch);
    }

    public int getParagraphNum() {
        return paragraphNum;
    }

    public boolean getIsPlaying() {
        return localTTS.isSpeaking();
    }

    public boolean isParagraphListExist() {
        Log.d(Tag, "paragraphList:" + paragraphList);
        return paragraphList != null && !paragraphList.isEmpty();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.d(Tag, "服务开始");
            paragraphList = intent.getStringArrayListExtra("paragraphList");
            paragraphNum = intent.getIntExtra("paragraphNum", 0);
//            Log.d(Tag, "paragraphList:" + paragraphList);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class TTSOnInitListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                int result = localTTS.setLanguage(Locale.CHINESE);
                Log.d(Tag, "result:" + result);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "不支持中文语音合成");
                    if (result == TextToSpeech.LANG_MISSING_DATA) {
                        Log.e("TTS", "缺少中文语音数据，正在下载...");
                        Intent installIntent = new Intent();
                        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        mContext.startActivity(installIntent);
                    }
                } else {
                    // 检查语音数据是否已下载
                    if (localTTS.isLanguageAvailable(Locale.CHINESE) >= TextToSpeech.LANG_AVAILABLE) {
                        Log.i(Tag, "中文语音数据已完整下载");
                        ttsState = 2;
                        int result0 = localTTS.setLanguage(Locale.CHINESE);
                        if (result0 != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE) {
                            Log.d(Tag, "TTS不支持中文");
                        }
                        int result1 = localTTS.setPitch(pitch);
                        int result2 = localTTS.setSpeechRate(speed);
                        localTTS.setOnUtteranceProgressListener(new ttsUtteranceListener());

                        Message msg = new Message();
                        msg.what = 1;
                        Log.d(Tag, "send message ttsOK");
                        ChapterActivity.handler_ttsOK.sendMessage(msg);
                    } else {
                        Log.e("TTS", "中文语音数据未完整下载");
                    }
                }
            } else {
                Log.e("TTS", "TextToSpeech引擎初始化失败");
            }
        }
    }

    private class ttsUtteranceListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {}
        @Override
        public void onRangeStart(String utteranceId, int start, int end, int frame) {
            //utteranceId是speak方法中最后一个参数：唯一标识码
            Log.d(Tag, "Range utteranceId:" + utteranceId + " start:" + start + " end:" + end + " frame:" + frame);
        }
        @Override
        public void onDone(String utteranceId) {
            // 播放完成
            if (paragraphNum < paragraphList.size()) {
                paragraphNum ++;
                ttsStart(paragraphNum);
            } else {
                // 播完发送停止信号
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_NEXT);
                sendBroadcast(intent);
                paragraphNum = 0;
            }
        }
        @Override
        public void onError(String utteranceId) {
            //这个onError方法已过时，用下面这个方法代替
        }
        @Override
        public void onError(String utteranceId,int errorCode) {
            //这个方法代替上面那个过时方法
            Intent intent = new Intent();
            intent.setAction(Constants.ACTION_NEXT);
            sendBroadcast(intent);
        }
    }

    /**
     * 开始
     * @param num 朗读起始的段落号
     */
    public void ttsStart(Integer num) {
        if (paragraphList == null || paragraphList.isEmpty()) return;
//        Log.d(Tag, "localTTS:" + (localTTS != null));
        if (localTTS == null) localTTS = new TextToSpeech(
                mContext.getApplicationContext(), new TTSOnInitListener());
        if (num >= 0 && num < paragraphList.size()) {
            ttsState = 1;
            Log.d(Tag, "num:" + num);
            String text = paragraphList.get(num);
//            StringBuilder text = new StringBuilder();
//            for (int i = num; i < paragraphList.size(); i++) {
//                String paragraph = paragraphList.get(i);
//                text.append("\r\n").append(paragraph);
//                if (text.length() > 2000) {
//                    paragraphNum = i;
//                    break;
//                }
//                paragraphNum = i;
//                Log.d(Tag, "paragraphNum:" + paragraphNum);
//            }
            localTTS.speak(text, 0, null, String.valueOf(paragraphNum));
            sendTTSStateToMain(Constants.MSG_PROGRESS);
        } else if (num == paragraphList.size() && isContinuous) {
            Log.d(Tag, "num = " + num);
            paragraphNum = 0;
            sendTTSStateToMain(Constants.MSG_NEXT);
        } else {
//            sendTTSStateToMain(Constants.MSG_ERROR);
        }
        updateNotification();
    }

    public void ttsPause() {
        if (localTTS != null && localTTS.isSpeaking()) {
            // TODO: 获取当前段落的文字位置
//            paragraphWordPosition = localTTS.getCurrentPosition();
            localTTS.stop();
            ttsState = 2;
            sendTTSStateToMain(Constants.MSG_PAUSE);
            updateNotification();
        }
    }

    public void ttsStop() {
        if (notificationManager != null) {
            notificationManager.cancel(Constants.NOTIFICATION_CEDE);
        }
        if (localTTS != null) {
            localTTS.stop();
            ttsState = 2;
        }
        stopForeground(true);
        sendTTSStateToMain(Constants.MSG_STOP);
    }

    public void ttsRead(int logNum) {
        localTTS.speak(Constants.getLogInfo(logNum),
                0, null, String.valueOf(logNum));
    }

    public void ttsShutdown() {
        if (notificationManager != null) {
            notificationManager.cancel(Constants.NOTIFICATION_CEDE);
        }
        if (localTTS != null) {
            Log.d(Tag, "tts shutdown");
            localTTS.shutdown();
            ttsState = 0;
        }
        if (paragraphList != null) {
            paragraphList = null;
        }
        sendTTSStateToMain(Constants.MSG_CLOSE);
    }

    private void sendTTSStateToMain(int msgCode) {
        Message msg = Message.obtain();
        msg.what = msgCode;
        msg.arg1 = paragraphNum;
        handler.sendMessage(msg);
    }

    public void setContinuousRead(boolean isContinuous) {
        this.isContinuous = isContinuous;
    }

    public class TTSBroadReceiver extends BroadcastReceiver {
//        private String Tag = "TTSBroadReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Tag, "收到消息: " + intent.getAction());
            switch (intent.getAction()) {
                case Constants.ACTION_PARAGRAPH:
                    // 朗读段落
                    paragraphNum = intent.getIntExtra("paragraphNum", 0);
                    Log.i(Tag, "段落:" + paragraphNum);
                    ttsStart(paragraphNum);
                    break;
                case Constants.ACTION_PAUSE:
                    Log.i(Tag, "暂停");
                    // 暂停播放
                    ttsPause();
                    break;
                case Constants.ACTION_READ:
                    Log.i(Tag, "播放");
                    // 开始播放
                    if (localTTS == null) {
                        localTTS = new TextToSpeech(context.getApplicationContext(),
                                new TTSService.TTSOnInitListener());
                    }
                    ttsStart(paragraphNum);
                    break;
                case Constants.ACTION_SEEK:
                    Log.i(Tag, "空降");
                    // 空降至
                    int position = intent.getIntExtra(Constants.PARAGRAPH_NUM, 0);
                    ttsStart(position);
                    break;
                case Constants.ACTION_NEXT:
                    Log.i(Tag, "下一段");
                    paragraphNum ++;
                    ttsStart(paragraphNum);
                    break;
                case Constants.ACTION_PREVIOUS:
                    Log.i(Tag, "上一段");
                    paragraphNum --;
                    ttsStart(paragraphNum);
                    break;
                case Constants.ACTION_STOP:
                    Log.i(Tag, "停止");
                    ttsStop();
                    break;
                case Constants.ACTION_CLOSE:
                    Log.i(Tag, "关闭");
                    ttsShutdown();
//                    onDestroy();
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    Log.i(Tag, "耳机拔出");
                    // 如果耳机拨出时暂停播放
                    ttsPause();
                    break;
                case Constants.ACTION_TEXT:
                    Log.i(Tag, "朗读信息");
                    int logNum = intent.getIntExtra("logNum", 0);
                    ttsRead(logNum);
                    break;
            }
        }
    }

    public final class LocalBinder extends Binder {
        public TTSService getService() {
            return TTSService.this;
        }
    }

    /** 回调接口 */
    public interface Callback {
        public void onTTSChange(Message msg);
    }

    /**
     * 往回调接口集合中添加一个实现类
     * @param callback 实现类的回调
     */
    public void addCallback(Callback callback) {
        callbackList.add(callback);
    }

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            // 遍历集合，通知所有的实现类，即activity
            for (TTSService.Callback callback : callbackList) {
                callback.onTTSChange(msg);
            }
        }
    };

    private void updateNotification() {
        Notification notification = buildNotification();
        // 通知的权限检查
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(Constants.NOTIFICATION_ID, notification);
        // 如果服务已经转为后台，需要重新进入前台
        startForeground(Constants.NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        //8.0以上版本通知适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    "媒体播放通道",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于媒体播放控制");
            // 是否绕过请勿打扰模式
            channel.canBypassDnd();
            // 设置可绕过  请勿打扰模式
            channel.setBypassDnd(true);
            // 应用图标右上角显示小红点
            channel.setShowBadge(false);
            // 所有情况下显示，包括锁屏
            channel.setLockscreenVisibility(VISIBILITY_SECRET);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 创建通知
     * @return 通知
     */
    private Notification buildNotification() {
//        Log.d(Tag, "create builder: " + Constants.CHANNEL_ID);
        RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.notification); // 自定义view，实际是remoteView
//        Intent intent_chapter = new Intent(mContext, ChapterActivity.class);
//        PendingIntent pending_intent_go = PendingIntent.getActivity(mContext, 1, intent_chapter, PendingIntent.FLAG_UPDATE_CURRENT);
//        remoteView.setOnClickPendingIntent(R.id.notice, pending_intent_go);

        // 4个参数context, requestCode, intent, flags
        Intent intent_cancel = new Intent();
        intent_cancel.setPackage("com.xuanniao.reader");
        intent_cancel.setAction(Constants.ACTION_STOP);
        PendingIntent pending_intent_close = PendingIntent.getBroadcast(this, 2, intent_cancel, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.notify_btn_close, pending_intent_close);

        // 上一篇
        Intent intent_prv = new Intent();
        intent_prv.setPackage("com.xuanniao.reader");
        intent_prv.setAction(Constants.ACTION_PREVIOUS);
        PendingIntent pending_intent_prev = PendingIntent.getBroadcast(this, 3, intent_prv, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.notify_btn_previous, pending_intent_prev);

        // 设置播放暂停
        PendingIntent pending_intent_play;
        Intent intent_play_pause = new Intent();
        intent_play_pause.setPackage("com.xuanniao.reader");
        if (ttsState == 1) {//如果正在播放则暂停
            intent_play_pause.setAction(Constants.ACTION_PAUSE);
            pending_intent_play = PendingIntent.getBroadcast(this, 4, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteView.setOnClickPendingIntent(R.id.notify_btn_play, pending_intent_play);
            remoteView.setImageViewResource(R.id.notify_btn_play, R.drawable.ic_pause);
        } else {//如果暂停则播放
            intent_play_pause.setAction(Constants.ACTION_READ);
            pending_intent_play = PendingIntent.getBroadcast(this, 5, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteView.setOnClickPendingIntent(R.id.notify_btn_play, pending_intent_play);
            remoteView.setImageViewResource(R.id.notify_btn_play, R.drawable.ic_play);
        }
        // 下一篇
        Intent intent_next = new Intent();
        intent_next.setPackage("com.xuanniao.reader");
        intent_next.setAction(Constants.ACTION_NEXT);
        PendingIntent pending_intent_next = PendingIntent.getBroadcast(this, 6, intent_next, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.notify_btn_next, pending_intent_next);

//        Log.d(Tag, "bookName:" + bookName);
        remoteView.setTextViewText(R.id.notify_tv_bookName, bookName);
        remoteView.setInt(R.id.progressBar, "setMax", paragraphList.size());

//        Log.d(Tag, "chapterTitle:" + chapterTitle);
        remoteView.setTextViewText(R.id.notify_tv_chapterTitle, chapterTitle);
        remoteView.setInt(R.id.progressBar, "setProgress", paragraphNum);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book)
                .setContent(remoteView)
                .setAutoCancel(true) // 允许自动清除
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL) //统一消除声音和震动
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 所有情况下显示，包括锁屏
                .setOngoing(true); // 禁止滑动删除
        return builder.build();
    }

    private void setLockScreen() {
        // TODO: 需要修改这里，让锁屏窗口能够正常显示
        Log.d(Tag, "创建锁屏窗口");
        // 创建一个悬浮窗口布局
        View view = LayoutInflater.from(this).inflate(R.layout.notification, null);
        // 创建一个LayoutParams对象，指定悬浮窗口的大小和位置
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        // 将悬浮窗口添加到窗口管理器中，并显示在锁屏界面上
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(view, layoutParams);
    }

    @Override
    public void onDestroy() {
        Log.i(Tag, "onDestroy: ttsService");
        if (localTTS != null) localTTS = null;
        if (receiver != null) {
            getApplicationContext().unregisterReceiver(receiver);
        }
        stopSelf();
    }
}
