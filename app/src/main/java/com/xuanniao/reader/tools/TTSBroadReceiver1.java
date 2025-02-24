package com.xuanniao.reader.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;


//public class TTSBroadReceiver1 extends BroadcastReceiver {
//    private static String Tag = "TTSBroadReceiver";
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        Log.d(Tag, "收到消息");
//        Log.d(Tag, "action: " + intent.getAction());
//        switch (intent.getAction()) {
//            case Constants.ACTION_PARAGRAPH:
//                // 朗读段落
//                paragraphNum = intent.getIntExtra("paragraphNum", 0);
//                Log.i(Tag, "列表:" + paragraphList);
//                Log.i(Tag, "朗读指定段落:" + paragraphNum);
//                ttsStart(paragraphNum);
//                break;
//            case Constants.ACTION_PAUSE:
//                Log.i(Tag, "暂停");
//                // 暂停播放
//                ttsPause();
//                break;
//            case Constants.ACTION_READ:
//                Log.i(Tag, "播放");
//                // 开始播放
//                if (localTTS != null) {
//                    Log.i(Tag, "paragraphList: " + paragraphList);
//                    //localTTS.seekTo(mCurrentPosition);
//                    ttsPrepared();
//                } else {
//                    localTTS = new TextToSpeech(context.getApplicationContext(),
//                            new TTSService.TTSOnInitListener());
//                    ttsStart(paragraphNum);
//                }
//                break;
//            case Constants.ACTION_SEEK:
//                Log.i(Tag, "空降");
//                // 空降至
//                int position = intent.getIntExtra(Constants.PARAGRAPH_NUM, 0);
//                ttsStart(position);
//                break;
//            case Constants.ACTION_NEXT:
//                Log.i(Tag, "下一段");
//                paragraphNum ++;
//                ttsStart(paragraphNum);
//                break;
//            case Constants.ACTION_PREVIOUS:
//                Log.i(Tag, "上一段");
//                paragraphNum --;
//                ttsStart(paragraphNum);
//                break;
//            case Constants.ACTION_CLOSE:
//                Log.i(Tag, "关闭");
//                ttsStop();
//
//                break;
//            case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
//                Log.i(Tag, "耳机拔出");
//                // 如果耳机拨出时暂停播放
//                Intent intent_pause = new Intent();
//                intent_pause.setAction(Constants.ACTION_PAUSE);
//                sendBroadcast(intent_pause);
//                break;
//        }
//    }
//}
