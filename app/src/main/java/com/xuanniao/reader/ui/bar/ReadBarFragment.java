package com.xuanniao.reader.ui.bar;

import android.content.Intent;
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
import com.xuanniao.reader.tools.Constants;
import com.xuanniao.reader.tools.TTSService;
import com.xuanniao.reader.ui.ChapterActivity;

import java.lang.reflect.Field;
import java.util.Objects;

import static android.content.Context.BIND_AUTO_CREATE;

public class ReadBarFragment extends Fragment {
    private final String Tag = "ReadBarFragment";
    private ChapterActivity chapterActivity;
    private View readBarFl;
    private Button btn_exit, btn_rewind, btn_pause, btn_forward, btn_reader;
    SharedPreferences sp;
    SharedPreferences.Editor spEditor;

    public static ReadBarFragment newInstance() {
        return new ReadBarFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        readBarFl = inflater.inflate(R.layout.bar_read, container, false);
        chapterActivity = (ChapterActivity) getActivity();
        if (chapterActivity != null) {
//            sp = docActivity.getSharedPreferences("info", MODE_PRIVATE);
            sp = PreferenceManager.getDefaultSharedPreferences(chapterActivity);
            spEditor = sp.edit();
        }
        init();
//        Intent serviceIntent = new Intent(this, TTSService.class);
//        serviceIntent.putExtra("tts_speed", speed);
//        serviceIntent.putExtra("tts_pitch", pitch);
//        serviceIntent.putExtra("continuousRead", sp.getBoolean("continuousRead", false));
//        chapterActivity.bindService(serviceIntent, conn, BIND_AUTO_CREATE);
        return readBarFl;
    }

    private void init() {
        if (readBarFl != null) {
            btn_rewind = readBarFl.findViewById(R.id.btn_rewind);
            btn_rewind.setOnClickListener(view ->
                    ttsAction(Constants.ACTION_PREVIOUS, null));
            btn_forward = readBarFl.findViewById(R.id.btn_forward);
            btn_forward.setOnClickListener(view ->
                    ttsAction(Constants.ACTION_NEXT, null));
            btn_pause = readBarFl.findViewById(R.id.btn_pause);
            btn_pause.setOnClickListener(view -> {
//                if (ttsState == 1) {
//                    ttsAction(Constants.ACTION_PAUSE, null);
//                } else if (ttsState == 2) {
//                    ttsAction(Constants.ACTION_READ, null);
//                }
            });
            btn_exit = readBarFl.findViewById(R.id.btn_exit);
            btn_exit.setOnClickListener(view -> {
//                if (mTTSService != null) ttsAction(Constants.ACTION_STOP, null);
            });
            btn_reader = readBarFl.findViewById(R.id.btn_reader);
            btn_reader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    if (barState != 2) controlAction(2);
                }
            });
        }
    }

    public void ttsAction(String action, Integer index) {
//        if (!mTTSService.getParagraphListExist()) {
//            Log.d(Tag, "段落列表：" + paragraphList);
//            if (paragraphList != null && !paragraphList.isEmpty())
//                this.startTTSService(paragraphList);
//            action = (Objects.equals(action, Constants.ACTION_READ))?
//                    Constants.ACTION_PARAGRAPH : action;
//            index = nowReading;
//            Log.d(Tag, "action:" + action);
//        }
//        if (paragraphList != null && !paragraphList.isEmpty()) {
//            Intent intent = new Intent();
//            intent.setAction(action);
//            intent.setPackage("com.xuanniao.reader");
//            Log.d(Tag, "列表项：" + index);
//            if (index != null) {
//                intent.putExtra("paragraphNum", index);
//                intent.putExtra("tts_pitch", pitch);
//                intent.putExtra("tts_speed", speed);
//                intent.putExtra(Constants.ACTION_PARAGRAPH, index);
//            }
//            sendBroadcast(intent);
//        } else {
//            Toast.makeText(ChapterActivity.this,
//                    "当前播放列表为空", Toast.LENGTH_SHORT).show();
//        }
    }

    private void backToTool() {
        getParentFragmentManager().beginTransaction()
                .remove(this).commitNow();
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
