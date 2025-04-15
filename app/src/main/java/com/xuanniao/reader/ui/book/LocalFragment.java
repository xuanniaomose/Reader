package com.xuanniao.reader.ui.book;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.xuanniao.reader.getter.BookGetter;
import com.xuanniao.reader.getter.InfoGetter;
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.item.BookDB;
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.tools.Tools;
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
    public static Handler handler_local, handler_info;

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
        handler_local = setLocalHandler();
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
                Log.d(Tag, "bookName:" + bookItem.getBookName() + " " + platformName);
                bookActivity.openBook(true, platformName, bookItem);
            }
        });
        lv_book.setOnItemLongClickListener((adapterView, view, i, l) -> {
            Log.d(Tag, "i:" + i);
            showDeleteWindow(i);
            return true;
        });
    }

    private void showDeleteWindow(int i) {
        BookItem item = bookList.get(i);
        AlertDialog dialog = new AlertDialog.Builder(mContext)
//                .setIcon(R.drawable.ic_logo)//设置标题的图片
                .setTitle("是否删除")//设置对话框的标题
                .setMessage(item.getBookName() + " 作者" + item.getAuthor())//设置对话框的内容
                //设置对话框的按钮
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long r = bdb.itemDeleteByID(Constants.TAB_BOOK, new String[]{String.valueOf(i+1)});
                        bookList.remove(i);
                        Toast.makeText(mContext, "删除了" + r + "本书", Toast.LENGTH_LONG).show();
                        setList();
                    }
                })
                .setNeutralButton("取消", (dialogInterface, num) -> {})
                .create();
        dialog.show();
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
            Intent intent = new Intent(mContext, BookGetter.class);
            intent.putExtra("isLocal", true);
            intent.putExtra("bookUri", String.valueOf(uri));
            mContext.startService(intent);
        }
    }

    private Handler setLocalHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    Toast.makeText(mContext, "程序BUG", Toast.LENGTH_SHORT).show();
                    return;
                }
                int platformID = msg.arg1;
                Log.d(Tag, "platformID:" + platformID);
                List<BookItem> list = (List<BookItem>) msg.obj;
                if (list == null || list.isEmpty()) {
                    Toast.makeText(getActivity(), "读取本地书目失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (BookItem item : list) {
                    if (item.getBookName() == null || item.getBookName().isEmpty()) continue;
                    if (bookList.contains(item)) continue;
                    if (msg.arg2 == 0) {
                        Intent intent = new Intent(mContext, InfoGetter.class);
                        intent.putExtra("isLocal", false);
                        intent.putExtra("isCreate", false);
                        intent.putExtra("platformID", platformID);
                        intent.putExtra("bookName", item.getBookName());
                        mContext.startService(intent);
                    }
                    bdb.writeItem(Constants.TAB_BOOK, item);
                    bookList.add(bookList.size() - 1, item);
                }
                bookAdapter = new BookAdapter(mContext, bookList);
                lv_book.setAdapter(bookAdapter);
            }
        };
    }

    private Handler setInfoHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    BookItem bookItem = (BookItem) msg.obj;
                    bookList.add(bookItem);
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
