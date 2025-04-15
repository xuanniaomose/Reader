package com.xuanniao.reader;

import android.util.Log;
import com.xuanniao.reader.tools.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void test() {
        String s = "完结, <a rel=\"nofollow\" href=\"javascript:void(0);\" onclick=\"if (!window.__cfRLUnblockHandlers) return false; addBookCase('1660');\" data-cf-modified-f2ef7c2c202c684a9032f1a7-=\"\">加入书架</a>, <a href=\"#footer\">直达底部</a>";
        s = s.replace(", <a rel=\"nofollow\" href=\"javascript:void(0);\" onclick=\"if (!window.__cfRLUnblockHandlers) return false; addBookCase", "");
        s = s.replace(";\" data-cf-modified-", "");
        s = s.replace("-=\"\">加入书架</a>, <a href=\"#footer\">直达底部</a>", "");
        s = s.replaceAll("\\('[0-9]*'\\)", "");
        s = s.replaceAll("[0-9A-Za-z]{6,24}", "");
        System.out.println("结果：" + s);
    }

    @Test
    public void html() {
        String html = "<div class=\"box_con\">\n" +
                "            <div class=\"con_top\">\n" +
                "                <script type=\"12b1f3ef7431d734214202c5-text/javascript\">textselect();</script>\n" +
                "                <a href=\"/\">笔趣阁</a> &gt; <a href=\"/class8/\">言情小说</a> &gt; <a\n" +
                "                    href=\"/62132/\">红颜赋</a> &gt; 第七章 影卫\n" +
                "            </div>\n" +
                "            <div class=\"bookname\">\n" +
                "                <h1>第七章 影卫</h1>\n" +
                "            </div>\n" +
                "            <div class=\"bottem1\">\n" +
                "                <a href=\"/62132/828426.html\">上一章</a> &larr; <a href=\"/62132/\">章节目录</a> &rarr; <a\n" +
                "                    href=\"/62132/828428.html\">下一章</a> <a rel=\"nofollow\" href=\"javascript:void(0);\" onclick=\"if (!window.__cfRLUnblockHandlers) return false; addBookMark('62132', '7', '第七章 影卫');\" data-cf-modified-12b1f3ef7431d734214202c5-=\"\">加入书签</a>\n" +
                "            </div>" +
                "        </div>";
        Document doc = Jsoup.parse(html);
        Element el = doc.select("div[class=box_con]").get(0);

        Element element1 = el.select("div[class=bookname]").get(0).select("h1").get(0);
        System.out.println("bookName:" + element1.html());
//
//        Element element2 = el.select("p").get(0);
//        System.out.println("author:" + element2.html().replace("作&nbsp;&nbsp;&nbsp;&nbsp;者：", ""));
//
//        Element element3 = doc.select("a[href=/class4/]").get(1);
//        System.out.println("classify:" + element3.html());
//
//        Element element4 = el.select("p").get(1);
//        System.out.println("status:" + element4.html().replace("状&nbsp;&nbsp;&nbsp;&nbsp;态：", "").replace(", <a rel=\"nofollow\" href=\"javascript:void(0);\" onclick=\"if (!window.__cfRLUnblockHandlers) return false; addBookCase('1660');\" data-cf-modified-d78b1746cb813c06e270ebb6-=\"\">加入书架</a>, <a href=\"#footer\">直达底部</a>",""));
//
//        Element element5 = el.select("p").get(2);
//        System.out.println("renewTime:" + element5.html().replace("最后更新：","").replaceAll(" [0-9]{2}:[0-9]{2}:[0-9]{2}", ""));
//
//        Element element6 = doc.select("div[id=intro]").get(0).select("p").get(0);
//        System.out.println("synopsis:" + element6.html().trim());
//
//        String str = doc.select("img[class=lazy]").get(0).attr("data-original");
//        System.out.println("coverUrl:" + str.replace("/cover", "https://www.beqege.cc/cover"));
    }

    @Test
    public void chineseMatcher() {
        String input = "今天的天气真不错";
        // 正则表达式，假设第三个字是随机汉字
        String regex = "^今天的天气[\u4e00-\u9fa5]*不错$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            System.out.println("匹配成功！");
        } else {
            System.out.println("匹配失败！");
        }
    }

    @Test
    public void stringList() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        String str = listToString(list);
        System.out.println(str);
    }

    public String listToString(List<Integer> list) {
        if (list == null || list.isEmpty()) return null;
        StringBuilder string = new StringBuilder();
        for (Integer numS : list) {
            string.append(numS).append(Constants.INTEGER_DIVIDER);
        }
        string.delete(string.length() - 1, string.length());
        return string.toString();
    }

    @Test
    public void getLongTime() {
        String stringTime = "2023-02-02";
        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd[['T'HH][:mm][:ss]]")
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
                .toFormatter();
        LocalDateTime localDateTime;
        try {
            localDateTime = LocalDateTime.parse(stringTime, formatter);
        } catch (DateTimeParseException e) {
            throw new RuntimeException(e);
        }
        ZoneId zoneId = ZoneId.of("GMT+8");
        System.out.println("Timestamp: " + localDateTime.atZone(zoneId).toInstant().toEpochMilli());
    }

}