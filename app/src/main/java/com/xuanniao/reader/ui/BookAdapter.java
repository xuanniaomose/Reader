package com.xuanniao.reader.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
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
import java.util.Locale;

public class BookAdapter extends ArrayAdapter<BookItem> {
    private final String Tag = "BookAdapter";
    private final Context context;
    private List<BookItem> bookList;
    private int highlightNum = -1;
    public BookAdapter(Context context, List<BookItem> bookList) {
        super(context, R.layout.book_list_i, bookList);
        this.bookList = bookList;
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
        BookAdapter.ViewHolder viewHolder;
        //为每一个子项加载设定的布局
        if (convertView == null) {
            item = LayoutInflater.from(getContext()).inflate(R.layout.book_list_i, parent, false);
            viewHolder = new BookAdapter.ViewHolder();
            viewHolder.tv_bookIName = item.findViewById(R.id.tv_bookIName);
            viewHolder.tv_bookIAuthor = item.findViewById(R.id.tv_bookIAuthor);
            viewHolder.tv_bookIRenewTime = item.findViewById(R.id.tv_bookIRenewTime);
            viewHolder.tv_bookIClassify = item.findViewById(R.id.tv_bookIClassify);
            viewHolder.tv_bookIHasRead = item.findViewById(R.id.tv_bookIHasRead);
            item.setTag(viewHolder); // 将ViewHolder存储在View中
        } else {
            item = convertView;
            viewHolder = (BookAdapter.ViewHolder) item.getTag();
        }
        BookItem bookItem = bookList.get(position);
        String bookName = bookItem.getBookName();
        String author = bookItem.getAuthor();
        long renewTime = bookItem.getRenewTime();
        String classify = bookItem.getClassify();
        List<Integer> chapterRead = bookItem.getChapterReadList();
        int chapterTotal = bookItem.getChapterTotal();
        viewHolder.tv_bookIName.setText(bookName);
        if (author != null && !author.isEmpty()) {
            viewHolder.tv_bookIAuthor.setText(author);
        }
        if (renewTime > 0) {
            SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            viewHolder.tv_bookIRenewTime.setText(formater.format(renewTime));
        }
        if (chapterRead != null && !chapterRead.isEmpty() && chapterTotal > 0) {
            viewHolder.tv_bookIHasRead.setText(chapterRead.get(chapterRead.size() - 1) + "/" +chapterTotal);
        }
        if (classify != null && !classify.isEmpty()) {
            viewHolder.tv_bookIClassify.setText(classify);
        }
        if (position == highlightNum) {
            viewHolder.tv_bookIName.setTextColor(getIntColor("textColorSecondary"));
            viewHolder.tv_bookIName.setBackgroundColor(getIntColor("windowBackgroundSecondary"));
        } else {
            viewHolder.tv_bookIName.setTextColor(getIntColor("textColor"));
            viewHolder.tv_bookIName.setBackgroundColor(getIntColor("transparent"));
        }
        return item;
    }

    public void setHighlight(int paragraphNum) {
        Log.d(Tag, "paragraphNum:" + paragraphNum);
        if (!(paragraphNum < -1 || paragraphNum > bookList.size())) {
            this.highlightNum = paragraphNum;
            notifyDataSetChanged();
        }
    }

    public void setBookList(List<BookItem> bookList) {
        this.bookList = bookList;
        notifyDataSetChanged();
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
        TextView tv_bookIName;
        TextView tv_bookIAuthor;
        TextView tv_bookIRenewTime;
        TextView tv_bookIClassify;
        TextView tv_bookIHasRead;
    }
}
