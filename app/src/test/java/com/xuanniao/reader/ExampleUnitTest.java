package com.xuanniao.reader;

import android.icu.text.SimpleDateFormat;
import com.xuanniao.reader.tools.Constants;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        int part = 0 * 100 + 5;
        int a = part / 100;
        System.out.println(a);
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
}