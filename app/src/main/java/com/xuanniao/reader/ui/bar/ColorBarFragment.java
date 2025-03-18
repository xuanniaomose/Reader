package com.xuanniao.reader.ui.bar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.ColorSelector;
import com.xuanniao.reader.tools.Tools;
import com.xuanniao.reader.ui.ChapterActivity;

public class ColorBarFragment extends Fragment {
    private final String Tag = "ColorBarFragment";
    private ChapterActivity docActivity;
    private View mColorBarFl;
    private ImageButton btn_colorBarBack;
    private ColorSelector colorSelector;
    private SeekBar sb_textSize;
    private TextView tv_textSize;
    private Switch sw_light;
    SharedPreferences sp;
    SharedPreferences.Editor spEditor;

    public static ColorBarFragment newInstance() {
        return new ColorBarFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mColorBarFl = inflater.inflate(R.layout.bar_color, container, false);
        docActivity = (ChapterActivity) getActivity();
        if (docActivity != null) {
//            sp = docActivity.getSharedPreferences("info", MODE_PRIVATE);
            sp = PreferenceManager.getDefaultSharedPreferences(docActivity);
        }
        spEditor = sp.edit();
        init();
        setListener();
        return mColorBarFl;
    }

    private void init() {
        if (mColorBarFl != null) {
            Log.d(Tag,"成功获取mNoteBarFl");
            btn_colorBarBack = mColorBarFl.findViewById(R.id.btn_colorBarBack);
            sb_textSize = mColorBarFl.findViewById(R.id.sb_textSize);
            tv_textSize = mColorBarFl.findViewById(R.id.tv_textSize);
            colorSelector = mColorBarFl.findViewById(R.id.colorSelector);
            String[] colorList = {"gray_300", "gray_700", "black", "red_700",
                    "yellow_700", "green_700", "blue_500", "blue_700", "purple_200"};
            Log.d(Tag, "tv_textSize:" + tv_textSize);
            Log.d(Tag, "colorSelector:" + colorSelector);
            colorSelector.setItemByList(colorList, 0);
            sw_light = mColorBarFl.findViewById(R.id.sw_light);
            sw_light.setChecked(sp.getBoolean("readLight", true));
        }
    }

    private void setListener() {
        btn_colorBarBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                backToTool();
            }
        });
        colorSelector.setItemClickListener(new ColorSelector.ItemClickListener() {
            @Override
            public void onClick(ColorSelector.ColorTab tab, int position) {
                // makeText的第二个参数，即输出参数，也要求必须是string
                Toast.makeText(getActivity(), String.valueOf(tab.getColorInt()), Toast.LENGTH_SHORT).show();
                docActivity.setTextColor(tab.getColorInt());
            }
        });
        sb_textSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {}
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                docActivity.setTextSize(progress);
                tv_textSize.setText(String.valueOf(progress));
            }
        });
        sw_light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String colorName = (b)? "white" : "black";
                docActivity.setReadBackground(Tools.getIntColor(docActivity, colorName));
            }
        });
    }

    private void backToTool() {
        getParentFragmentManager().beginTransaction()
                .remove(this).commitNow();
    }
}
