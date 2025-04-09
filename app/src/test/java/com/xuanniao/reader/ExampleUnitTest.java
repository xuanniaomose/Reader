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
        String s = "/10_10966/270298";
        s = s.replaceAll("/[0-9]*_[0-9]*/", "替换成功");
        System.out.print(s);
    }
}