package com.xuanniao.reader.ui.book;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.xuanniao.reader.R;
import com.xuanniao.reader.getter.BookGetter;
import com.xuanniao.reader.getter.CatalogGetter;
import com.xuanniao.reader.getter.InfoGetter;
import com.xuanniao.reader.item.BookItem;
import com.xuanniao.reader.item.PlatformItem;
import com.xuanniao.reader.ui.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchFragment extends Fragment {
    private static final String Tag = "搜索页面";
    private Context context;
    TabLayout tabs_search;
    ViewPager pagerSearch;
    private Spinner sp_isExact;
    private ResultPagesAdapter resultPagesAdapter;
    private SearchView svSearch;
    private SharedPreferences spConfig;
    List<List<BookItem>> resultList;
    private ImageButton btn_settings;
    public static Handler handler_setResult, handler_setPlatform, handler_info;
    private String[] searchModArray;
    int mNum, searchMod = 0;

    static SearchFragment newInstance(int num) {
        SearchFragment f = new SearchFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("num", num);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNum = getArguments() != null ? getArguments().getInt("num") : 1;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_search, container, false);
        sp_isExact = fragmentView.findViewById(R.id.sp_isExact);
        svSearch = fragmentView.findViewById(R.id.sv_search);
        tabs_search = fragmentView.findViewById(R.id.tabs_search);
        pagerSearch = fragmentView.findViewById(R.id.pager_search);
        btn_settings = fragmentView.findViewById(R.id.btn_settings);
        handler_setResult = setResultHandler();
        handler_info = setInfoHandler();
        handler_setPlatform= setPlatformHandler();
        return fragmentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BookActivity bookActivity = (BookActivity) context;
        spConfig = PreferenceManager.getDefaultSharedPreferences(bookActivity);
        List<PlatformItem> platformList = bookActivity.getPlatformList();
        resultPagesAdapter = new ResultPagesAdapter(context, getChildFragmentManager(), platformList);
        pagerSearch.setAdapter(resultPagesAdapter);
        tabs_search.setupWithViewPager(pagerSearch);
        if (platformList != null) resultList = setDefultResultList(platformList.size());
        searchModArray = getResources().getStringArray(R.array.searchMod);
        ArrayAdapter adapter = new ArrayAdapter(context, R.layout.spinner_item, searchModArray);
        sp_isExact.setAdapter(adapter);
        sp_isExact.setSelection(Integer.parseInt(spConfig.getString("searchMod", "0")));
        sp_isExact.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = parent.getItemAtPosition(position).toString();
                if (s.equals("精确")) {
                    Toast.makeText(context, s + "搜索：书目代码", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, s + "搜索：书名", Toast.LENGTH_SHORT).show();
                }
                searchMod = position;
                Log.d(Tag, "searchMod:" + position);
            }
            // 只有当patent中的资源没有时，调用此方法
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        svSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                svSearch.onActionViewExpanded();
            }
        });
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String query) {
                Log.d(Tag, "搜索:" + query);
                int platformID = resultPagesAdapter.getPageNum();
                Intent intent;
                if (searchMod == 0) {
                    intent = new Intent(context, BookGetter.class);
                    intent.putExtra("bookName", query);
                } else {
                    Log.d(Tag, "platformID:" + platformID);
//                    if (platformList != null && Objects.equals(platformList.get(platformID)
//                            .getAccurateSearch(), "catalog")) {
//                        intent = new Intent(context, CatalogGetter.class);
//                    } else {
                        intent = new Intent(context, InfoGetter.class);
                        intent.putExtra("num", 0);
//                    }
                    intent.putExtra("bookCode", query);
                }
                intent.putExtra("isLocal", false);
                intent.putExtra("platformID", platformID);
                context.startService(intent);
                svSearch.clearFocus();
                return true;
            }
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SettingActivity.class);
                startActivity(intent);
            }
        });
    }

    private Handler setResultHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    int platformID = msg.arg1;
                    Log.d(Tag, "platformID:" + platformID);
                    List<BookItem> list = (List<BookItem>) msg.obj;
                    if (list != null && !list.isEmpty()) {
                        int cache = 1;
                        int currentPage = resultPagesAdapter.getPageNum();
                        int min = Math.max(currentPage - cache, 0);
                        int max = Math.min(resultPagesAdapter.getCount(), currentPage + cache);

                        if (min <= platformID && platformID <= max) {
                            resultList.add(platformID - 1, list);
                            BookListFragment rf = resultPagesAdapter.getFragment(platformID);
                            rf.setResultList(list);
                        }

                        if (msg.arg2 == 0) {
                            for (int i = 0; i < list.size(); i++) {
                                BookItem bookItem = list.get(i);
                                Intent intent = new Intent(context, InfoGetter.class);
                                intent.putExtra("isNeedSave", false);
                                intent.putExtra("platformID", platformID);
                                intent.putExtra("num", i);
                                intent.putExtra("bookName", bookItem.getBookName());
                                intent.putExtra("bookCode", bookItem.getBookCode());
                                context.startActivity(intent);
                            }
                        }
                    }
                } else if (msg.what == 2) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "程序BUG", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private Handler setInfoHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    int platformID = msg.arg1;
                    int num = msg.arg2;

                    int cache = 1;
                    int currentPage = resultPagesAdapter.getPageNum();
                    int min = Math.max(currentPage - cache, 0);
                    int max = Math.min(resultPagesAdapter.getCount(), currentPage + cache);

                    if (min <= platformID && platformID <= max) {
                        BookItem bookItem = (BookItem) msg.obj;
                        Log.d(Tag, "bookName:" + bookItem.getBookName());
                        Log.d(Tag, "platform:" + bookItem.getPlatformName());
//                        List<BookItem> list = resultList.get(platformID - 1);
//                        list.set(num, bookItem);
                        List<BookItem> list = new ArrayList<>();
                        list.add(bookItem);
//                        resultList.remove(platformID - 1);
//                        resultList.add(platformID - 1, list);
                        BookListFragment rf = resultPagesAdapter.getFragment(platformID);
                        rf.setResultList(list);
                    }
                } else if (msg.what == 2) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "程序BUG", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private Handler setPlatformHandler() {
        return new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    int platformID = msg.arg1;
                    Log.d(Tag, "platformID:" + platformID);
                    List<PlatformItem> list = (List<PlatformItem>) msg.obj;
                    resultPagesAdapter.setFragment(list);
                    tabs_search.setupWithViewPager(pagerSearch);
                    resultPagesAdapter.notifyDataSetChanged();
//                    int start = tabs_search.getTabCount();
//                    Log.d(Tag, "start:" + start);
//                    int size = list.size();
//                    if (start < size) {
//                        for (int i = start; i < size; i++) {
//                            Log.d(Tag, "i:" + i);
//                            TabLayout.Tab tab = new TabLayout.Tab();
//                            tab.setText(list.get(i).getPlatformName());
//                            tabs_search.addTab(tab);
//                        }
//                    }
//                    if (start > size) {
//                        for (int i = 0; i < size; i++) {
//                            Log.d(Tag, "i:" + i);
//                            TabLayout.Tab tab = tabs_search.getTabAt(i);
//                            tab.setText(list.get(i).getPlatformName());
//                        }
////                        for (int i = size; i <= start; i++) {
////                            tabs_search.removeTabAt(i);
////                        }
//                    }
                } else if (msg.what == 2) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "程序BUG", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public List<List<BookItem>> setDefultResultList(int n) {
        BookItem bookItem = new BookItem();
        bookItem.setBookName("无搜索结果");
        List<BookItem> bookItemList = new ArrayList<>();
        bookItemList.add(bookItem);
        List<List<BookItem>> resultList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            resultList.add(bookItemList);
//            BookListFragment rf = resultPagesAdapter.getFragment(i + 1);
//            rf.setResultList(bookItemList);
        }
        return resultList;
    }

    public List<BookItem> getResultList(int i) {
        if (resultList != null && resultList.size() > i) {
            Log.d(Tag, "第：" + i + "个页面来拿数据了");
            return this.resultList.get(i);
        } else {
            return null;
        }
    }
}