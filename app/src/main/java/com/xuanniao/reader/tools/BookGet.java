package com.xuanniao.reader.tools;

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
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BookGet extends Service {
    static String Tag = "BookGet";
    PlatformDB pdb;
    static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/" +
            "537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0";
    static String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/" +
            "avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";
    static int figures = 4; // 章节号补零后的位数

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
            int isLocal = intent.getIntExtra("isLocal", 0);
            int platformID = intent.getIntExtra("platformID", -1);
            String bookName = intent.getStringExtra("bookName");
            String bookCode = intent.getStringExtra("bookCode");
            String chapterCode = intent.getStringExtra("chapterCode");
            int chapterNum = intent.getIntExtra("chapterNum", -1);
            String chapterTitle = intent.getStringExtra("chapterTitle");
            Log.d(Tag, "bookCode:" + bookCode + " chapterCode:" + chapterCode);
            pdb = PlatformDB.getInstance(this, Constants.DB_PLATFORM);
            List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
            if (isLocal == 1) {
                if (chapterCode == null) {
                    CatalogItem catalogItem = FileTools.loadLocalCatalog(this, bookName);
                    sendMessage(1, isManual, null, catalogItem, null);
                } else {
                    ChapterItem chapterItem = FileTools.
                            loadLocalChapter(this, bookName, chapterNum, chapterTitle);
                    sendMessage(1, isManual, null, null, chapterItem);
                }
            } else if (isLocal == 2) {
                if (platformID != -1) {
                    PlatformItem platformItem = platformList.get(platformID);
                    loadLocalAssets(this, isManual, platformItem, bookName, bookCode, chapterNum, chapterCode);
                } else {
                    for (PlatformItem platformItem : platformList) {
                        loadLocalAssets(this, isManual, platformItem, bookName, bookCode, chapterNum, chapterCode);
                    }
                }
            } else {
                Log.d(Tag, "开始获取页面 " + "isLocal:" + isLocal + " | platformID:" + platformID);
                if (platformID != -1) {
                    PlatformItem platformItem = platformList.get(platformID);
                    Log.d(Tag, "chapterNum:" + chapterNum + " | chapterCode:" + chapterCode);
                    getHtml(this, isManual, platformItem, bookName, bookCode, chapterNum, chapterCode, chapterTitle);
                } else {
                    for (PlatformItem platformItem : platformList) {
                        getHtml(this, isManual, platformItem, bookName, bookCode, chapterNum, chapterCode, chapterTitle);
                    }
                }
            }
            stopSelf();
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    public static Headers setHeaders(String cookie) {
        Log.d(Tag, "cookie:" + cookie);
        String[] kv = cookie.split("=");
        Headers headers;
        Headers.Builder headersbuilder = new Headers.Builder();
        headersbuilder.add("User-Agent", userAgent);
        headersbuilder.add("Accept", accept);
        headersbuilder.add(kv[0], kv[1]);
        headers = headersbuilder.build();
        Log.d(Tag, String.valueOf(headers));
        return headers;
    }

    public static Headers setSearchHeaders(String cookie) {
        Headers headers;
        Headers.Builder headersbuilder = new Headers.Builder();
        headersbuilder.add("User-Agent", userAgent);
        headersbuilder.add("Accept", accept);
        String[] kva = cookie.split("; ");
        for (String kvs : kva) {
            String[] kv = kvs.split("=");
            Log.d(Tag, kv[0] + ":" + kv[1]);
            headersbuilder.add(kv[0], kv[1]);
        }headers = headersbuilder.build();
        Log.d(Tag, String.valueOf(headers));
        return headers;
    }

    /**
     * 从网络获取内容
     * @param bookName 书籍名称
     * @param bookCode 书籍编号
     * @param chapterCode 章节编号
     */
    private void getHtml(Context context, boolean isManual, PlatformItem platformItem, String bookName,
                         String bookCode, int chapterNum, String chapterCode, String chapterTitle) {
        Log.d(Tag, "path:" + platformItem.getSearchPath());
        if (bookCode == null && platformItem.getSearchPath().endsWith("index.php")) {
            postHtml(platformItem, bookName);
            return;
        }
        String url;
        if (bookCode != null) {
            url = platformItem.getPlatformUrl() + bookCode;
        } else {
            url = platformItem.getSearchPath() + bookName;
        }
        if (chapterCode != null) {
            url = url + "/" + chapterCode + ".html";
        } else {
            url = url + platformItem.getCatalogPath();
//            url = url  + ".html";
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
                .headers(setSearchHeaders(cookie))
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
                if (chapterCode == null) {
                    if (bookCode == null) {
                        List<BookItem> resultList = htmlToBookList(platformItem, bookName, htmlContent);
                        sendMessage(platformItem.getID(), isManual, resultList, null, null);
                    } else {
                        CatalogItem catalogItem = htmlToCatalog(platformItem, htmlContent);
                        sendMessage(platformItem.getID(), isManual, null, catalogItem, null);
                    }
                } else {
                    chapterSend(context, isManual, platformItem, htmlContent, bookName, chapterCode, chapterTitle, chapterNum);
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String htmlContent;
                try {
                    if (response.body() != null) {
                        Log.d(Tag, "请求成功");
                        String charsetName = platformItem.getCharsetName();
//                        Log.d(Tag, "Supported charsets:");
//                        SortedMap<String, Charset> charsetsMap = Charset.availableCharsets();
//                        for (String name : charsetsMap.keySet()) {
//                            Log.d(Tag, name);
//                        }
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
                    if (chapterCode == null) {
                        if (bookCode == null) {
                            List<BookItem> resultList = htmlToBookList(platformItem, bookName, htmlContent);
                            sendMessage(1, isManual, resultList, null, null);
                        } else {
                            CatalogItem catalogItem = htmlToCatalog(platformItem, htmlContent);
                            sendMessage(1, isManual, null, catalogItem, null);
                            if (catalogItem != null && catalogItem.getChapterCodeList() != null
                                    && !catalogItem.getChapterCodeList().isEmpty()) {
                                catalogItem.setBookCode(bookCode);
                                FileTools.newBook(context, catalogItem);
                            }
                        }
                    } else {
                        chapterSend(context, isManual, platformItem, htmlContent,
                                bookName, chapterCode, chapterTitle, chapterNum);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
//                    Log.e(Tag, "e:" + e.getMessage());
                }
            }
        });
    }

    /**
     * 向网络发送内容
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
                    .headers(setSearchHeaders(platformItem.getPlatformCookie()))
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d(Tag, "onFailure: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d(Tag, "response.code() = " + response.code());
                    Log.d(Tag, "response.body() = " + response.body().string());
                    Log.d(Tag, "response.message() = " + response.message());
                }
            });
        } catch (UnsupportedEncodingException e) {
            Log.e(Tag, "e:" + e.getMessage());
        }
    }

    private List<BookItem> htmlToBookList(PlatformItem platformItem, String searchText, String htmlContent) {
        List<BookItem> bookList = new ArrayList<>();
//        Log.d(Tag, "htmlContent:" + htmlContent);
        String resultError = platformItem.getResultError();
        if (htmlContent.contains(resultError) || htmlContent.equals("0")) {
            Log.d(Tag, "请求失败");
            BookItem bookItem = new BookItem();
            bookItem.setBookName("");
            bookList.add(bookItem);
        } else {
            Log.d(Tag, "开始匹配BookList");
            if (htmlContent != null) {
                String[] resultPage = platformItem.getResultPage();
                Log.d(Tag, "resultPage:" + Arrays.toString(resultPage));
                JSONObject resultFormatJson = null;
                try {
                    resultFormatJson = JSONObject.parseObject(platformItem.getResultPageFormat());
                    if (resultFormatJson == null) return null;
                    JSONArray findList = resultFormatJson.getJSONArray("resultList");
                    Document doc = Jsoup.parseBodyFragment(htmlContent);
                    Elements bookAttrs = switchActionToElements(findList, doc);
                    if (bookAttrs != null) {
                        for (Element bookAttr : bookAttrs) {
                            BookItem bookItem = getBookItem(resultPage, resultFormatJson, bookAttr);
                            bookItem.setPlatformName(platformItem.getPlatformName());
                            bookList.add(bookItem);
                        }
                    } else {
                        Map<String, String> map = switchActionToMap(findList, doc);
                        BookItem bookItem = getBookItemByMap(resultPage, resultFormatJson, map);
                        bookItem.setPlatformName(platformItem.getPlatformName());
                        bookList.add(bookItem);
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                    return null;
                }
                Log.d(Tag, "结束匹配BookList");
            }
        }
        return bookList;
    }

    private BookItem getBookItem(String[] attrArray, JSONObject pageJson, Element htmlListItem) {
        BookItem bookItem = new BookItem();
//        Log.d(Tag, "pageJson:" + pageJson);
        for (int i = 1; i < attrArray.length; i++) {
            String attr = attrArray[i];
            if (attr == null) continue;
            JSONArray attrStepJson = pageJson.getJSONArray(attr);
            String str = getAttr(attrStepJson, htmlListItem);
//            Log.d(Tag, "str:" + str);
            if (str == null) continue;
            switch (attr) {
                case "resultList":
                    continue;
                case "bookCode":
                    bookItem.setBookCode(str);
                    break;
                case "bookName":
                    bookItem.setBookName(str);
                    break;
                case "synopsis":
                    bookItem.setSynopsis(str);
                    break;
                case "classify":
                    bookItem.setClassify(str);
                    break;
                case "author":
                    bookItem.setAuthor(str);
                    break;
            }
        }
        return bookItem;
    }

    private BookItem getBookItemByMap(String[] attrArray, JSONObject pageJson, Map<String, String> map) {
        BookItem bookItem = new BookItem();
//        Log.d(Tag, "pageJson:" + pageJson);
        for (int i = 1; i < attrArray.length; i++) {
            String attr = attrArray[i];
            String attrValue = null;
            if (Objects.equals(pageJson.getJSONArray(attr).getJSONObject(0)
                    .getString("action"), "noAction")) {
                attrValue = map.get(attr);
            } else {

            }
            map.get(attr);
            if (attr == null) continue;
            switch (attr) {
                case "resultList":
                    continue;
                case "bookCode":
                    bookItem.setBookCode(attrValue);
                    break;
                case "bookName":
                    bookItem.setBookName(attrValue);
                    break;
                case "classify":
                    bookItem.setClassify(attrValue);
                    break;
                case "chapter_count_geted":
                    bookItem.setChapterTotal(Integer.parseInt(attrValue));
                    break;
                case "author":
                    bookItem.setAuthor(attrValue);
                    break;
            }
        }
        return bookItem;
    }

    private CatalogItem htmlToCatalog(PlatformItem platformItem, String htmlContent) {
        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setPlatformName(platformItem.getPlatformName());
//        Log.d(Tag, "htmlContent:" + htmlContent);
        String catalogError = platformItem.getCatalogError();
        if (htmlContent.contains(catalogError) || htmlContent.equals("0")) {
            Log.d(Tag, "请求失败");
            catalogItem.addChapterCode("");
        } else {
            Log.d(Tag, "开始匹配CatalogList");
            if (htmlContent != null) {
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
                    String bookName = getAttr(bookNameStep, doc.head());
                    catalogItem.setBookName(bookName);

                    JSONArray authorStep = catalogFormatJson.getJSONArray("author");
                    if (authorStep != null) {
                        String author = getAttr(authorStep, doc.head());
                    }

                    Elements catalogAttrs = switchActionToElements(findList, doc);
                    if (catalogAttrs == null) return null;
                    JSONArray chapterCodeStep = catalogFormatJson.getJSONArray("chapterCode");
                    JSONArray chapterTitleStep = catalogFormatJson.getJSONArray("chapterTitle");
                    for (Element catalogAttr : catalogAttrs) {
                        Log.d(Tag, "catalogAttr:" + catalogAttr.text());
                        String chapterCode = getAttr(chapterCodeStep, catalogAttr);
                        catalogItem.addChapterCode(chapterCode);
                        String chapterTitle = getAttr(chapterTitleStep, catalogAttr);
                        catalogItem.addChapterTitle(chapterTitle);
                        Log.d(Tag, "chapterTitle:" + chapterTitle + "  chapterCode:" + chapterCode);
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                    Log.e(Tag, "e:" + e.getMessage());
                    return null;
                }
                Log.d(Tag, "结束匹配CatalogList");
//                figures = String.valueOf(catalogItem.getChapterCodeList().size()).length();
//                figures = (figures < 3)? 4 : figures;
            }
        }
        return catalogItem;
    }

    private ChapterItem htmlToChapter(PlatformItem platformItem, String htmlContent) {
        ChapterItem chapterItem = new ChapterItem();
        Log.d(Tag, "htmlContent:" + htmlContent);
        String chapterError = platformItem.getChapterError();
        if (htmlContent.contains(chapterError) || htmlContent.equals("0")) {
            chapterItem.setTitle("");
            chapterItem.addParagraph("");
        } else {
            Log.d(Tag, "开始匹配ChapterList");
            if (htmlContent != null) {
                String[] chapterPage = platformItem.getChapterPage();
//                Log.d(Tag, "chapterPage:" + Arrays.toString(chapterPage));
                JSONObject chapterFormatJson = null;
                try {
                    chapterFormatJson = JSONObject.parseObject(platformItem.getChapterPageFormat());
                    if (chapterFormatJson == null) return null;
                    Document doc = Jsoup.parseBodyFragment(htmlContent);
                    JSONArray titleStep = chapterFormatJson.getJSONArray("title");
                    String title = getAttr(titleStep, doc.body());
                    if (title == null) return null;
                    chapterItem.setTitle(title);
                    JSONArray findList = chapterFormatJson.getJSONArray("paragraphList");
                    if (Arrays.asList(chapterPage).contains("paragraph")) {
                        Elements chapterAttrs = switchActionToElements(findList, doc);
                        if (chapterAttrs == null) return null;
                        JSONArray paragraphStep = chapterFormatJson.getJSONArray("paragraph");
                        for (Element chapterAttr : chapterAttrs) {
                            String paragraph = getAttr(paragraphStep, chapterAttr);
                            chapterItem.addParagraph(paragraph);
                        }
                    } else {
                        String[] paragraphArray = switchActionToParagraphArray(findList, doc);
                        if (paragraphArray == null) return null;
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
                    return null;
                }
                Log.d(Tag, "结束匹配ChapterList");
            }
        }
        return chapterItem;
    }

    private Elements switchActionToElements(JSONArray actionArray, Document doc) {
        Element el = null;
        Elements elements = null;
//        Log.d(Tag, "actionArray.length():" + actionArray.size());
        for (int i = 0; i < actionArray.size(); i++) {
//            Log.d(Tag, "i:" + i);
            try {
                JSONObject actionStep = actionArray.getJSONObject(i);
                String action = actionStep.getString("action");
//                Log.d(Tag, "action:" + action);
                switch (action) {
                    case "select":
                        String selectBy = actionStep.getString("by");
                        Log.d(Tag, "selectBy:" + selectBy);
                        String selectGet = actionStep.getString("get");
                        String from = actionStep.getString("from");
                        Log.d(Tag, "selectGet:" + selectGet);
                        if (selectBy != null && i == 0) {
                            elements = doc.select(selectBy);
                            Log.d(Tag, "i=0 elements:" + elements.text());
                        } else if (selectBy != null && i > 0 && el != null) {
                            elements = el.select(selectBy);
                            Log.d(Tag, "i>0 elements:" + elements.text());
                        }
                        if (i == actionArray.size() - 1) {
                            Log.d(Tag, "i=max elements:" + elements.html());
                            if (from != null) {
                                 elements.subList(Integer.parseInt(from), elements.size());
                            }
                            return elements;
                        }
                        if (elements != null && selectGet != null) {
                            el = elements.get(Integer.parseInt(selectGet));
                            Log.d(Tag, "el:" + el.html());
                        }
                        break;
                }
            } catch (JSONException | IndexOutOfBoundsException e) {
                Log.e(Tag, "json数据格式错误，请核对后重新加载:" + e);
                Log.e(Tag, "e:" + e.getMessage());
//                Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return null;
    }

    private Map<String, String> switchActionToMap(JSONArray actionArray, Document doc) {
        Map<String, String> map = new HashMap<String, String>();
        Element el = null;
        Elements elements = null;
        String str = null;
//        Log.d(Tag, "actionArray.length():" + actionArray.size());
        for (int i = 0; i < actionArray.size(); i++) {
//            Log.d(Tag, "i:" + i);
            try {
                JSONObject actionStep = actionArray.getJSONObject(i);
                String action = actionStep.getString("action");
//                Log.d(Tag, "action:" + action);
                switch (action) {
                    case "select":
                        String selectBy = actionStep.getString("by");
//                        Log.d(Tag, "selectBy:" + selectBy);
                        String selectGet = actionStep.getString("get");
//                        Log.d(Tag, "selectGet:" + selectGet);

                        if (selectBy != null && i == 0) {
                            elements = doc.select(selectBy);
//                            Log.d(Tag, "i=0 elements:" + elements.html());
                        } else if (selectBy != null && i > 0 && el != null) {
                            elements = el.select(selectBy);
                            Log.d(Tag, "i>0 elements:" + elements.html());
                        }
                        if (elements != null && selectGet != null) {
                            el = elements.get(Integer.parseInt(selectGet));
                            Log.d(Tag, "el:" + el.html());
                        }
                        break;
                    case "elementGetVar":
                        int getNum = Integer.parseInt(actionStep.getString("get"));
                        if (el != null) {
                            Log.d(Tag, "el:" + el.html());
                            str = el.data().toString().split("var")[getNum];
                        }
                        break;
                    case "forSearch":
                        String listSig = actionStep.getString("action");
                        if (str != null && str.contains(listSig)) {
                            str = str.replace(listSig, "");
                            str = str.replace("}];", "");
                            String[] bookArray = str.split("}, \\{");
                            for (String bookStr : bookArray) {
                                bookStr.replace("\"", "");
                                String[] kv = bookStr.split(": ");
                                map.put(kv[0], kv[1]);
                            }
                        }
                        break;
                }
            } catch (JSONException | IndexOutOfBoundsException e) {
                Log.d(Tag, "数据无法解析:" + e);
                return null;
            }
        }
        return map;
    }

    private String getAttr(JSONArray attrStepJson, Element htmlListItem) {
//        Log.d(Tag, "item:" + attr.trim());
        String str = null;
        try {
            Elements elements = null;
            Element element = null;
            for (int a = 0; a < attrStepJson.size(); a++) {
                JSONObject step = attrStepJson.getJSONObject(a);
//                Log.d(Tag, "actionStep:" + step);
                String action = step.getString("action");
                switch (action) {
                    case "select":
                        String selectBy = step.getString("by");
                        String selectGet = step.getString("get");
                        if (selectBy != null) {
                            elements = htmlListItem.select(selectBy);
                        }
                        if (elements != null && !elements.isEmpty() && selectGet != null) {
                            element = elements.get(Integer.parseInt(selectGet));
                        }
                        break;
                    case "attr":
                        String attrBy = step.getString("by");
                        if (attrBy != null) {
                            if (element == null) {
                                str = htmlListItem.attr(attrBy);
                            } else {
                                str = element.attr(attrBy);
                            }
                        }
                        break;
                    case "i":
                        String from = step.getString("from");
                        if (from != null) {
                            str = String.valueOf((a + Integer.parseInt(from)));
                        }
                        break;
                    case "html":
                        if (element == null) {
                            str = htmlListItem.html();
                        } else {
                            str = element.html();
                        }
                        break;
                    case "replace":
                        String target = step.getString("target");
                        String to = step.getString("to");
                        if (target != null && to != null && str != null) {
                            str = str.replace(target, to);
                        }
                        break;
                    case "replaceAll":
                        String targetA = step.getString("target");
                        String toA = step.getString("to");
                        if (targetA != null && toA != null && str != null) {
                            str = str.replaceAll(targetA, toA);
                        }
                        break;
                }
            }
        } catch (JSONException | IndexOutOfBoundsException e) {
            Log.d(Tag, "json数据格式错误，请核对后重新加载:" + e);
            return null;
        }
        return str;
    }

    private String[] switchActionToParagraphArray(JSONArray actionArray, Document doc) {
        Element el = null;
        Elements elements = null;
        String str = null;
//        Log.d(Tag, "actionArray.length():" + actionArray.size());
        for (int i = 0; i < actionArray.size(); i++) {
//            Log.d(Tag, "i:" + i);
            try {
                JSONObject actionStep = actionArray.getJSONObject(i);
                String action = actionStep.getString("action");
//                Log.d(Tag, "action:" + action);
                switch (action) {
                    case "select":
                        String selectBy = actionStep.getString("by");
//                        Log.d(Tag, "selectBy:" + selectBy);
                        String selectGet = actionStep.getString("get");
                        String from = actionStep.getString("from");
//                        Log.d(Tag, "selectGet:" + selectGet);
                        if (selectBy != null && i == 0) {
                            elements = doc.select(selectBy);
//                            Log.d(Tag, "i=0 elements:" + elements.html());
                        } else if (selectBy != null && i > 0 && el != null) {
                            elements = el.select(selectBy);
//                            Log.d(Tag, "i>0 elements:" + elements.html());
                        }
                        if (elements != null && selectGet != null) {
                            el = elements.get(Integer.parseInt(selectGet));
//                            Log.d(Tag, "el:" + el.html());
                        }
                        break;
                    case "html":
                        if (el != null) {
                            str = el.html();
                        }
                        break;
                    case "replaceAll":
                        if (str == null) return null;
                        String targetA = actionStep.getString("target");
                        String toA = actionStep.getString("to");
                        if (targetA != null && toA != null && str != null) {
                            str = str.replaceAll(targetA, toA);
                        }
                        break;
                    case "split":
                        String[] sa = str.split(actionStep.getString("by"));
                        return sa;
                }
            } catch (JSONException | IndexOutOfBoundsException e) {
                Log.e(Tag, "json数据格式错误，请核对后重新加载:" + e);
//                Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return null;
    }

    private void sendMessage(int platformID, boolean isManual, List<BookItem> bookList,
                             CatalogItem catalogItem, ChapterItem chapterItem) {
        Message msg = new Message();
        boolean b = bookList == null || bookList.isEmpty();
        boolean c = catalogItem == null || catalogItem.getChapterCodeList() == null || catalogItem.getChapterCodeList().isEmpty();
        boolean p = chapterItem == null;
        if (!b & c & p) {
            Log.d(Tag, "解读出来是书籍列表");
            Log.d(Tag, "第一本书名：" + bookList.get(0).getBookName());
            if (Objects.equals(bookList.get(0).getBookName(), "")) {
                // 网络错误
                msg.what = 2;
            } else {
                msg.what = 1;
                msg.arg1 = platformID;
                msg.obj = bookList;
            }
            SearchFragment.handler_setResult.sendMessage(msg);
        } else if (b & !c & p){
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
        } else if (b & c & !p) {
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
        } else {
            Log.d(Tag, "程序BUG");
            msg.what = 0;
        }
    }

    /**
     * 从本地assets文件夹获取内容
     * @param platformItem 平台列表
     * @param bookName 书籍名称
     * @param bookCode 书籍代码
     * @param chapterCode 章节代码
     */
    private void loadLocalAssets(Context context, boolean isManual, PlatformItem platformItem,
                                 String bookName, String bookCode, int chapterNum, String chapterCode) {
        String fileName = null;
        fileName = (bookCode == null)? bookName : bookCode;
        fileName = (chapterCode == null)? fileName : chapterCode;
        fileName = fileName + ".html";
        Log.d(Tag, "fileName:" + fileName);
        String htmlContent;
        try {
            InputStream inputStream = getAssets().open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\r\n");
            }
            htmlContent = stringBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (htmlContent != null) {
            if (chapterCode == null && bookCode == null) {
                List<BookItem> bookList = htmlToBookList(platformItem, bookName, htmlContent);
                sendMessage(platformItem.getID(), isManual, bookList, null, null);
            } else if (chapterCode == null && bookCode != null) {
                CatalogItem catalogItem = htmlToCatalog(platformItem, htmlContent);
                sendMessage(1, isManual, null, catalogItem, null);
                if (catalogItem != null && !catalogItem.getChapterCodeList().isEmpty()) {
                    FileTools.newBook(context, catalogItem);
                }
            } else {
                chapterSend(context, isManual, platformItem, htmlContent,
                        bookName, chapterCode, "", chapterNum);
            }
        }
    }

    private void chapterSend(Context context, boolean isManual, PlatformItem platformItem, String htmlContent,
                             String bookName, String chapterCode, String chapterTitle, int chapterNum) {
        Log.d(Tag, "输出到段落");
        ChapterItem chapterItem = htmlToChapter(platformItem, htmlContent);
        if (chapterItem != null) {
            chapterItem.setBookName(bookName);
            chapterItem.setChapterCode(chapterCode);
            chapterItem.setTitle(chapterTitle);
            chapterItem.setChapterNum(chapterNum);
            sendMessage(platformItem.getID(), isManual, null, null, chapterItem);
            if (chapterItem.getChapter() != null && !chapterItem.getChapter().isEmpty()) {
                FileTools.chapterSave(context, chapterItem);
            }
        } else {
            chapterItem = new ChapterItem();
            chapterItem.setBookName(bookName);
            sendMessage(platformItem.getID(), isManual, null, null, chapterItem);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Tag, "销毁了服务");
    }
}
