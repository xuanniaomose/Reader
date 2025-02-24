package com.xuanniao.reader.tools;

import android.util.Log;
import com.xuanniao.reader.ui.book.PlatformItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class JsonRead {
    static String Tag = "JsonRead";

    public static List<PlatformItem> JsonToPlatformList(JSONObject o) {
        List<PlatformItem> platformList = new ArrayList<>();
        try {
            JSONArray list = (JSONArray) o.get("list");
            if (list == null || list.length() == 0) {
                return null;
            }
            for (int i = 0; i < list.length(); i++) {
                Object object = list.get(i);
                PlatformItem platformItem = new PlatformItem();
                JSONObject item = (JSONObject) object;
                platformItem.setID(i);
                String platformName = (String) item.get("platformName");
                platformItem.setPlatformName(platformName);
                String platformUrl = (String) item.get("platformUrl");
                platformItem.setPlatformUrl(platformUrl);
                String searchPath = (String) item.get("searchPath");
                platformItem.setSearchPath(searchPath);
                String platformCookie = (String) item.get("platformCookie");
                platformItem.setPlatformCookie(platformCookie);
                String charsetName = (String) item.get("charsetName");
                platformItem.setCharsetName(charsetName);

                // 结果页面
                JSONObject resultPage = (JSONObject) item.get("resultPage");
                Iterator resultListI = resultPage.keys();
                List<String> resultList = new ArrayList<>();
                while(resultListI.hasNext()){
                    String key = (String) resultListI.next();
                    resultList.add(key);
                }
                Log.d(Tag, "resultList:" + Arrays.toString(resultList.toArray(new String[]{})));
                platformItem.setResultPage(resultList.toArray(new String[]{}));
                String resultError = (String) item.get("resultError");
                platformItem.setResultError(resultError);
                Log.d(Tag, "resultPage:" + resultPage);
                platformItem.setResultPageFormat(resultPage.toString());

                // 目录页面
                JSONObject catalogPage = (JSONObject) item.get("catalogPage");
                Iterator catalogListI = catalogPage.keys();
                List<String> catalogList = new ArrayList<>();
                while(catalogListI.hasNext()){
                    String key = (String) catalogListI.next();
                    catalogList.add(key);
                }
                Log.d(Tag, "catalogList:" + catalogList);
                platformItem.setCatalogPage(catalogList.toArray(new String[]{}));
                String catalogError = (String) item.get("catalogError");
                platformItem.setCatalogError(catalogError);
                Log.d(Tag, "catalogPage:" + catalogPage);
                platformItem.setCatalogPageFormat(catalogPage.toString());

                // 章节页面
                JSONObject chapterPage = (JSONObject) item.get("chapterPage");
                Iterator chapterListI = chapterPage.keys();
                List<String> chapterList = new ArrayList<>();
                while(chapterListI.hasNext()){
                    String key = (String) chapterListI.next();
                    chapterList.add(key);
                }
                platformItem.setChapterPage(chapterList.toArray(new String[]{}));
                Log.d(Tag, "chapterList:" + chapterList);
                String chapterError = (String) item.get("chapterError");
                platformItem.setChapterError(chapterError);
                Log.d(Tag, "chapterPage:" + chapterPage);
                platformItem.setChapterPageFormat(chapterPage.toString());

                platformList.add(platformItem);
            }
        } catch (JSONException e) {
            Log.e(Tag, String.valueOf(e));
        }
        return platformList;
    }
}