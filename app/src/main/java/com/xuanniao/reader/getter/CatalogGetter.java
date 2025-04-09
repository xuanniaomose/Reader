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
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.tools.PlatformDB;
import com.xuanniao.reader.ui.*;
import com.xuanniao.reader.ui.book.PlatformItem;
import com.xuanniao.reader.ui.book.SearchFragment;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CatalogGetter extends Service {
    static String Tag = "BookGet";
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
            String bookName = intent.getStringExtra("bookName");
            String bookCode = intent.getStringExtra("bookCode");
            Log.d(Tag, "bookName:" + bookName + "bookCode:" + bookCode);

            pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
            List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
            Log.d(Tag, "isLocal:" + isLocal + " | platformID:" + platformID);
            if (isLocal) {
                CatalogItem catalogItem = FileTools.loadLocalCatalog(this, bookName);
                sendMessage(1, catalogItem);
            } else {
                if (platformID != -1) {
                    PlatformItem platformItem = platformList.get(platformID);
                    getHtml(this, platformItem, bookCode);
                } else {
                    for (PlatformItem platformItem : platformList) {
                        getHtml(this, platformItem, bookCode);
                    }
                }
            }
//            if (isAssets) {
//                if (platformID != -1) {
//                    PlatformItem platformItem = platformList.get(platformID);
//                    loadLocalAssets(this, platformItem, bookName);
//                } else {
//                    for (PlatformItem platformItem : platformList) {
//                        loadLocalAssets(this, platformItem, bookName);
//                    }
//                }
//            }
            stopSelf();
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 从网络获取内容
     * @param context 上下文
     * @param platformItem 平台
     * @param bookCode 书籍编号
     */
    private void getHtml(Context context, PlatformItem platformItem, String bookCode) {
        if (bookCode == null || platformItem.getCatalogPath() == null) return;
        String url = platformItem.getPlatformUrl() + bookCode + platformItem.getCatalogPath();
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
                CatalogItem catalogItem = htmlToCatalog(platformItem, htmlContent);
                sendMessage(platformItem.getID(), catalogItem);
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
                    CatalogItem catalogItem = htmlToCatalog(platformItem, htmlContent);
                    sendMessage(1, catalogItem);
                    if (catalogItem != null && catalogItem.getChapterCodeList() != null
                            && !catalogItem.getChapterCodeList().isEmpty()) {
                        catalogItem.setBookCode(bookCode);
                        FileTools.newBook(context, catalogItem);
                    }
                } catch (IOException e) {
//                    throw new RuntimeException(e);
                    Log.e(Tag, "e:" + e.getMessage());
                }
            }
        });
    }

    static CatalogItem htmlToCatalog(PlatformItem platformItem, String htmlContent) {
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setPlatformName(platformItem.getPlatformName());
//        Log.d(Tag, "htmlContent:" + htmlContent);
        String catalogError = platformItem.getCatalogError();
        if (htmlContent == null || htmlContent.contains(catalogError) || htmlContent.equals("0")) {
            Log.d(Tag, "请求失败");
            catalogItem.addChapterCode("");
        } else {
            Log.d(Tag, "开始匹配CatalogList");
            String[] catalogPage = platformItem.getCatalogPage();
//                Log.d(Tag, "catalogPage:" + catalogPage);
            JSONObject catalogFormatJson = null;
            try {
                catalogFormatJson = JSONObject.parseObject(platformItem.getCatalogPageFormat());
                if (catalogFormatJson == null) return null;
                JSONArray findList = catalogFormatJson.getJSONArray("catalogList");
                Document doc = Jsoup.parse(htmlContent);
//                    Document doc = Jsoup.parseBodyFragment(htmlContent);
                JSONArray bookNameStep = catalogFormatJson.getJSONArray("bookName");
                String bookName = Filter.getAttr(bookNameStep, doc.head());
                catalogItem.setBookName(bookName);

                JSONArray authorStep = catalogFormatJson.getJSONArray("author");
                if (authorStep != null) {
                    String author = Filter.getAttr(authorStep, doc.head());
                }

                Elements catalogAttrs = Filter.switchActionToElements(findList, doc);
                if (catalogAttrs == null) return null;
                JSONArray chapterCodeStep = catalogFormatJson.getJSONArray("chapterCode");
                JSONArray chapterTitleStep = catalogFormatJson.getJSONArray("chapterTitle");
                for (Element catalogAttr : catalogAttrs) {
                    Log.d(Tag, "catalogAttr:" + catalogAttr.text());
                    String chapterCode = Filter.getAttr(chapterCodeStep, catalogAttr);
                    catalogItem.addChapterCode(chapterCode);
                    String chapterTitle = Filter.getAttr(chapterTitleStep, catalogAttr);
                    catalogItem.addChapterTitle(chapterTitle);
                    Log.d(Tag, "chapterTitle:" + chapterTitle + "  chapterCode:" + chapterCode);
                }
            } catch (JSONException e) {
//                    Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                Log.e(Tag, "e:" + e.getMessage());
                return null;
            }
            Log.d(Tag, "结束匹配CatalogList");
//                figures = String.valueOf(catalogItem.getChapterCodeList().size()).length();
//                figures = (figures < 3)? 4 : figures;
        }
        return catalogItem;
    }

    private void sendMessage(int platformID, CatalogItem catalogItem) {
        Message msg = new Message();
        Log.d(Tag, "解读出来是目录列表");
        if (Objects.equals(catalogItem.getChapterCodeList().get(0), "")) {
            // 网络错误
            msg.what = 2;
        } else {
            msg.what = 1;
            msg.arg1 = platformID;
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
