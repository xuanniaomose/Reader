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
import android.widget.ImageView;
import android.widget.TextView;
import com.xuanniao.reader.R;
import com.xuanniao.reader.getter.CoverGetter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

public class BookAdapter extends ArrayAdapter<BookItem> {
    private final String Tag = "BookAdapter";
    private final Context context;
    private List<BookItem> bookList;
//    private ImageLoaderTask imageLoader;
    private int highlightNum = -1;
    public BookAdapter(Context context, List<BookItem> bookList) {
        super(context, R.layout.book_list_item, bookList);
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
            item = LayoutInflater.from(getContext()).inflate(R.layout.book_list_item, parent, false);
            viewHolder = new BookAdapter.ViewHolder();
            viewHolder.iv_bookICover = item.findViewById(R.id.iv_bookICover);
            viewHolder.tv_bookIName = item.findViewById(R.id.tv_bookIName);
            viewHolder.tv_wordCount = item.findViewById(R.id.tv_wordCount);
            viewHolder.tv_bookIAuthor = item.findViewById(R.id.tv_bookIAuthor);
            viewHolder.tv_bookIRenewTime = item.findViewById(R.id.tv_bookIRenewTime);
            viewHolder.tv_bookIClassify = item.findViewById(R.id.tv_bookIClassify);
            viewHolder.tv_bookIStatus = item.findViewById(R.id.tv_bookIStatus);
            viewHolder.tv_bookIHasRead = item.findViewById(R.id.tv_bookIHasRead);
            viewHolder.tv_bookITotal = item.findViewById(R.id.tv_bookITotal);
            viewHolder.tv_bookISynopsis = item.findViewById(R.id.tv_bookISynopsis);
            item.setTag(viewHolder); // 将ViewHolder存储在View中
        } else {
            item = convertView;
            viewHolder = (BookAdapter.ViewHolder) item.getTag();
        }
        BookItem bookItem = bookList.get(position);
        String coverUrl = bookItem.getCoverUrl();
        String bookName = bookItem.getBookName();
        String author = bookItem.getAuthor();
        long renewTime = bookItem.getRenewTime();
        int wordCount = bookItem.getWordCount();
        String classify = bookItem.getClassify();
        String status = bookItem.getStatus();
        List<Integer> chapterRead = bookItem.getChapterReadList();
        int chapterTotal = bookItem.getChapterTotal();
        String synopsis = bookItem.getSynopsis();
        Log.d(Tag, "bookName:" + bookName);

        if (coverUrl != null) {
            viewHolder.iv_bookICover.setTag(coverUrl);
            CoverGetter coverGetter = new CoverGetter(context, viewHolder.iv_bookICover);
            coverGetter.execute(coverUrl);
        }

        viewHolder.tv_bookIName.setText(bookName);
        if (wordCount > 0) {
            viewHolder.tv_wordCount.setText(wordCount + "万字");
        }
        if (author != null && !author.isEmpty()) {
            viewHolder.tv_bookIAuthor.setText("作者：" + author);
        }
        if (renewTime > 0) {
            SimpleDateFormat formater = new SimpleDateFormat("yyyy.MM.dd", Locale.CHINA);
            viewHolder.tv_bookIRenewTime.setText("更新：" + formater.format(renewTime));
        }
        if (chapterRead != null && !chapterRead.isEmpty() && chapterTotal > 0) {
            viewHolder.tv_bookIHasRead.setText(chapterRead.get(chapterRead.size() - 1) + "/" +chapterTotal);
        }
        if (classify != null && !classify.isEmpty()) {
            viewHolder.tv_bookIClassify.setText("类型：" + classify);
        }
        if (status != null && !status.isEmpty()) {
            viewHolder.tv_bookIStatus.setText("状态：" + status);
        }
        if (chapterRead != null && !chapterRead.isEmpty()) {
            viewHolder.tv_bookIHasRead.setText("已读：" + chapterRead.size());
        }
        if (chapterTotal > 0) {
            viewHolder.tv_bookITotal.setText("总章数：" + chapterTotal);
        }
        if (position == highlightNum) {
            viewHolder.tv_bookIName.setTextColor(getIntColor("textColorSecondary"));
            viewHolder.tv_bookIName.setBackgroundColor(getIntColor("windowBackgroundSecondary"));
        } else {
            viewHolder.tv_bookIName.setTextColor(getIntColor("textColor"));
            viewHolder.tv_bookIName.setBackgroundColor(getIntColor("transparent"));
        }
        if (synopsis != null && !synopsis.isEmpty()) {
            viewHolder.tv_bookISynopsis.setText("简介：" + synopsis);
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

    @Override
    public BookItem getItem(int position) {
        return bookList.get(position);
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
        ImageView iv_bookICover;
        TextView tv_bookIName;
        TextView tv_wordCount;
        TextView tv_bookIAuthor;
        TextView tv_bookIRenewTime;
        TextView tv_bookIClassify;
        TextView tv_bookIStatus;
        TextView tv_bookIHasRead;
        TextView tv_bookITotal;
        TextView tv_bookISynopsis;
    }
}
