package com.xuanniao.reader.ui.book;

import android.util.Log;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainPagesAdapter extends FragmentPagerAdapter {
    private static final String Tag = "MainPages适配器";
    private static Fragment mCurrentFragment;
    private int mPageNum;
    private final List<Fragment> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentTitleList = new ArrayList<>();


    public MainPagesAdapter(FragmentManager fm) {
        super(fm);
        addFragment();
    }

    public void addFragment() {
        mFragmentList.add(new SearchFragment());
        mFragmentTitleList.add("搜索");
        mFragmentList.add(new LocalFragment());
        mFragmentTitleList.add("本地");
        mFragmentList.add(new PlatformFragment());
        mFragmentTitleList.add("平台");
    }

    /** 初始化所有页面 */
    @Override
    public void setPrimaryItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
        mCurrentFragment = (Fragment) object;
        mPageNum = position + 1;
        super.setPrimaryItem(container, position, object);
    }

    /** 新建页面 */
    @Override
    public Fragment getItem(int position) {
        // 调用getItem来实例化给定页面的fragment，返回ResultListFragment(定义为下面的静态内部类)
        Log.d(Tag, "getItem: " + position);
        return mFragmentList.get(position);
    }

    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Log.d(Tag, "instantiateItem" + position);
        return super.instantiateItem(container, position);
    }

    public Fragment getCurrentFragment() {
        return getFragment(mPageNum);
    }

    public int getPageNum() {
        return mPageNum;
    }

    public Fragment getFragment(int pageNum) {
        Fragment fragment = null;
        if (mFragmentList != null && mFragmentList.size() > 0) {
            fragment = mFragmentList.get(pageNum - 1);
        }
        return fragment;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    }
}
