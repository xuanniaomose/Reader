package com.xuanniao.reader.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.xuanniao.reader.R;
import com.xuanniao.reader.item.BookDB;
import com.xuanniao.reader.item.PlatformDB;
import com.xuanniao.reader.item.PlatformItem;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Tools {
    static String Tag = "Tools";
    public static int getIntColor(Context context, String colorName) {
        int defColor = 0x00000000;
        try {
            Class<R.color> res = R.color.class;//获取color类
            Field field = res.getField(colorName);
            // 获取colorName为color_xx的color id
            int colorId = field.getInt(null);
            // 通过id获取颜色的int值
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                defColor = context.getResources().getColor(
                        colorId, context.getTheme());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(Tag + "_intColor", e.getMessage());
        }
        return defColor;
    }

    public static int getPlatformID(Context context, String platformName) {
        int platformID = 0;
        PlatformDB pdb = PlatformDB.getInstance(context, Constants.DB_BOOK);
        List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
        for (int i = 0; i < platformList.size(); i++) {
            PlatformItem item = platformList.get(i);
            if (Objects.equals(item.getPlatformName(), platformName)) {
                platformID = i;
            }
        }
        return platformID;
    }

    public static String getPlatformCookie(Context context, String platformName) {
        PlatformDB pdb = PlatformDB.getInstance(context, Constants.DB_BOOK);
        List<PlatformItem> platformList = pdb.queryAll(Constants.TAB_PLATFORM);
        for (PlatformItem item : platformList) {
            if (Objects.equals(item.getPlatformName(), platformName)) {
                return item.getPlatformCookie();
            }
        }
        return null;
    }

    public static long getLongTime(String stringTime) {
        Log.d(Tag, "stringTime:" + stringTime);
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
        return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    public static String getStringTime(long longTime) {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) ;
        return simpleDateFormat.format(longTime);
    }
}
