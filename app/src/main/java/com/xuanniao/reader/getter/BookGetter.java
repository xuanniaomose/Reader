package com.xuanniao.reader.getter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.item.PlatformDB;
import com.xuanniao.reader.item.PlatformItem;
import com.xuanniao.reader.tools.Tools;
import com.xuanniao.reader.ui.book.LocalFragment;
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

public class BookGetter extends Service {
    static String Tag = "BookGetter";
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
            String bookUri = intent.getStringExtra("bookUri");
            Log.d(Tag, "bookName:" + bookName + " bookCode:" + bookCode);

            pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
            List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
            Log.d(Tag, "isLocal:" + isLocal + " | platformID:" + platformID);
            if (isLocal) {
                BookItem bookItem = FileTools.loadLocalBook(this, Uri.parse(bookUri));
                List<BookItem> bookList = new ArrayList<>();
                bookList.add(bookItem);
                platformID = (platformID > -1)? platformID :
                        Tools.getPlatformID(this, bookItem.getPlatformName());
                sendMessage(platformID, bookList, true, true);
            } else {
                if (platformID != -1) {
                    PlatformItem platformItem = platformList.get(platformID);
                    getHtml(this, platformItem, bookName, bookCode);
                } else {
                    for (PlatformItem platformItem : platformList) {
                        getHtml(this, platformItem, bookName, bookCode);
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
     * @param platformItem 平台
     * @param bookName 书名名称
     * @param bookCode 书籍代码
     */
    private void getHtml(Context context, PlatformItem platformItem, String bookName, String bookCode) {
        Log.d(Tag, "path:" + platformItem.getSearchPath());
        if (bookCode == null && platformItem.getSearchPath().endsWith("index.php")) {
            postHtml(platformItem, bookName);
            return;
        }
        String url = platformItem.getPlatformUrl();
        if (bookCode != null) {
            url = url + bookCode;
        } else {
            url = url + platformItem.getSearchPath() + bookName;
        }
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
                boolean isDetailed = getResultPage(platformItem);
                List<BookItem> bookList = htmlToBookList(platformItem, htmlContent);
                sendMessage(platformItem.getID(), bookList, isDetailed, false);
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
                    List<BookItem> bookList = htmlToBookList(platformItem, htmlContent);
                    boolean isDetailed = getResultPage(platformItem);
                    sendMessage(1, bookList, isDetailed, false);
                } catch (IOException e) {
//                    throw new RuntimeException(e);
                    Log.e(Tag, "e:" + e.getMessage());
                }
            }
        });
    }

    /**
     * 向网络发送内容
     * @param platformItem 平台
     * @param bookName 书籍名称
     */
    private void postHtml(PlatformItem platformItem, String bookName) {
        Log.d(Tag, "进入POST流程");
        String url = platformItem.getPlatformUrl();
        url = url +  platformItem.getSearchPath();
        Log.d(Tag, "url:" + url);
        OkHttpClient okHttpClient = new OkHttpClient();
        //注意：FormBody 类已经内置了静态常量来表示这个body的数据内容类型属于 application/x-www-form-urlencoded
        //所以，不用另外设置 MediaType
        try {
            Log.d(Tag, "charsetName:" + platformItem.getCharsetName());
            RequestBody requestBody = new FormBody.Builder()
                    //类似左边键名，右边键值
                    .add("keyboard", URLEncoder.encode(bookName, "GBK"))
                    .add("show", "title")
                    .add("classid", "0")
                    .build();
            Log.d(Tag, "keyboard:" + URLEncoder.encode(bookName, "GBK"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .headers(Filter.setHeaders(platformItem.getPlatformCookie()))
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.d(Tag, "onFailure: " + e.getMessage());
                }
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Log.d(Tag, "response.code() = " + response.code());
                    if (response.body() != null) {
                        Log.d(Tag, "response.body() = " + response.body().string());
                    }
                    Log.d(Tag, "response.message() = " + response.message());
                }
            });
        } catch (UnsupportedEncodingException e) {
            Log.e(Tag, "e:" + e.getMessage());
        }
    }

    @NonNull
    private List<BookItem> htmlToBookList(PlatformItem platformItem, String htmlContent) {
        List<BookItem> bookList = new ArrayList<>();
//        Log.d(Tag, "htmlContent:" + htmlContent);
        String resultError = platformItem.getResultError();
        if (htmlContent.contains(resultError) || htmlContent.equals("0")) {
            Log.d(Tag, "请求失败");
            return setEmptyList(bookList);
        } else {
            Log.d(Tag, "开始匹配BookList");

            String[] resultPage = platformItem.getResultPage();

            Log.d(Tag, "resultPage:" + Arrays.toString(resultPage));
            JSONObject resultFormatJson = null;
            try {
                resultFormatJson = JSONObject.parseObject(platformItem.getResultPageFormat());
                if (resultFormatJson == null) return setEmptyList(bookList);
                JSONArray findList = resultFormatJson.getJSONArray("resultList");
                Document doc = Jsoup.parseBodyFragment(htmlContent);
                Elements bookAttrs = Filter.switchActionToElements(findList, doc);
                if (bookAttrs != null) {
                    for (Element bookAttr : bookAttrs) {
                        BookItem bookItem = Filter.getBookItem(resultPage, resultFormatJson, bookAttr);
                        bookItem.setPlatformName(platformItem.getPlatformName());
                        bookList.add(bookItem);
                    }
                } else {
                    Map<String, String> map = Filter.switchActionToMap(findList, doc);
                    BookItem bookItem = Filter.getBookItemByMap(resultPage, resultFormatJson, map);
                    bookItem.setPlatformName(platformItem.getPlatformName());
                    bookList.add(bookItem);
                }
            } catch (JSONException e) {
                Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                return setEmptyList(bookList);
            }
            Log.d(Tag, "结束匹配BookList");
        }
        return bookList;
    }

    private boolean getResultPage(PlatformItem platformItem) {
        String[] resultPage = platformItem.getResultPage();
        for (String result : resultPage) {
            if (Objects.equals(result, "synopsis")) return true;
        }
        return false;
    }

    private List<BookItem> setEmptyList(List<BookItem> bookList) {
        BookItem bookItem = new BookItem();
        bookItem.setBookName("");
        bookList.add(bookItem);
        return bookList;
    }

    private void sendMessage(int platformID, List<BookItem> bookList, boolean isDetailed, boolean isLocal) {
        Message msg = new Message();
        Log.d(Tag, "解读出来是书籍列表");
        Log.d(Tag, "platformID:" + platformID);
        Log.d(Tag, "bookList.isEmpty():" + bookList.isEmpty());
        Log.d(Tag, "BookName:" + bookList.get(0).getBookName());
        if (bookList.isEmpty() || Objects.equals(bookList.get(0).getBookName(), "")) {
            // 网络错误
            msg.what = 2;
        } else {
            msg.what = 1;
            msg.arg1 = platformID;
            msg.arg2 = (isDetailed)? 1 : 0;
            msg.obj = bookList;
        }
        if (isLocal) {
            LocalFragment.handler_local.sendMessage(msg);
        } else {
            SearchFragment.handler_setResult.sendMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Tag, "销毁了 书目加载服务");
    }
}
