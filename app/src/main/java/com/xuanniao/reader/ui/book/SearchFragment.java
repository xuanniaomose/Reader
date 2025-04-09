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
import com.xuanniao.reader.ui.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
    public static Handler handler_setResult, handler_setPlatform;
    private String[] searchModArray;
    int mNum, searchMod = 0;

    /**
     * Create a new instance of CountingFragment, providing "num"
     * as an argument.
     */
    static SearchFragment newInstance(int num) {
        SearchFragment f = new SearchFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("num", num);
        f.setArguments(args);

        return f;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
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

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
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
        handler_setPlatform= setPlatformHandler();
        return fragmentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BookActivity bookActivity = (BookActivity) context;
        spConfig = PreferenceManager.getDefaultSharedPreferences(bookActivity);
        List<PlatformItem> platformList = bookActivity.getPlatformList();
        resultList = new ArrayList<>();
        resultPagesAdapter = new ResultPagesAdapter(context, getChildFragmentManager(), platformList);
        pagerSearch.setAdapter(resultPagesAdapter);
        tabs_search.setupWithViewPager(pagerSearch);
        searchModArray = getResources().getStringArray(R.array.searchMod);
        ArrayAdapter adapter = new ArrayAdapter(context, R.layout.spinner_item, searchModArray);
        sp_isExact.setAdapter(adapter);
        sp_isExact.setSelection(Integer.parseInt(spConfig.getString("searchMod", "0")));
        sp_isExact.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = parent.getItemAtPosition(position).toString();
                if (s.equals("精确")) {
                    Toast.makeText(context, s + "搜索：使用书目代码进行搜索", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, s + "搜索：目前无法使用", Toast.LENGTH_SHORT).show();
                }
                searchMod = position;
            }
            //只有当patent中的资源没有时，调用此方法
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
//        EditText editText = svSearch.findViewById(androidx.appcompat.R.id.search_src_text);
//        editText.setTextColor(Color.WHITE);
////        editText.setHintTextColor(Color.GRAY);
//        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        svSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                svSearch.onActionViewExpanded();
            }
        });
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String query) {
                Log.d(Tag, "搜索:" + query);
//                resultPagesAdapter.search(query);
//                ResultListFragment f = resultPagesAdapter.getCurrentFragment();
                if (searchMod == 0) {
                    Toast.makeText(context, "搜索功能还没做完，请切换为精确搜索模式进行搜索", Toast.LENGTH_SHORT).show();
//                    Intent intent = new Intent(context, BookGet.class);
//                    intent.putExtra("isLocal", 0);
//                    intent.putExtra("bookName", query);
//                    intent.putExtra("chapterCode", -1);
//                    context.startService(intent);
                } else {
                    Intent intent = new Intent(context, CatalogActivity.class);
                    intent.putExtra("isLocal", 0);
                    intent.putExtra("bookCode", query);
                    intent.putExtra("platformID", resultPagesAdapter.getPageNum());
                    context.startActivity(intent);
                }
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
                        resultList.add(list);

                        int cache = 1;
                        int currentPage = resultPagesAdapter.getPageNum();
                        int min = Math.max(currentPage - cache, 0);
                        int max = Math.min(resultPagesAdapter.getCount(), currentPage + cache);

                        if (min <= platformID && platformID <= max) {
                            BookListFragment rf = resultPagesAdapter.getFragment(platformID);
                                rf.setResultList(list);
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

    public List<BookItem> getResultList(int i) {
        if (resultList != null && resultList.size() > i) {
            Log.d(Tag, "第：" + i + "个页面来拿数据了");
            return this.resultList.get(i);
        } else {
            return null;
        }
    }
}