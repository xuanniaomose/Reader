package com.xuanniao.reader.ui.bar;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.ReadControl;
import com.xuanniao.reader.tools.TTSService;
import com.xuanniao.reader.ui.ChapterActivity;

import java.lang.reflect.Field;

public class TTSBarFragment extends Fragment {
    private final String Tag = "TTSBarFragment";
    private ChapterActivity chapterActivity;
    private View mTTSBarFl;
    private Switch sw_followSystem, sw_continuous;
    private ImageButton btn_ttsBarBack;
    private SeekBar sb_speed, sb_pitch;
    private TextView tv_speedText, tv_pitchText;
    private TextView tv_speedNum, tv_pitchNum;
    SharedPreferences sp;
    SharedPreferences.Editor spEditor;
    private ReadControl readControl;
    private TTSService ttsService;
    private float ttsPitchNum, ttsSpeedNum;

    public static TTSBarFragment newInstance() {
        return new TTSBarFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mTTSBarFl = inflater.inflate(R.layout.bar_tts, container, false);
        chapterActivity = (ChapterActivity) getActivity();
        if (chapterActivity != null) {
//            sp = docActivity.getSharedPreferences("info", MODE_PRIVATE);
            sp = PreferenceManager.getDefaultSharedPreferences(chapterActivity);
            spEditor = sp.edit();
        }
        init();
        setListener();
        return mTTSBarFl;
    }

    private void init() {
        readControl = chapterActivity.getReadControl();
        ttsSpeedNum = sp.getFloat("speed", 1.0f);
        ttsPitchNum = sp.getFloat("pitch", 1.0f);
        if (mTTSBarFl != null) {
            sb_speed = mTTSBarFl.findViewById(R.id.sb_speed);
            tv_speedNum = mTTSBarFl.findViewById(R.id.tv_speedNum);
            tv_speedText = mTTSBarFl.findViewById(R.id.tv_speedText);
            sb_pitch = mTTSBarFl.findViewById(R.id.sb_pitch);
            tv_pitchNum = mTTSBarFl.findViewById(R.id.tv_pitchNum);
            tv_pitchText = mTTSBarFl.findViewById(R.id.tv_pitchText);

            btn_ttsBarBack = mTTSBarFl.findViewById(R.id.btn_ttsBarBack);
            sw_followSystem = mTTSBarFl.findViewById(R.id.sw_followSystem);
            sw_followSystem.setChecked(sp.getBoolean("followSystem", false));
            if (chapterActivity != null) {
                ttsService = readControl.getTTSService();
                // 设置语速
                ttsService.setSpeechRate(ttsSpeedNum);
                sb_speed.setProgress((int) (ttsSpeedNum * 10));
                // 设置语调
                ttsService.setPitch(ttsPitchNum);
                sb_pitch.setProgress((int) (ttsPitchNum * 10));
            }
            sw_continuous = mTTSBarFl.findViewById(R.id.sw_continuous);
            sw_continuous.setChecked(sp.getBoolean("isContinuousRead", false));
        }
    }

    private void setListener() {
        btn_ttsBarBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                backToTool();
            }
        });
        sw_followSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    disAbleSeekBar();
                    spEditor.putBoolean("followSystem", true).apply();
                } else {
                    enAbleSeekBar();
                    spEditor.putBoolean("followSystem", false).apply();
                }
            }
        });
        sw_continuous.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    ttsService.setContinuousRead(true);
                    spEditor.putBoolean("isContinuousRead", true).apply();
                } else {
                    ttsService.setContinuousRead(false);
                    spEditor.putBoolean("isContinuousRead", false).apply();
                }
            }
        });
        sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float speed = (float) seekBar.getProgress() / 10;
                ttsService.setSpeechRate(speed);
            }
        });
        sb_pitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float pitch = (float) seekBar.getProgress() / 10;
                ttsService.setPitch(pitch);
            }
        });
        // ToggleButton的使用方法
//        ttsService.setOnUtteranceProgressListener(new UtteranceProgressListener() {
//            @Override
//            public void onStart(String utteranceId) {
//                ttsPlay_bt.setChecked(true);
//            }
//            @Override
//            public void onDone(String utteranceId) {
//                ttsPlay_bt.setChecked(false);
//            }
//            @Override
//            public void onError(String utteranceId) {}
//            @Override
//            public void onError(String utteranceId, int errorCode) {}
//        });
    }

    private void backToTool() {
        getParentFragmentManager().beginTransaction()
                .remove(this).commitNow();
    }

    private void enAbleSeekBar() {
        ttsSpeedNum = sp.getFloat("speed", 1.0f);
        tv_speedText.setTextColor(getIntColor("textColorPrimary"));
        sb_speed.setEnabled(true);
        sb_speed.setClickable(true);
        sb_speed.setFocusable(true);
        tv_speedNum.setTextColor(getIntColor("textColorPrimary"));
        tv_speedNum.setText(String.valueOf(ttsSpeedNum));

        ttsPitchNum = sp.getFloat("pitch", 1.0f);
        tv_pitchText.setTextColor(getIntColor("textColorPrimary"));
        sb_pitch.setEnabled(true);
        sb_pitch.setClickable(true);
        sb_pitch.setFocusable(true);
        tv_pitchNum.setTextColor(getIntColor("textColorPrimary"));
        tv_pitchNum.setText(String.valueOf(ttsSpeedNum));
    }

    private void disAbleSeekBar() {
        tv_speedText.setTextColor(getIntColor("textColorSecondary"));
        sb_speed.setEnabled(false);
        sb_speed.setClickable(false);
        sb_speed.setFocusable(false);
        tv_speedNum.setTextColor(getIntColor("textColorSecondary"));

        tv_pitchText.setTextColor(getIntColor("textColorSecondary"));
        sb_pitch.setEnabled(false);
        sb_pitch.setClickable(false);
        sb_pitch.setFocusable(false);
        tv_pitchNum.setTextColor(getIntColor("textColorSecondary"));
    }

    int getIntColor(String colorName) {
        int defColor = 0x00000000;
        try {
            Class<R.color> res = R.color.class;//获取color类
            Field field = res.getField(colorName);
            // 获取colorName为color_xx的color id
            int colorId = field.getInt(null);
            // 通过id获取颜色的int值
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                defColor = chapterActivity.getResources().getColor(
                        colorId, getContext().getTheme());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(Tag, e.getMessage());
        }
        return defColor;
    }
}
