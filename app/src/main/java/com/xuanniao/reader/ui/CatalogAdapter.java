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
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.item.CatalogItem;
import com.xuanniao.reader.item.BookDB;
import com.xuanniao.reader.tools.Constants;

import java.lang.reflect.Field;
import java.util.List;

public class CatalogAdapter extends ArrayAdapter<String> {
    private final String Tag = "CatalogAdapter";
    private final Context context;
    private final CatalogItem catalogItem;
    private List<Integer> readList, savedList;
    public CatalogAdapter(Context context, CatalogItem catalogItem) {
        super(context, R.layout.article_i, catalogItem.getChapterTitleList());
        this.catalogItem = catalogItem;
        this.context = context;
        setHighlight();
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
            viewHolder.tv_catalogTitle = item.findViewById(R.id.tv_paragraph);
            viewHolder.tv_catalogTitle.setTextSize(18);
            item.setTag(viewHolder); // 将ViewHolder存储在View中
        } else {
            item = convertView;
            viewHolder = (ViewHolder) item.getTag();
        }
        if (readList != null && readList.contains(position + 1)) {
            viewHolder.tv_catalogTitle.setTextColor(getIntColor("colorOnPrimary"));
        } else {
            viewHolder.tv_catalogTitle.setTextColor(getIntColor("colorPrimary"));
        }
        String title = catalogItem.getChapterTitle(position);
        if (savedList != null && savedList.contains(position + 1)) {
            Log.d(Tag, "position:" + position);
            // 加下划线
//            viewHolder.tv_catalogTitle.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
            viewHolder.tv_catalogTitle.setText(title + " (已下载)");
        } else {
            viewHolder.tv_catalogTitle.setText(title);
        }
        return item;
    }

    public void setHighlight() {
        BookDB bdb = BookDB.getInstance(context, Constants.DB_BOOK);
        if (catalogItem.getBookName() == null) return;
        Log.d(Tag, "bookName:" + catalogItem.getBookName());
        BookItem bookItem = bdb.queryByFieldItem(Constants.TAB_BOOK, "bookName", catalogItem.getBookName());
        if (bookItem == null) return;
        readList = bookItem.getChapterReadList();
        savedList = bookItem.getChapterSavedList();
        Log.d(Tag, "readChapterList:" + readList);
        Log.d(Tag, "savedChapterList:" + savedList);
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
            Log.e(Tag, e.getMessage());
        }
        return defColor;
    }

    static class ViewHolder {
        TextView tv_catalogTitle;
    }
}
