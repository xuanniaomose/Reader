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
import com.xuanniao.reader.item.PlatformDB;
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.item.PlatformItem;
import com.xuanniao.reader.ui.book.LocalFragment;
import com.xuanniao.reader.ui.book.SearchFragment;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class InfoGetter extends Service {
    static String Tag = "InfoGetter";
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
            boolean isNeedSave = intent.getBooleanExtra("isNeedSave", false);
            boolean isLocal = intent.getBooleanExtra("isLocal", false);
            int platformID = intent.getIntExtra("platformID", -1);
            int num = intent.getIntExtra("num", 0);
            String bookName = intent.getStringExtra("bookName");
            String bookCode = intent.getStringExtra("bookCode");
//            Log.d(Tag, "bookName:" + bookName + " bookCode:" + bookCode);

            pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
            List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
            if (platformID != -1) {
                PlatformItem platformItem = platformList.get(platformID);
                getHtml(this, platformItem, num, bookCode, isNeedSave, isLocal);
            } else {
                for (PlatformItem platformItem : platformList) {
                    getHtml(this, platformItem, num, bookCode, isNeedSave, isLocal);
                }
            }
            // 本地测试
//            Log.d(Tag, "platformID:" + platformID);
//            PlatformItem platformItem = platformList.get(platformID);
//            String htmlContent = FileTools.loadLocalAssets(this, bookCode);
//            BookItem bookItem = htmlToBookInfo(platformItem, htmlContent);
//            bookItem.setBookCode(bookCode);
//            sendMessage(platformID, num, bookItem);
//            if (isNeedSave) FileTools.infoSave(this, bookItem);

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
    private void getHtml(Context context, PlatformItem platformItem, int num,
                         String bookCode, boolean isCreate, boolean isLocal) {
        String url = platformItem.getPlatformUrl() + "/" + bookCode + platformItem.getInfoPath();
        Log.d("url", url);
        String cookie = platformItem.getPlatformCookie();
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
                BookItem bookItem = htmlToBookInfo(platformItem, htmlContent);
                sendMessage(platformItem.getID(), num, bookItem, isLocal);
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
//                        Log.d(Tag, "charsetName:" + charsetName);
                        htmlContent = new String(response.body().bytes(), charsetName);
//                        Log.d(Tag, "htmlContent:" + htmlContent);
                    } else {
                        Log.d(Tag, "请求失败 没有返回值");
                        htmlContent = "0";
                    }
                    BookItem bookItem = htmlToBookInfo(platformItem, htmlContent);
                    bookItem.setBookCode(bookCode);
                    sendMessage(platformItem.getID(), num, bookItem, isLocal);
                    if (isCreate) FileTools.infoSave(context, bookItem, cookie);
                } catch (IOException e) {
//                    throw new RuntimeException(e);
                    Log.e(Tag, "e:" + e.getMessage());
                }
            }
        });
    }

    private BookItem htmlToBookInfo(PlatformItem platformItem, String htmlContent) {
        BookItem bookItem = new BookItem();
        bookItem.setBookName("");
//        Log.d(Tag, "htmlContent:" + htmlContent);
        String infoError = platformItem.getInfoError();
        if (htmlContent.contains(infoError) || htmlContent.equals("0")) {
            Log.d(Tag, "请求失败");
        } else {
//            Log.d(Tag, "开始匹配bookInfo");
            String[] infoPage = platformItem.getInfoPage();
//            Log.d(Tag, "infoPage:" + Arrays.toString(infoPage));
            JSONObject infoFormatJson = null;
            try {
                infoFormatJson = JSONObject.parseObject(platformItem.getInfoPageFormat());
                if (infoFormatJson == null) return bookItem;
                Document doc = Jsoup.parseBodyFragment(htmlContent);

                JSONArray findList = infoFormatJson.getJSONArray("infoList");
                Element infoAttr = Filter.switchActionToElement(findList, doc);
                Log.d(Tag, "infoAttr:" + (infoAttr != null));
                if (infoAttr != null) {
                    bookItem = Filter.getBookItem(infoPage, infoFormatJson, infoAttr);
                } else {
                    Map<String, String> map = Filter.switchActionToMap(findList, doc);
                    bookItem = Filter.getBookItemByMap(infoPage, infoFormatJson, map);
                }
                bookItem.setPlatformName(platformItem.getPlatformName());

                JSONArray synopsisStep = infoFormatJson.getJSONArray("synopsis");
                String synopsis = Filter.getAttr(synopsisStep, doc.body());
                bookItem.setSynopsis(synopsis);

                JSONArray coverUrlStep = infoFormatJson.getJSONArray("coverUrl");
                if (coverUrlStep != null) {
                    String coverUrl = Filter.getAttr(coverUrlStep, doc.body());
                    bookItem.setCoverUrl(coverUrl);
                }
            } catch (JSONException e) {
                Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                return bookItem;
            }
            Log.d(Tag, "结束匹配BookList");
        }
        return bookItem;
    }

    private void sendMessage(int platformID, int num, BookItem bookItem, boolean isLocal) {
        Message msg = new Message();
        Log.d(Tag, "解读出来是书目详情");
        if (Objects.equals(bookItem.getBookName(), "")) {
            // 网络错误
            msg.what = 2;
        } else {
            msg.what = 1;
            msg.arg1 = platformID;
            msg.arg2 = num;
            msg.obj = bookItem;
        }
        if (isLocal) {
            LocalFragment.handler_local.sendMessage(msg);
        } else {
            SearchFragment.handler_info.sendMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Tag, "销毁了 详情加载服务");
    }
}
