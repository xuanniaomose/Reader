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
    private List<String> paragraphList;
    private int paragraphNum = -1;
    public ChapterAdapter(Context context, List<String> paragraphList) {
        super(context, R.layout.article_i, paragraphList);
        this.paragraphList = paragraphList;
        this.context = context;
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
            viewHolder.tv_paragraph.setTextColor(getIntColor("green_500"));
            viewHolder.tv_paragraph.setBackgroundColor(getIntColor("gray_700"));
        } else {
            viewHolder.tv_paragraph.setTextColor(getIntColor("green_700"));
            viewHolder.tv_paragraph.setBackgroundColor(getIntColor("black"));
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
