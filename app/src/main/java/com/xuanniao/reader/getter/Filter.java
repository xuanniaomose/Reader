package com.xuanniao.reader.getter;

import android.util.Log;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.xuanniao.reader.ui.BookItem;
import okhttp3.Headers;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Filter {
    private final static String Tag = "Filter";
    static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/" +
            "537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0";
    static String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/" +
            "avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";

    public static Headers setHeaders(String cookie) {
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

    static BookItem getBookItem(String[] attrArray, JSONObject pageJson, Element htmlListItem) {
        BookItem bookItem = new BookItem();
//        Log.d(Tag, "pageJson:" + pageJson);
        for (int i = 1; i < attrArray.length; i++) {
            String attr = attrArray[i];
            if (attr == null) continue;
            JSONArray attrStepJson = pageJson.getJSONArray(attr);
            Log.d(Tag, "htmlListItem:" + htmlListItem);
            String str = getAttr(attrStepJson, htmlListItem);
            Log.d(Tag, "str:" + str);
            if (str == null) continue;
            switch (attr) {
                case "resultList":
                    continue;
                case "infoList":
                    continue;
                case "bookCode":
                    bookItem.setBookCode(str);
                    break;
                case "bookName":
                    bookItem.setBookName(str);
                    break;
                case "author":
                    bookItem.setAuthor(str);
                    break;
                case "classify":
                    bookItem.setClassify(str);
                    break;
                case "status":
                    bookItem.setStatus(str);
                    break;
                case "renewTime":
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE);
                    Date parsedDate = null;
                    try {
                        parsedDate = dateFormat.parse(str);
                    } catch (ParseException e) {
                        Log.e(Tag, "Error:" + e);
                    }
                    long date = (parsedDate != null)? parsedDate.getTime() : 0;
                    bookItem.setRenewTime(date);
                    break;
                case "synopsis":
                    bookItem.setSynopsis(str);
                    break;
            }
        }
        return bookItem;
    }

    static BookItem getBookItemByMap(String[] attrArray, JSONObject pageJson, Map<String, String> map) {
        BookItem bookItem = new BookItem();
        Log.d(Tag, "attrArray:" + Arrays.toString(attrArray));
        Log.d(Tag, "pageJson:" + pageJson);
        for (int i = 1; i < attrArray.length; i++) {
            String attr = attrArray[i];
            String attrValue = null;
//            if (Objects.equals(pageJson.getJSONArray(attr).getJSONObject(0)
//                    .getString("action"), "noAction")) {
//                attrValue = map.get(attr);
//            }
            attrValue = map.get(attr);
            Log.d(Tag, "attrValue:" + attrValue);
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

    static Elements switchActionToElements(JSONArray actionArray, Document doc) {
        Element el = null;
        Elements elements = null;
//        Log.d(Tag, "actionArray.length():" + actionArray.size());
        for (int i = 0; i < actionArray.size(); i++) {
//            Log.d(Tag, "i:" + i);
            try {
                JSONObject actionStep = actionArray.getJSONObject(i);
                String action = actionStep.getString("action");
                Log.d(Tag, "action:" + action);
                if (!Objects.equals(action, "select")) return null;

                String selectBy = actionStep.getString("by");
                Log.d(Tag, "selectBy:" + selectBy);
                String selectGet = actionStep.getString("get");
                String from = actionStep.getString("from");
                Log.d(Tag, "selectGet:" + selectGet);
                if (selectBy != null && i == 0) {
                    elements = doc.select(selectBy);
                    Log.d(Tag, "i=0 elements:" + elements.html());
                } else if (selectBy != null && i > 0 && el != null) {
                    elements = el.select(selectBy);
                    Log.d(Tag, "i>0 elements:" + elements.html());
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
            } catch (JSONException | IndexOutOfBoundsException e) {
                Log.e(Tag, "挑选成组时出错:" + e);
//                Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return null;
    }

    static Element switchActionToElement(JSONArray actionArray, Document doc) {
        Element el = null;
        Elements elements = null;
//        Log.d(Tag, "actionArray.length():" + actionArray.size());
        for (int i = 0; i < actionArray.size(); i++) {
            Log.d(Tag, "i:" + i);
            try {
                JSONObject actionStep = actionArray.getJSONObject(i);
                String action = actionStep.getString("action");
                Log.d(Tag, "action:" + action);
                if (!Objects.equals(action, "select")) return null;

                String selectBy = actionStep.getString("by");
                Log.d(Tag, "selectBy:" + selectBy);
                String selectGet = actionStep.getString("get");
                Log.d(Tag, "selectGet:" + selectGet);

                if (selectBy != null && i == 0) {
                    elements = doc.select(selectBy);
                    Log.d(Tag, "i=0 elements:" + elements.html());
                } else if (selectBy != null && i > 0 && el != null) {
                    elements = el.select(selectBy);
                    Log.d(Tag, "i>0 elements:" + elements.html());
                }
                if (elements != null && selectGet != null) {
                    el = elements.get(Integer.parseInt(selectGet));
                    Log.d(Tag, "el:" + el.html());
                }
                if (i == actionArray.size() - 1) {
                    if (el != null) {
                        Log.d(Tag, "i=max element:" + el.html());
                        return el;
                    }
                }
            } catch (JSONException | IndexOutOfBoundsException e) {
                Log.e(Tag, "挑选成组时出错:" + e);
                return null;
            }
        }
        return null;
    }

    static Map<String, String> switchActionToMap(JSONArray actionArray, Document doc) {
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
                Log.d(Tag, "挑选成Map时出错:" + e);
                return null;
            }
        }
        return map;
    }

    static String getAttr(JSONArray attrStepJson, Element htmlListItem) {
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
            Log.d(Tag, "属性解析时出错:" + e);
            return null;
        }
        return str;
    }

    static String[] switchActionToParagraphArray(JSONArray actionArray, Document doc) {
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
                Log.e(Tag, "清洗段落时出错:" + e);
//                Toast.makeText(this, "json数据格式错误，请核对后重新加载", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return null;
    }
}
