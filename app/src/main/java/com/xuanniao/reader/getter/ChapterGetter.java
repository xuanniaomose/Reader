package com.xuanniao.reader.getter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.xuanniao.reader.item.ChapterItem;
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.item.PlatformDB;
import com.xuanniao.reader.ui.*;
import com.xuanniao.reader.item.PlatformItem;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChapterGetter extends Service {
    static String Tag = "ChapterGetter";
    PlatformDB pdb;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(Tag, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            Log.d(Tag, "onStartCommand - startId = " + startId + ", " +
                    "Thread ID = " + Thread.currentThread().getId());
            boolean isManual = intent.getBooleanExtra("isManual", true);
            boolean isLocal = intent.getBooleanExtra("isLocal", true);
            int platformID = intent.getIntExtra("platformID", -1);
            String bookName = intent.getStringExtra("bookName");
            String bookCode = intent.getStringExtra("bookCode");
            int chapterNum = intent.getIntExtra("chapterNum", -1);
            String chapterCode = intent.getStringExtra("chapterCode");
            String chapterTitle = intent.getStringExtra("chapterTitle");
            Log.d(Tag, "bookCode:" + bookCode + " chapterCode:" + chapterCode);

            pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
            List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
            Log.d(Tag, "isLocal:" + isLocal + " | platformID:" + platformID);
            if (isLocal) {
                ChapterItem chapterItem = FileTools.loadLocalChapter(
                        this, bookName, chapterNum, chapterTitle);
                sendMessage(1, isManual, chapterItem);
            } else {
                ChapterItem chapterItem = new ChapterItem();
                chapterItem.setBookName(bookName);
                chapterItem.setTitle(chapterTitle);
                chapterItem.setChapterNum(chapterNum);
                chapterItem.setChapterCode(chapterCode);
                if (platformID != -1) {
                    PlatformItem platformItem = platformList.get(platformID);
                    Log.d(Tag, "chapterNum:" + chapterNum + " | chapterCode:" + chapterCode);
                    getHtml(this, isManual, platformItem, bookCode, chapterCode, chapterItem);
                } else {
                    for (PlatformItem platformItem : platformList) {
                        getHtml(this, isManual, platformItem, bookCode, chapterCode, chapterItem);
                    }
                }
            }
            stopSelf();
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 从网络获取内容
     * @param context 上下文
     * @param isManual 是否手动
     * @param platformItem 平台
     * @param bookCode 书籍编号
     * @param chapterCode 章节编号
     * @param chapterItem 输入的章节
     */
    private void getHtml(Context context, boolean isManual, PlatformItem platformItem,
                         String bookCode, String chapterCode, ChapterItem chapterItem) {
        String url = platformItem.getPlatformUrl() + bookCode + "/" + chapterCode + ".html";
        Log.d("url", url);
        String cookie = platformItem.getPlatformCookie();
        if (cookie.contains("timeLong")) {
            String timeLong = String.valueOf(System.currentTimeMillis());
            cookie = cookie.replaceAll("timeLong", timeLong.substring(0, 10));
        }
        Log.d(Tag, "Cookie:" + cookie);
        Request request = new Request.Builder()
                .url(url)
                .headers(Filter.setHeaders(cookie))
                .build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                String htmlContent = "0";
                Log.d(Tag, "请求失败onFailure");
                ChapterItem item = htmlToChapter(platformItem, htmlContent, chapterItem);
                sendMessage(platformItem.getID(), isManual, item);
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String htmlContent;
                try {
                    if (response.body() != null) {
                        Log.d(Tag, "请求成功");
                        String charsetName = platformItem.getCharsetName();
                        charsetName = (charsetName.isEmpty())?
                                PreferenceManager.getDefaultSharedPreferences(context)
                                        .getString("charsetName", "UTF-8") : charsetName;
                        Log.d(Tag, "charsetName:" + charsetName);
                        htmlContent = new String(response.body().bytes(), charsetName);
                        Log.d(Tag, "htmlContent:" + htmlContent);
                    } else {
                        Log.d(Tag, "请求失败 没有返回值");
                        htmlContent = "0";
                    }
                    ChapterItem item = htmlToChapter(platformItem, htmlContent, chapterItem);
                    sendMessage(platformItem.getID(), isManual, item);
                } catch (IOException e) {
                    Log.e(Tag, "e:" + e.getMessage());
                    sendMessage(platformItem.getID(), isManual, chapterItem);
                }
            }
        });
    }

    /**
     * @param platformItem 平台
     * @param htmlContent 返回的网页
     * @param chapterItem 输入的章节
     * @return 处理完输出的章节
     */
    private ChapterItem htmlToChapter(PlatformItem platformItem, String htmlContent, ChapterItem chapterItem) {
        Log.d(Tag, "htmlContent:" + htmlContent);
        String chapterError = platformItem.getChapterError();
        if (htmlContent.contains(chapterError) || htmlContent.equals("0")) {
            return chapterItem;
        } else {
            Log.d(Tag, "开始匹配ChapterList");
            if (htmlContent != null) {
                String[] chapterPage = platformItem.getChapterPage();
//                Log.d(Tag, "chapterPage:" + Arrays.toString(chapterPage));
                JSONObject chapterFormatJson = null;
                try {
                    chapterFormatJson = JSONObject.parseObject(platformItem.getChapterPageFormat());
                    if (chapterFormatJson == null) return chapterItem;
                    Document doc = Jsoup.parseBodyFragment(htmlContent);
                    JSONArray titleStep = chapterFormatJson.getJSONArray("title");
                    String title = Filter.getAttr(titleStep, doc.body());
                    if (title == null) return chapterItem;
                    chapterItem.setTitle(title);
                    JSONArray findList = chapterFormatJson.getJSONArray("paragraphList");
                    if (Arrays.asList(chapterPage).contains("paragraph")) {
                        Elements chapterAttrs = Filter.switchActionToElements(findList, doc);
                        if (chapterAttrs == null) return chapterItem;
                        JSONArray paragraphStep = chapterFormatJson.getJSONArray("paragraph");
                        for (Element chapterAttr : chapterAttrs) {
                            String paragraph = Filter.getAttr(paragraphStep, chapterAttr);
                            chapterItem.addParagraph(paragraph);
                        }
                    } else {
                        String[] paragraphArray = Filter.switchActionToParagraphArray(findList, doc);
                        if (paragraphArray == null) return chapterItem;
                        for (String chapterAttr : paragraphArray) {
                            if (Objects.equals(chapterAttr.trim(), "")) continue;
                            if (Objects.equals(chapterAttr.trim(), "<script>chaptererror();</script>")) continue;
                            if (chapterAttr.trim().contains("请记住本书首发域名")) continue;
                            if (chapterAttr.trim().contains("<!--")) continue;
                            chapterItem.addParagraph(chapterAttr.trim());
                        }
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                    return chapterItem;
                }
                Log.d(Tag, "结束匹配ChapterList");
            }
        }
        return chapterItem;
    }

    private void sendMessage(int platformID, boolean isManual, ChapterItem chapterItem) {
        Message msg = new Message();
        Log.d(Tag, "解读出来是段落列表");
        if (chapterItem.getChapter() == null || chapterItem.getChapter().isEmpty()) {
            if (chapterItem.getIsLocal() == 1) {
                // 本地没有
                msg.what = 3;
                msg.arg1 = platformID;
                msg.arg2 = (isManual)? 1 : 0;
                msg.obj = chapterItem;
            } else {
                // 网络错误
                msg.what = 2;
                msg.arg2 = (isManual)? 1 : 0;
            }
        } else {
            msg.what = 1;
            msg.arg1 = platformID;
            msg.arg2 = (isManual)? 1 : 0;
            msg.obj = chapterItem;
        }
        ChapterActivity.handler_paragraph.sendMessage(msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Tag, "销毁了 章节加载服务");
    }
}
