package com.xuanniao.reader.ui.book;

import android.content.Context;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.tools.PlatformDB;

import java.util.List;

public class PlatformAdapter extends ArrayAdapter<PlatformItem> {
    static String Tag = "PlatformAdapter";
    Context context;
    List<PlatformItem> platformList;
    private final int resourceID;
    public PlatformAdapter(Context context, List<PlatformItem> objects) {
        super(context, R.layout.platform_i, objects);
        this.context = context;
        this.platformList = objects;
        resourceID = R.layout.platform_i;
        Log.d("resourceID", String.valueOf(resourceID));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PlatformItem platformItem = getItem(position);//得到当前项的 PlatformItem 实例
        View item;
        ViewHolder viewHolder;
        if (convertView == null) {
            item = LayoutInflater.from(getContext()).inflate(resourceID,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.ll_titlePlatformExpand = item.findViewById(R.id.ll_platformExpand);
            viewHolder.iv_expand = item.findViewById(R.id.iv_expand);
            viewHolder.tv_titlePlatformName = item.findViewById(R.id.tv_titlePlatformName);
            viewHolder.tl_details = item.findViewById(R.id.tl_details);
            viewHolder.tv_platformName = item.findViewById(R.id.tv_platformName);
            viewHolder.tv_url = item.findViewById(R.id.tv_url);
            viewHolder.tv_searchPath = item.findViewById(R.id.tv_searchPath);
            viewHolder.tv_cookie = item.findViewById(R.id.tv_cookie);
            item.setTag(viewHolder);
        } else {
            item = convertView;
            viewHolder = (ViewHolder) item.getTag();
        }
        String platformName = platformItem.getPlatformName();
        platformName = (platformName == null || platformName.isEmpty())? "平台" : platformName;
        viewHolder.tv_titlePlatformName.setText(platformName);
        viewHolder.ll_titlePlatformExpand.setTag(position);
//        viewHolder.ll_platformExpand.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                PlatformItem platformItem = getItem((Integer)v.getTag());
//                Toast.makeText(getContext(),"点击了"+RecodeItem.getWord(),Toast.LENGTH_SHORT).show();
        viewHolder.tv_platformName.setText(platformName);
        String platformUrl = platformItem.getPlatformUrl();
        Log.d(Tag, "platformUrl:" + platformUrl);
        if (platformUrl != null && !platformUrl.isEmpty()) {
            viewHolder.tv_url.setText(platformUrl);
        }
        String searchPath = platformItem.getSearchPath();
        if (searchPath!= null && !searchPath.isEmpty()) {
            viewHolder.tv_searchPath.setText(searchPath);
        }
        String platformCookie = platformItem.getPlatformCookie();
        if (platformCookie != null && !platformCookie.isEmpty()) {
            viewHolder.tv_cookie.setText(platformCookie);
        }
//        String authorFormat = platformItem.getAuthorFormat();
//        if (authorFormat != null && !authorFormat.isEmpty()) {
//            viewHolder.et_authorFormat.setText(authorFormat);
//        }
//        String renewFormat = platformItem.getRenewFormat();
//        if (renewFormat != null && !renewFormat.isEmpty()) {
//            viewHolder.et_renewFormat.setText(renewFormat);
//        }
//        String synopsisFormat = platformItem.getSynopsisFormat();
//        if (synopsisFormat != null && !synopsisFormat.isEmpty()) {
//            viewHolder.et_synopsisFormat.setText(synopsisFormat);
//        }
//        String catalogFormat = platformItem.getCatalogListFormat();
//        if (catalogFormat != null && !catalogFormat.isEmpty()) {
//            viewHolder.et_catalogFormat.setText(catalogFormat);
//        }
//        String catalogError = platformItem.getCatalogError();
//        if (catalogError != null && !catalogError.isEmpty()) {
//            viewHolder.et_catalogError.setText(catalogError);
//        }
//        String titleFormat = platformItem.getTitleFormat();
//        if (titleFormat != null && !titleFormat.isEmpty()) {
//            viewHolder.et_titleFormat.setText(titleFormat);
//        }
//        String paragraphFormat = platformItem.getParagraphFormat();
//        if (paragraphFormat != null && !paragraphFormat.isEmpty()) {
//            viewHolder.et_paragraphFormat.setText(paragraphFormat);
//        }
//        String paragraphError = platformItem.getChapterError();
//        if (paragraphError != null && !paragraphError.isEmpty()) {
//            viewHolder.et_paragraphError.setText(paragraphError);
//        }
//            }
//        });
//        setTextWatcher(viewHolder.tv_platformName, position, "platformName");
//        setTextWatcher(viewHolder.tv_url, position, "platformUrl");
//        setTextWatcher(viewHolder.tv_cookie, position, "platformCookie");
//        setTextWatcher(viewHolder.et_authorFormat, position, "authorFormat");
//        setTextWatcher(viewHolder.et_renewFormat, position, "renewFormat");
//        setTextWatcher(viewHolder.et_synopsisFormat, position, "synopsisFormat");
//        setTextWatcher(viewHolder.et_catalogFormat, position, "catalogFormat");
//        setTextWatcher(viewHolder.et_catalogError, position, "catalogError");
//        setTextWatcher(viewHolder.et_titleFormat, position, "titleFormat");
//        setTextWatcher(viewHolder.et_paragraphFormat, position, "paragraphFormat");
//        setTextWatcher(viewHolder.et_paragraphError, position, "paragraphError");
        return item;
    }

//    private void setTextWatcher(EditText editText, int position, String key) {
//        editText.addTextChangedListener(new android.text.TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                Log.d(Tag, "onTextChanged");
//                editText.removeTextChangedListener(this);
//                String text = editText.getText().toString().trim();
//                Log.d(Tag, "text:" + text);
//                if (!text.isEmpty()) {
//                    pdb.updateItem(Constants.TAB_PLATFORM, position + 1, key, text);
//                }
//                editText.addTextChangedListener(this);
//            }
//            @Override
//            public void afterTextChanged(Editable editable) {
//                Log.d(Tag, "afterTextChanged");
//            }
//        });
//    }

    public void setList(List<PlatformItem> list) {
        this.platformList = list;
        notifyDataSetInvalidated();
    }

    static class ViewHolder {
        LinearLayout ll_titlePlatformExpand;
        ImageView iv_expand;
        TextView tv_titlePlatformName;
        TableLayout tl_details;
        TextView tv_platformName, tv_url, tv_searchPath, tv_cookie;
    }

}
