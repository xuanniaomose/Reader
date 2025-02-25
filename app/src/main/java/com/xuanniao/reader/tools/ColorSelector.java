package com.xuanniao.reader.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.RequiresApi;
import com.xuanniao.reader.R;

import java.lang.reflect.Field;

public class ColorSelector extends RadioGroup {
    private String tag = "ColorSelector";
    private int layoutWidth = dp2px(396); // 9个tab的
    private int layoutHeight = dp2px(36);
    private ItemClickListener itemClickListener;
    private int lastSelected = 0;
//    public static final int RECTANGLE = 0;
//    public static final int OVAL = 1;
//    public static final int LINE = 2;
//    @IntDef({RECTANGLE, OVAL, LINE})
//    @interface radioStyle {}

    public static class ColorTab {
        private String colorName;
        private int colorInt;
        private int style = 0;
        private Object tag;

        public ColorTab(String colorName, int colorInt, int style) {
            this.colorName = colorName;
            this.colorInt = colorInt;
            this.style = style;
//            Log.d((String) tag, "colorInt:"+colorInt);
        }

        public ColorTab(String colorName, int colorInt, int style, Object tag) {
            this.colorName = colorName;
            this.colorInt = colorInt;
            this.style = style;
            this.tag = tag;
        }

        public Object getTag() {
            return tag;
        }

        public void setTag(Object tag) {
            this.tag = tag;
        }

        public String getColorName() {
            return colorName;
        }

        public void setColorName(String colorName) {
            this.colorName = colorName;
        }

        public int getColorInt() {
            return colorInt;
        }

        public void setColorInt(int colorInt) {
            this.colorInt = colorInt;
        }
    }

    public ColorSelector(Context context) {
        super(context);
        init();
    }

    public ColorSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 重置当前RadioGroup的宽度和高度
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(layoutWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(layoutHeight, MeasureSpec.EXACTLY));
    }

    public void setItemByList(String[] colorList, int style) {
        for (String s : colorList) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                add(new ColorTab(s, getIntColor(s), style));
            }
        }
        layoutWidth = colorList.length * dp2px(48);
        onSizeChanged(layoutWidth, layoutHeight, dp2px(396), dp2px(50));
    }

//    public void setItemByList(String[] colorList, int style, int colorInt, Object tag) {
//        for (String s : colorList) {
//            add(new ColorTab(s, colorInt, style, tag));
//        }
//    }

//    public void addColorTab(String colorName) {
//        add(new ColorTab(colorName, getIntColor(colorName), RECTANGLE));
//    }
//
//    public void addColorTab(String colorName, int colorInt, Object tag, int style) {
//        add(new ColorTab(colorName, colorInt, style, tag));
//    }

    // 核心是绑定Selector
    @SuppressLint({"NewApi", "ResourceType"})
    private void add(ColorTab tab) {
        RadioButton radioButton = new RadioButton(getContext());
        // 删除原生的adioButton效果
        radioButton.setButtonDrawable(null);
//        radioButton.setText(tab.getColorName());
//        radioButton.setTextColor(getResources().getColorStateList(R.drawable.XXX))
        Drawable drawable;
        // 默认选中第一个Tab
        if (getChildCount() == 0) {
            drawable = tabStyle(tab.style, tab.colorInt, true);
        } else {
            drawable = tabStyle(tab.style, tab.colorInt, false);
        }
        radioButton.setBackground(drawable);
        // 设置RadioButton大小
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp2px(4),dp2px(4),dp2px(4),dp2px(4));
        // item控件的对齐方式一定要在params的时候就设置好
        params.gravity = Gravity.CENTER;
        radioButton.setLayoutParams(params);
        radioButton.setTag(tab);
        this.addView(radioButton);

    }

    /**
     * 将特定的dp值转换为像素单位
     */
    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, getResources().getDisplayMetrics());
    }

    // 从color.xml中获取颜色值
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    int getIntColor(String colorName) {
        int defColor = 0x00000000;
        try {
            Class<R.color> res = R.color.class;//获取color类
            Field field = res.getField(colorName);
            // 获取colorName为color_xx的color id
            int colorId = field.getInt(null);
            // 通过id获取颜色的int值
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                defColor = getResources().getColor(
                        colorId, getContext().getTheme());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return defColor;
    }

    // 风格化绘制单元格按钮
    Drawable tabStyle(int style, int color, boolean Selected) {
        GradientDrawable drawable = new GradientDrawable();
        switch (style) {
            case 0:
//                Log.d(tag, "rectangle");
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(dp2px(4));
                drawable.setBounds(0, dp2px(5), 0, dp2px(5));
                drawable.setSize(dp2px(40),dp2px(25));
                break;
            case 1:
//                Log.d(tag, "oval");
                drawable.setShape(GradientDrawable.OVAL);
                drawable.setSize(dp2px(32),dp2px(32));
                break;
            case 2:
//                Log.d(tag, "line");
                drawable.setShape(GradientDrawable.LINE);
                drawable.setSize(dp2px(40),dp2px(5));
                drawable.setStroke(dp2px(2), color);
                break;
        }
        String hex = Integer.toHexString(color);
        Log.d(tag, "hex:"+hex);
        drawable.setColor(color);
        if (Selected) {
            drawable.setStroke(dp2px(2), 0xFFFFFFFF);
        } else {
            drawable.setStroke(dp2px(2), 0xFF6E6E6E);
        }
        return drawable;
    }

    private void init() {
        setOrientation(HORIZONTAL);
//        setBackgroundColor(0xFF888888);
        //处理回调
        setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radio = (RadioButton) group.findViewById(checkedId);
                if (!radio.isChecked()) return;
                ColorTab tab = (ColorTab) radio.getTag();
                if (itemClickListener == null) return;
                itemClickListener.onClick(tab, indexOfChild(radio));
                if (lastSelected != indexOfChild(radio)) {
                    // 把上一个选中的边框变灰
                    RadioButton lastRadio = (RadioButton) getChildAt(lastSelected);
                    ColorTab lastTab = (ColorTab) lastRadio.getTag();
                    Drawable lastDrawable = tabStyle(lastTab.style, lastTab.colorInt, false);
                    lastRadio.setBackground(lastDrawable);
                    lastSelected = indexOfChild(radio);
                }
                Drawable drawable = tabStyle(tab.style, tab.colorInt, true);
                radio.setBackground(drawable);
            }
        });
    }

    //注册回调
    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    //回调接口
    public interface ItemClickListener {
        void onClick(ColorTab tab, int position);
    }
}
