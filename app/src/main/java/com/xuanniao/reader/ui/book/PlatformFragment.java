package com.xuanniao.reader.ui.book;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.tools.FileTools;
import com.xuanniao.reader.tools.PlatformDB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlatformFragment extends Fragment {
    private static final String Tag = "平台设置页面";
    protected Context mContext;
    private PlatformDB pdb;
    private ListView lv_platform;
    Button btn_export, btn_addPlatformByFile;
    private boolean isFirstLoad;
    private List<PlatformItem> platformList;
    private PlatformAdapter platformAdapter;

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
        View fragmentView = inflater.inflate(R.layout.fragment_platform, container, false);
        lv_platform = fragmentView.findViewById(R.id.lv_platform);
        btn_addPlatformByFile = fragmentView.findViewById(R.id.btn_addPlatformByFile);
        btn_export = fragmentView.findViewById(R.id.btn_export);
        pdb = PlatformDB.getInstance(mContext, Constants.DB_PLATFORM);
        return fragmentView;
    }

    @Override
    public void onResume() {
        Log.d(Tag, "载入元素");
        super.onResume();
        if (isFirstLoad) {
            isFirstLoad = false;
            setList();
            btn_addPlatformByFile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/json");
                    startActivityForResult(intent, Constants.FILE_SELECT_CODE);
                }
            });
        }
        btn_export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentE = new Intent();
                intentE.setAction("export");
                intentE.setPackage("com.xuanniao.reader");
                intentE.putExtra("dbType", 1);
                mContext.startService(intentE);
            }
        });
    }

    public void setList() {
        Log.d(Tag, "setList");
        platformList = pdb.queryAll(Constants.TAB_PLATFORM);
        if (platformList == null || platformList.isEmpty()) {
            PlatformItem platformItem = new PlatformItem();
            platformItem.setPlatformName("平台1");
            platformList = new ArrayList<>();
            platformList.add(platformItem);
        }
        Log.d(Tag,  "平台列表：" + platformList.get(0).getPlatformName());
        platformAdapter = new PlatformAdapter(mContext, platformList);
//        BookActivity bookActivity = (BookActivity) getActivity();
        lv_platform.setAdapter(platformAdapter);
//        lv_platform.setOnItemClickListener((adapterView, view, i, l) -> {
//            PlatformItem platformItemL = platformList.get(i);
//            String platformName = platformItemL.getPlatformName();
//            String platformUrl = platformItemL.getPlatformUrl();
//            Log.d(Tag, "platformName:" + platformName + " | platformUrl" + platformUrl);
//            bookActivity.openPlatform(platformName, platformUrl);
//        });
    }

    @Override
    // 带回文件路径
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            Log.d(Tag, "uri:" + uri);
            List<PlatformItem> list = FileTools.loadLocalPlatformData(mContext, uri);
            if ((list != null && !list.isEmpty()) && (platformList != null && !platformList.isEmpty())) {
                platformList.clear();
                pdb.tabDelete(Constants.TAB_PLATFORM);
            }
            if (list != null) {
                for (PlatformItem item : list) {
                    Log.d(Tag, "写入平台:" + item.getPlatformName());
                    platformList = list;
                    pdb.writeItem(Constants.TAB_PLATFORM, item);
                }
                Log.d(Tag, "平台名称:" + platformList.get(0).getPlatformName());
                platformAdapter = new PlatformAdapter(mContext, platformList);
                lv_platform.setAdapter(platformAdapter);
                Message msg = new Message();
                msg.what = 1;
                msg.obj = platformList;
                SearchFragment.handler_setPlatform.sendMessage(msg);

                showFalseWindow();
            }
        }
    }

    private void showFalseWindow() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                //.setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("已完成平台导入")//设置对话框的标题
                .setMessage("请重启app")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("稍后", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("关闭app", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                }).create();
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        isFirstLoad = true;
        super.onDestroyView();
    }
}
