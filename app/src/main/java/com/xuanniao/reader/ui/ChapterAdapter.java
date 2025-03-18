package com.xuanniao.reader.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.xuanniao.reader.R;

import java.lang.reflect.Field;
import java.util.List;

public class ChapterAdapter extends ArrayAdapter<String> {
    private final String Tag = "ChapterAdapter";
    private final Context context;
    private int textColor, textSize = 0;
    private List<String> paragraphList;
    private int paragraphNum = -1;
    public ChapterAdapter(Context context, List<String> paragraphList, int textColor) {
        super(context, R.layout.article_i, paragraphList);
        this.context = context;
        this.textColor = textColor;
        this.paragraphList = paragraphList;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item;
        ViewHolder viewHolder;
        //为每一个子项加载设定的布局
        if (convertView == null) {
            item = LayoutInflater.from(getContext()).inflate(R.layout.article_i, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.tv_paragraph = item.findViewById(R.id.tv_paragraph);
            item.setTag(viewHolder); // 将ViewHolder存储在View中
        } else {
            item = convertView;
            viewHolder = (ViewHolder) item.getTag();
        }
        if (position == paragraphNum) {
            viewHolder.tv_paragraph.setTextColor(getIntColor("colorPrimary"));
            viewHolder.tv_paragraph.setBackgroundColor(getIntColor("windowBackgroundSecondary"));
        } else {
            viewHolder.tv_paragraph.setTextColor(textColor);
            viewHolder.tv_paragraph.setBackgroundColor(getIntColor("transparent"));
            if (textSize != 0) {
                viewHolder.tv_paragraph.setTextSize(textSize);
            }
        }
        String paragraph = paragraphList.get(position);
//        Log.d(Tag, "paragraph:" + paragraph);
        viewHolder.tv_paragraph.setText("      " + paragraph);
        return item;
    }

    public void setHighlight(int paragraphNum) {
//        Log.d(Tag, "paragraphNum:" + paragraphNum);
        if (!(paragraphNum < -1 || paragraphNum > paragraphList.size())) {
            this.paragraphNum = paragraphNum;
            notifyDataSetChanged();
        }
    }

    public void setTextColor(int color) {
        this.textColor = color;
        this.notifyDataSetInvalidated();
    }

    public void setTextSize(int size) {
        this.textSize = size;
        this.notifyDataSetInvalidated();
    }

    int getIntColor(String colorName) {
        int defColor = 0x00000000;
        try {
            Class<R.color> res = R.color.class;//获取color类
            Field field = res.getField(colorName);
            // 获取colorName为color_xx的color id
            int colorId = field.getInt(null);
            // 通过id获取颜色的int值
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                defColor = context.getResources().getColor(
                        colorId, getContext().getTheme());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return defColor;
    }

    static class ViewHolder {
        TextView tv_paragraph;
    }
}
