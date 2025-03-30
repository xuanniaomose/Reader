package com.xuanniao.reader.tools;

public class Constants {
    public static int FILE_SELECT_CODE = 1;
    public static int FOLDER_SELECT_CODE = 2;
    public static String CHANNEL_ID = "xnReader";
    public static String CHANNEL_NAME = "ttsChannel";
    public static int NOTIFICATION_ID = 1;
    public static String URI_KEY = "uriKey";
    public static String SP_CONFIG = "config";
    public static String SP_BOOK = "book";
    public static String DB_BOOK = "bookDB";
    public static String DB_PLATFORM = "platformDB";
    public static String TAB_BOOK = "bookInfo";
    public static String TAB_PLATFORM = "platformTab";
    public static String BOOK_FRAGMENT_NUM = "bookFragmentNum";
    public static String RESULT_FRAGMENT_NUM = "resultFragmentNum";
    public static String INTEGER_DIVIDER = ",";
    public static String CHAPTER_TITLE = "chapterTitle";
    public static String PARAGRAPH_NUM = "paragraphNum";

    // TTS消息
    public static final int MSG_CLOSE = -1;
    public static final int MSG_STOP = 0;
    public static final int MSG_PROGRESS = 1;
    public static final int MSG_PAUSE = 2;
    public static final int MSG_NEXT = 3;
    public static final int NOTIFICATION_CEDE = 110;

    // TTSService的name
    // 朗读段落
    public static final String ACTION_TEXT = "com.xuanniao.reader.text";
    public static final String ACTION_PARAGRAPH = "com.xuanniao.reader.paragraph";
    // 暂停
    public static final String ACTION_PAUSE = "com.xuanniao.reader.pause";
    // 播放
    public static final String ACTION_READ = "com.xuanniao.reader.read";
    // 下一段
    public static final String ACTION_NEXT = "com.xuanniao.reader.next";
    // 上一段
    public static final String ACTION_PREVIOUS = "com.xuanniao.reader.previous";
    public static final String ACTION_STOP = "com.xuanniao.reader.stop";
    // 关闭
    public static final String ACTION_CLOSE = "com.xuanniao.reader.close";
    // seekbar手动控制
    public static final String ACTION_SEEK ="com.xuanniao.reader.seek";

    public static String getLogInfo(int logNum) {
        switch (logNum) {
            case -1:
                return "后台时长过长，朗读已停止";
            case -2:
                return "json数据格式错误，请核对后重新加载";
            case -3:
                return "数据无法解析";
            case -4:
                return "程序错误";
            default:
                return "别听了 不想读了";
        }
    }
}
