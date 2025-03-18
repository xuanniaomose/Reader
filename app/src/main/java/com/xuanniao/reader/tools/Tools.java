package com.xuanniao.reader.tools;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.xuanniao.reader.R;

import java.lang.reflect.Field;

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
}
