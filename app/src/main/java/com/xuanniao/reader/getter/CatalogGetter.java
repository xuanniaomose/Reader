package com.xuanniao.reader.getter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.item.CatalogItem;
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

public class CatalogGetter extends Service {
    static String Tag = "CatalogGetter";
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
            boolean isLocal = intent.getBooleanExtra("isLocal", true);
            int platformID = intent.getIntExtra("platformID", -1);
            BookItem bookItem = (BookItem) intent.getSerializableExtra("bookItem");
            String bookName = bookItem.getBookName();
            String bookCode = bookItem.getBookCode();
            Log.d(Tag, "bookName:" + bookName + "bookCode:" + bookCode);

            pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
            List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
            Log.d(Tag, "isLocal:" + isLocal + " | platformID:" + platformID);
            if (isLocal) {
                CatalogItem catalogItem = FileTools.loadLocalCatalog(this, bookName);
                if (catalogItem == null) return;
                sendMessage(1, catalogItem, 0);
            } else {
                if (platformID != -1) {
                    PlatformItem platformItem = platformList.get(platformID);
                    getHtml(this, platformItem, bookItem);
                } else {
                    for (PlatformItem platformItem : platformList) {
                        getHtml(this, platformItem, bookItem);
                    }
                }
            }
            // 本地测试
//            PlatformItem platformItem = platformList.get(platformID);
//            FileTools.loadLocalAssets(this, platformItem, bookName);
            stopSelf();
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 从网络获取内容
     * @param context 上下文
     * @param platformItem 平台
     * @param bookItem 书目
     */
    private void getHtml(Context context, PlatformItem platformItem, BookItem bookItem) {
        if (bookItem.getBookCode() == null || platformItem.getCatalogPath() == null) return;
        String url = platformItem.getPlatformUrl() + bookItem.getBookCode()
                + "/" + platformItem.getCatalogPath();
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
                getFailure(platformItem.getPlatformName(), platformItem.getID());
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String htmlContent = responseToContent(context, response.body(),
                        platformItem.getCharsetName(), platformItem.getCatalogError());
                htmlToCatalog(context, platformItem, bookItem.getBookCode(),
                        htmlContent, 0);
            }
        });
    }

    void htmlToCatalog(Context context, PlatformItem platformItem, String bookCode,
                       String htmlContent, int part) {
//        Log.d(Tag, "htmlContent:" + htmlContent);
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setBookCode(bookCode);
        catalogItem.setPlatformName(platformItem.getPlatformName());
        Log.d(Tag, "开始匹配CatalogList");
        JSONObject catalogFormatJson;
        Document doc = Jsoup.parse(htmlContent);
        try {
            catalogFormatJson = JSONObject.parseObject(platformItem.getCatalogPageFormat());
            if (catalogFormatJson == null) return;
            JSONArray findPage = catalogFormatJson.getJSONArray("catalogPartPage");
            if (part == 0 && findPage != null) {
                Log.d(Tag, "正在解析首页:" + part);
                Elements catalogPages = Filter.switchActionToElements(findPage, doc);
                if (catalogPages == null) return;
                JSONArray pageCodeStep = catalogFormatJson.getJSONArray("catalogPageCode");
                JSONArray pageNameStep = catalogFormatJson.getJSONArray("catalogPageName");
                int total = catalogPages.size();
                for (int i = 0; i < total; i++) {
                    Element pageAttr = catalogPages.get(i);
                    String pageCode = Filter.getAttr(pageCodeStep, pageAttr);
                    String pageName = Filter.getAttr(pageNameStep, pageAttr);
                    // 根据pageCode下载各个部分的目录
                    getPartPageHtml(context, platformItem, bookCode,
                            pageCode, (i + 1) * 100 + total);
                }
            } else {
                Log.d(Tag, "正在解析part:" + part);
                catalogItem = getCatalogItem(catalogFormatJson, doc, catalogItem);
            }
        } catch (JSONException e) {
            Log.e(Tag, "e:" + e.getMessage());
            return;
        }
        Log.d(Tag, "结束匹配CatalogList");
        if (catalogItem != null) sendMessage(1, catalogItem, part);
    }

    private static CatalogItem getCatalogItem(JSONObject fJson, Document doc, CatalogItem item) {
        JSONArray findList = fJson.getJSONArray("catalogList");

        JSONArray bookNameStep = fJson.getJSONArray("bookName");
        String bookName = Filter.getAttr(bookNameStep, doc.head());
        item.setBookName(bookName);

        Elements catalogAttrs = Filter.switchActionToElements(findList, doc);
        if (catalogAttrs == null) return null;
        JSONArray chapterCodeStep = fJson.getJSONArray("chapterCode");
        JSONArray chapterTitleStep = fJson.getJSONArray("chapterTitle");
        for (Element catalogAttr : catalogAttrs) {
            Log.d(Tag, "catalogAttr:" + catalogAttr.text());
            String chapterCode = Filter.getAttr(chapterCodeStep, catalogAttr);
            item.addChapterCode(chapterCode);
            String chapterTitle = Filter.getAttr(chapterTitleStep, catalogAttr);
            item.addChapterTitle(chapterTitle);
            Log.d(Tag, "chapterTitle:" + chapterTitle + "  chapterCode:" + chapterCode);
        }
        return item;
    }

    void getPartPageHtml(Context context, PlatformItem item, String bookCode,
                         String pageCode, int part) {
        String strUrl = item.getPlatformUrl() + bookCode + "/" + pageCode;
        Log.d("url", strUrl);
        String cookie = item.getPlatformCookie();
        Log.d(Tag, "Cookie:" + cookie);
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
                getFailure(item.getPlatformName(), item.getID());
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String htmlContent = responseToContent(context, response.body(),
                        item.getCharsetName(), item.getCatalogError());
                htmlToCatalog(context, item, bookCode, htmlContent, part);
            }
        });
    }

    private void getFailure(String platformName, int platformID) {
        Log.d(Tag, "请求失败onFailure");
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setPlatformName(platformName);
        catalogItem.addChapterCode("");
        sendMessage(platformID, catalogItem, 0);
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
                Log.d(Tag, "charsetName:" + charsetName);
                htmlContent = new String(responseBody.bytes(), charsetName);
                Log.d(Tag, "htmlContent:" + htmlContent);
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

    private void sendMessage(int platformID, CatalogItem catalogItem, int part) {
        Message msg = new Message();
        Log.d(Tag, "解读出来是目录列表");
        if (catalogItem.getChapterCodeList() == null ||
                catalogItem.getChapterCodeList().isEmpty() ||
                Objects.equals(catalogItem.getChapterCodeList().get(0), "")) {
            // 网络错误
            msg.what = 2;
        } else {
            msg.what = 1;
            msg.arg1 = platformID;
            msg.arg2 = part;
            msg.obj = catalogItem;
        }
        CatalogActivity.handler_catalog.sendMessage(msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Tag, "销毁了 目录加载服务");
    }
}
