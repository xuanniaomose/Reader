package com.xuanniao.reader.getter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import androidx.documentfile.provider.DocumentFile;
import com.xuanniao.reader.item.ChapterItem;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CoverGetter extends AsyncTask<String, Void, Bitmap> {
    private static final String Tag = "CoverGetter";
    private Context context;
    private boolean isOnlyDownload;
    private final WeakReference<ImageView> imageViewReference;
    private Map<String, WeakReference<ImageView>> mImageCache =
            new HashMap<String, WeakReference<ImageView>>();
    private String imageUrl; // 当前任务的图片URL

    public CoverGetter(Context context) {
        this.context = context;
        this.imageViewReference = null;
        this.isOnlyDownload = true;
    }

    public CoverGetter(Context context, ImageView imageView) {
        this.context = context;
        // 使用弱引用防止内存泄漏
        imageViewReference = new WeakReference<>(imageView);
        this.isOnlyDownload = false;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        imageUrl = params[0];
        String bookName = (params.length > 1)? params[1] : null;
        try {
            // 1. 检查本地缓存
            File file = getImageFile(imageUrl);
            if (!isOnlyDownload && file.exists()) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }

            if (!isOnlyDownload && bookName != null) {
                String fileName = "cover.jpg";
                DocumentFile imageFile = FileTools.getDocumentFile(context, bookName, fileName);
//                Log.d(Tag, "imageFile:" + imageFile);
                if (imageFile.exists()) {
                    try {
                        InputStream is = context.getContentResolver().openInputStream(imageFile.getUri());
                        Log.d(Tag, "InputStream:" + (is != null));
                        return BitmapFactory.decodeStream(is);
                    } catch (FileNotFoundException e) {
//                        throw new RuntimeException(e);
                        return null;
                    }
                }
            }

            // 2. 网络下载
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(12000); // 15秒超时
            connection.setReadTimeout(12000);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            Log.d(Tag, "下载完成");

            // 3. 保存到本地
//            saveBitmapToLocal(imageUrl, bitmap);
            if (bookName != null) bitmapSave(context, bookName, imageUrl, bitmap);
            Log.d(Tag, "图片过程完成");
            return bitmap;
        } catch (IOException e) {
//            e.printStackTrace();
            Log.e(Tag, "Error:" + e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isOnlyDownload) return;
        if (isCancelled() || bitmap == null) return;

        // 1. 获取关联的ImageView
        ImageView imageView = imageViewReference.get();
        if (imageView == null) return;

        // 2. 检查标记防止错位
        String currentUrl = (String) imageView.getTag();
        if (!imageUrl.equals(currentUrl)) return;

        // 3. 更新UI并缓存
        imageView.setImageBitmap(bitmap);
//        addBitmapToMemoryCache(imageUrl, bitmap);
        mImageCache.put(imageUrl, new WeakReference<>(imageView));

    }

    /**
     * 存储位图
     * @param context 上下文
     * @param bitmap 位图
     * @return 存储结果
     */
    public static int bitmapSave(Context context, String bookName, String url, Bitmap bitmap) {
//        String fileName = url.hashCode() + ".jpg";
        String fileName = "cover.jpg";
        Log.d(Tag, "bookName:" + bookName);
        try {
            DocumentFile imageFile = FileTools.getDocumentFile(context, bookName, fileName);
            if (imageFile == null) {
                Log.d(Tag, "图像路径错误");
                return -2;
            }
            OutputStream out = context.getContentResolver().openOutputStream(imageFile.getUri());
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out); // 压缩质量80%
            }
            out.flush();
            out.close();
            Log.d(Tag, "存储完成");
            return 0;
        } catch (IOException e) {
            Log.e(Tag, e.getMessage());
            Log.d(Tag, "存储过程错误");
            return -3;
        }
    }

    // 保存到本地文件
    private void saveBitmapToLocal(String url, Bitmap bitmap) {
        try {
            File imageFile = getImageFile(String.valueOf(url.hashCode()));
            FileOutputStream fos = new FileOutputStream(imageFile);
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos); // 压缩质量80%
            }
            fos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 获取本地文件路径
    private File getImageFile(String url) {
        String filename = String.valueOf(url.hashCode());
        return new File(context.getCacheDir(), filename);
    }
}
