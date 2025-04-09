package com.xuanniao.reader.ui.book;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ResultPagesAdapter extends FragmentPagerAdapter {
    private static final String Tag = "ResultPager适配器";
    private Context context;
    private int mPageNum;
    private BookListFragment mCurrentFragment;
    private List<PlatformItem> platformList;
    private final List<BookListFragment> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentTitleList = new ArrayList<>();


    public ResultPagesAdapter(Context context, FragmentManager fm, List<PlatformItem> platformList) {
        super(fm);
        this.context = context;
        this.platformList = platformList;
        addFragment();
    }

    public void addFragment() {
        if (platformList == null || platformList.isEmpty()) {
            PlatformItem platformItem = new PlatformItem();
            platformItem.setPlatformName("无平台");
            mFragmentList.add(BookListFragment.newInstance(platformItem));
            mFragmentTitleList.add("无平台");
            return;
        }
        for (PlatformItem platformItem : platformList) {
            mFragmentList.add(BookListFragment.newInstance(platformItem));
            mFragmentTitleList.add(platformItem.getPlatformName());
        }
        Log.d(Tag, "platNum:" + mFragmentList.size());
    }

    public void setFragment(List<PlatformItem> platformList) {
        if (this.platformList != platformList) {
            this.mFragmentList.clear();
            this.mFragmentTitleList.clear();
            for (PlatformItem platformItem : platformList) {
                mFragmentList.add(BookListFragment.newInstance(platformItem));
                mFragmentTitleList.add(platformItem.getPlatformName());
            }
            notifyDataSetChanged();
        }
    }

    /** 初始化所有页面 */
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        mCurrentFragment = (BookListFragment) object;
        mPageNum = position;
        super.setPrimaryItem(container, position, object);
    }

    /** 新建页面 */
    @Override
    public Fragment getItem(int position) {
        // 调用getItem来实例化给定页面的fragment，返回ResultListFragment(定义为下面的静态内部类)
        Log.d(Tag, "getItem: " + position);
        Fragment fragment;
        if (position < mFragmentList.size() && position >= 0) {
            fragment = mFragmentList.get(position);
        } else {
            fragment = null;
        }
        return fragment;
    }

    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Log.d(Tag, "instantiateItem" + position);
        return super.instantiateItem(container, position);
    }

    public BookListFragment getCurrentFragment() {
        return mCurrentFragment;
    }

    public int getPageNum() {
        return mPageNum;
    }

    public BookListFragment getFragment(int pageNum) {
        BookListFragment rf = null;
        if (mFragmentList != null && mFragmentList.size() > 0) {
            rf = (BookListFragment) mFragmentList.get(pageNum);
        }
        return rf;
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

