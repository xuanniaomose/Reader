package com.xuanniao.reader.getter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CoverGetter extends AsyncTask<String, Void, Bitmap> {
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
        try {
            // 1. 检查本地文件
            File imageFile = getImageFile(imageUrl);
            if (imageFile.exists()) {
                return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            }

            // 2. 网络下载
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(12000); // 15秒超时
            connection.setReadTimeout(12000);
            connection.setDoInput(true);
            connection.connect();

            // 3. 下载并保存到本地
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            saveBitmapToLocal(imageUrl, bitmap); // 保存到本地
            return bitmap;

        } catch (IOException e) {
            e.printStackTrace();
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

    // 保存到本地文件
    private void saveBitmapToLocal(String url, Bitmap bitmap) {
        try {
            File imageFile = getImageFile(String.valueOf(url.hashCode()));
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos); // 压缩质量80%
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取本地文件路径
    private File getImageFile(String url) {
        String filename = String.valueOf(url.hashCode());
        return new File(context.getCacheDir(), filename);
    }
}
