package com.xuanniao.reader.ui.book;

import android.content.Context;
import android.os.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.xuanniao.reader.R;
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.item.PlatformItem;
import com.xuanniao.reader.ui.*;

import java.util.ArrayList;
import java.util.List;

public class BookListFragment extends Fragment {
    private static final String Tag = "BookListFragment页面";
    protected Context context;
    PlatformItem platformItem;
    private static int platformNum;
    private List<BookItem> resultList;
    private BookAdapter bookAdapter;
    private ListView lv_result;
    ViewGroup container;
    View fragmentView;

    public static BookListFragment newInstance(PlatformItem item) {
        BookListFragment fragment = new BookListFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("platformItem", item);
        Log.d(Tag, "新页面:" + item.getID());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,Bundle savedInstanceState) {
        platformItem = getArguments().getParcelable("platformItem");
        platformNum = platformItem.getID();
        Log.d(Tag, "platformNum:" + platformItem.getID());
        fragmentView = inflater.inflate(R.layout.fragment_book, container, false);
        Log.d(Tag, "onCreateView " + platformNum);
        this.container = container;
        lv_result = fragmentView.findViewById(R.id.lv_book);
        return fragmentView;
    }

    @Override
    public void onResume() {
        Log.d(Tag, platformNum + "载入元素");
        super.onResume();
        SearchFragment sf = (SearchFragment) getParentFragment();
        if (sf != null) {
            List<BookItem> list = sf.getResultList(platformNum);
            if (list != null && !list.isEmpty()) {
                setResultList(list);
                resultList = list;
                Log.d(Tag, "resultList:" + resultList);
            }
        }
        if (resultList == null) {
            setDefList(lv_result);
        }
    }

    public void setDefList(ListView lvResult) {
        if (resultList == null) {
            resultList = new ArrayList<>();
            BookItem bookItem = new BookItem();
            bookItem.setBookName("无搜索结果");
            resultList.add(bookItem);
        }
        Log.d(Tag, platformNum + "默认列表：" + resultList.get(0).getBookName());
        bookAdapter = new BookAdapter(context, resultList);
        lvResult.setAdapter(bookAdapter);
    }

    /**
     *在布局加载后执行(有可能布局还不可见)，建议在此方法内加载数据和处理布局显示数据
     */
    public void setResultList(List<BookItem> list) {
        if (resultList == list) {
            Log.d(Tag, platformNum + "无需重新加载");
            return;
        }
        if (resultList != null) {
            Log.d(Tag, platformNum + "重新加载列表");
            resultList.clear();
        }
        resultList = list;
        bookAdapter = new BookAdapter(context, resultList);
        lv_result.setAdapter(bookAdapter);
        Log.d(Tag, "resultList.size():" + resultList.size());
        Log.d(Tag, "platform:" + resultList.get(0).getPlatformName());
        BookActivity bookActivity = (BookActivity) getActivity();
        lv_result.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BookItem item = resultList.get(i);
                String platformName = item.getPlatformName();
//                Log.d(Tag, "platform:" + platformName + " | bookName:" +
//                        item.getBookName() + " | bookCode:" + item.getBookCode());
                bookActivity.openBook(false, platformName, item);
            }
        });
        bookAdapter.notifyDataSetInvalidated();
    }

    @Override
    public void onDestroyView() {
        if (fragmentView != null) {
            container.removeView(fragmentView);
        }
        super.onDestroyView();
    }
}
