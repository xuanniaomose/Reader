package com.xuanniao.reader;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String s = "返回：(1)\\/最新网址:";
        s = s.replaceAll("返回：\\(.*\\)\\\\/", "替换成功");
        System.out.print(s);
    }
}