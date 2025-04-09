package com.xuanniao.reader.ui.book;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.BookDB;
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.getter.FileTools;
import com.xuanniao.reader.ui.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LocalFragment extends Fragment {
    private static final String Tag = "本地页面";
    protected Context mContext;
    BookDB bdb;
    private ListView lv_book;
    private boolean isFirstLoad;
    private List<BookItem> bookList;
    private BookAdapter bookAdapter;
    public static Handler handler_info;

    public static SearchFragment newInstance(int index) {
        SearchFragment fragment = new SearchFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.BOOK_FRAGMENT_NUM, index);
        Log.d(Tag, "新页面:" + index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFirstLoad = true;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_book, container, false);
        lv_book = fragmentView.findViewById(R.id.lv_book);
        bookList = new ArrayList<>();
        bdb = BookDB.getInstance(mContext, Constants.DB_BOOK);
        handler_info = setInfoHandler();
        return fragmentView;
    }

    @Override
    public void onResume() {
        Log.d(Tag, "载入元素");
        super.onResume();
        if (isFirstLoad) {
            isFirstLoad = false;
            setList();
        }
    }

    public void setList() {
        Log.d(Tag, "setList");
        bookList = bdb.queryAll(Constants.TAB_BOOK);
        Log.d(Tag,"bookList :" + bookList);
        if (bookList == null || bookList.isEmpty()) {
            bookList = new ArrayList<>();
        }
        BookItem item = new BookItem();
        item.setBookName("点击添加本地书籍");
        bookList.add(item);
        Log.d(Tag,  "书籍列表：" + bookList.get(0).getBookName());
        bookAdapter = new BookAdapter(getContext(), bookList);
        // TODO: 逐一从文件中验证bookList的可用性
        BookActivity bookActivity = (BookActivity) getActivity();
        lv_book.setAdapter(bookAdapter);
        lv_book.setOnItemClickListener((adapterView, view, i, l) -> {
            if (i == bookList.size() - 1) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, Constants.FOLDER_SELECT_CODE);
            } else {
                BookItem bookItem = bookList.get(i);
                String platformName = bookItem.getPlatformName();
                String bookName = bookItem.getBookName();
                String bookCode = bookItem.getBookCode();
                Log.d(Tag, "bookName:" + bookName + " | platformName" + platformName);
                bookActivity.openBook(1, platformName, bookName, bookCode);
            }
        });
    }

    @Override
    // 带回文件路径
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.FILE_SELECT_CODE &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 检查是否有权限
            boolean isRefuse = !Environment.isExternalStorageManager();
            Log.d(Tag, "授权：" + isRefuse);
        }
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            Log.d(Tag, "uri:" + uri);
            // 获取持久化权限
            mContext.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // TODO: 这里换成BookGetter
            BookItem bookItem = FileTools.loadLocalBook(mContext, uri);
            if (bookItem.getBookName() != null || !bookItem.getBookName().isEmpty()) {
//                bdb.writeItem(Constants.TAB_BOOK, bookItem);
                Log.d(Tag, "书目名称:" + bookItem.getBookName());
//                Intent intent = new Intent(mContext, InfoGetter.class);
//                intent.putExtra("isLocal", false);
//                intent.putExtra("isCreate", false);
//                intent.putExtra("platformID", platformId);
//                intent.putExtra("bookName", bookName);
//                startService(intent);
                bookList.add(bookList.size() - 1, bookItem);
                bookAdapter = new BookAdapter(mContext, bookList);
                lv_book.setAdapter(bookAdapter);
            } else {
                Toast.makeText(getActivity(), "读取本地书目失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Handler setInfoHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    BookItem bookItem = (BookItem) msg.obj;
                    bookAdapter = new BookAdapter(mContext, bookList);
                    lv_book.setAdapter(bookAdapter);
                } else if (msg.what == 2) {
                    Toast.makeText(mContext, "网络错误", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "程序BUG", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onDestroyView() {
        isFirstLoad = true;
        super.onDestroyView();
    }
}
