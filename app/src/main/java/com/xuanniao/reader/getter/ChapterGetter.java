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
            int part = intent.getIntExtra("part", 0);
            String pageCode = intent.getStringExtra("pageCode");
            Log.d(Tag, "bookCode:" + bookCode + " chapterCode:" + chapterCode);

            pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
            List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
            Log.d(Tag, "isLocal:" + isLocal + " | platformID:" + platformID);
            if (isLocal) {
                ChapterItem chapterItem = FileTools.loadLocalChapter(
                        this, bookName, chapterNum, chapterTitle);
                sendMessage(isManual, chapterItem, 0);
            } else {
                ChapterItem chapterItem = new ChapterItem();
                chapterItem.setBookName(bookName);
                chapterItem.setBookCode(bookCode);
                chapterItem.setTitle(chapterTitle);
                chapterItem.setChapterNum(chapterNum);
                chapterItem.setChapterCode(chapterCode);
                Log.d(Tag, "chapterNum:" + chapterNum + " | chapterCode:" + chapterCode);
                if (platformID == -1) {
                    PlatformItem platformItem = platformList.get(platformID);
                    if (pageCode == null) {
                        getHtml(this, isManual, platformItem, chapterItem);
                    } else {
                        getPartPageHtml(this, isManual, platformItem,
                                chapterItem, pageCode, part);
                    }
                } else {
                    for (PlatformItem platformItem : platformList) {
                        getHtml(this, isManual, platformItem, chapterItem);
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
     * @param chapterItem 输入的章节
     */
    private void getHtml(Context context, boolean isManual, PlatformItem platformItem, ChapterItem chapterItem) {
        String url = platformItem.getPlatformUrl() + chapterItem.getBookCode()
                + "/" + chapterItem.getChapterCode() + ".html";
        Log.d("url", url);
        String cookie = platformItem.getPlatformCookie();
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
                Log.d(Tag, "请求失败onFailure");
                sendMessage(isManual, chapterItem, 0);
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String htmlContent = responseToContent(context, response.body(),
                        platformItem.getCharsetName(), platformItem.getChapterError());
                htmlToChapter(isManual, platformItem, htmlContent, chapterItem, 0);

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
                    htmlToChapter(isManual, platformItem, htmlContent, chapterItem, 0);
                } catch (IOException e) {
                    Log.e(Tag, "e:" + e.getMessage());
                    sendMessage(isManual, chapterItem, 0);
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
    private void htmlToChapter(boolean isManual, PlatformItem platformItem, String htmlContent,
                               ChapterItem chapterItem, int part) {
//        Log.d(Tag, "htmlContent:" + htmlContent);
        Log.d(Tag, "开始匹配ChapterList");
        String[] chapterPage = platformItem.getChapterPage();
//        Log.d(Tag, "chapterPage:" + Arrays.toString(chapterPage));
        JSONObject chapterFormatJson = null;
        try {
            chapterFormatJson = JSONObject.parseObject(platformItem.getChapterPageFormat());
            if (chapterFormatJson == null) return;

            Document doc = Jsoup.parseBodyFragment(htmlContent);
            JSONArray titleStep = chapterFormatJson.getJSONArray("title");
            String title = Filter.getAttr(titleStep, doc.body());
            if (title == null) return;
            chapterItem.setTitle(title);
            JSONArray findPage = chapterFormatJson.getJSONArray("chapterPartPage");
            if (part == 0 && findPage != null) {
                Elements chapterPages = Filter.switchActionToElements(findPage, doc);
                if (chapterPages == null) return;
                JSONArray pageCodeStep = chapterFormatJson.getJSONArray("chapterPageCode");
                JSONArray pageNameStep = chapterFormatJson.getJSONArray("chapterPageName");
                int total = chapterPages.size();
                Log.d(Tag, "chapterPages: " + chapterPages);
                for (int i = 0; i < total; i++) {
                    Element pageAttr = chapterPages.get(i);
                    String pageCode = Filter.getAttr(pageCodeStep, pageAttr);
                    String pageName = Filter.getAttr(pageNameStep, pageAttr);
                    chapterItem.addChapterPageCode(pageCode);
                }
                part = (part + 1) * 100 + total;
            }
            JSONArray pageIndex = chapterFormatJson.getJSONArray("chapterPartPageIndex");
            if (part == 0 && pageIndex != null) {
                String chapterPath = platformItem.getChapterPath();
                boolean haveHtml = chapterPath.contains(".html");
                if (haveHtml) chapterPath = chapterPath.replace(".html", "");
                chapterPath = chapterPath.replaceAll("[0-9]", "");
                int total = pageIndex.size();
                for (int i = 0; i < total; i++) {
                    int index = (int) pageIndex.get(i);
                    String pageCode = (haveHtml)? chapterPath + index + ".html" :
                            chapterPath + index;
                    chapterItem.addChapterPageCode(pageCode);
                }
                part = (part + 1) * 100 + total;
            }
            boolean containP = Arrays.asList(chapterPage).contains("paragraph");
            chapterItem = getChapterItem(chapterFormatJson, containP, doc, chapterItem);
        } catch (JSONException e) {
            Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(Tag, "结束匹配ChapterList");
        sendMessage(isManual, chapterItem, part);
    }

    private static ChapterItem getChapterItem(JSONObject fJson, boolean containP, Document doc, ChapterItem item){
        JSONArray findList = fJson.getJSONArray("paragraphList");
        if (containP) {
            Elements chapterAttrs = Filter.switchActionToElements(findList, doc);
            if (chapterAttrs == null) return item;
            JSONArray paragraphStep = fJson.getJSONArray("paragraph");
            for (Element chapterAttr : chapterAttrs) {
                String paragraph = Filter.getAttr(paragraphStep, chapterAttr);
                item.addParagraph(paragraph);
            }
        } else {
            String[] paragraphArray = Filter.switchActionToParagraphArray(findList, doc);
            if (paragraphArray == null) return item;
            for (String chapterAttr : paragraphArray) {
                if (Objects.equals(chapterAttr.trim(), "")) continue;
                if (Objects.equals(chapterAttr.trim(), "<script>chaptererror();</script>")) continue;
                if (chapterAttr.trim().contains("请记住本书首发域名")) continue;
                if (chapterAttr.trim().contains("<!--")) continue;
                item.addParagraph(chapterAttr.trim());
            }
        }
        return item;
    }

    void getPartPageHtml(Context context, boolean isManual, PlatformItem platformItem,
                         ChapterItem chapterItem, String pageCode, int part) {
        String strUrl = platformItem.getPlatformUrl() + chapterItem.getChapterCode()
                + "/" + pageCode;
        Log.d("url", strUrl);
        String cookie = platformItem.getPlatformCookie();
//        Log.d(Tag, "Cookie:" + cookie);
        Request request = new Request.Builder()
                .url(strUrl)
                .headers(Filter.setHeaders(cookie))
                .build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d(Tag, "请求失败onFailure");
                sendMessage(isManual, chapterItem, 0);
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String htmlContent = responseToContent(context, response.body(),
                        platformItem.getCharsetName(), platformItem.getCatalogError());
                htmlToChapter(isManual, platformItem, htmlContent, chapterItem, part);
            }
        });
    }

    private String responseToContent(Context context, okhttp3.ResponseBody responseBody,
                                     String charsetName, String catalogError) {
        String htmlContent;
        try {
            if (responseBody != null) {
                Log.d(Tag, "请求成功");
                if (charsetName == null || charsetName.isEmpty()) {
                    charsetName = PreferenceManager.getDefaultSharedPreferences(context)
                            .getString("charsetName", "UTF-8");
                }
//                Log.d(Tag, "charsetName:" + charsetName);
                htmlContent = new String(responseBody.bytes(), charsetName);
//                Log.d(Tag, "htmlContent:" + htmlContent);
            } else {
                Log.d(Tag, "请求失败 没有返回值");
                htmlContent = "0";
            }
            if (htmlContent.contains(catalogError)) {
                Log.d(Tag, "请求失败");
                htmlContent = "0";
            }
        } catch (IOException e) {
//            throw new RuntimeException(e);
            htmlContent = "0";
        }
        return htmlContent;
    }

    private void sendMessage(boolean isManual, ChapterItem chapterItem, int part) {
        Message msg = new Message();
        Log.d(Tag, "解读出来是段落列表");
        if (chapterItem.getChapter() == null || chapterItem.getChapter().isEmpty()) {
            if (chapterItem.getIsLocal()) {
                // 本地没有
                msg.what = 3;
                msg.arg1 = (isManual)? 1 : 0;
                msg.arg2 = part;
                msg.obj = chapterItem;
            } else {
                // 网络错误
                msg.what = 2;
                msg.arg1 = (isManual)? 1 : 0;
            }
        } else {
            msg.what = 1;
            msg.arg1 = (isManual)? 1 : 0;
            msg.arg2 = part;
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
