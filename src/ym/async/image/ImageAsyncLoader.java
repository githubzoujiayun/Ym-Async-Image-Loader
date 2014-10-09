package ym.async.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageAsyncLoader {  
    private static final String TAG = "ImageAsyncLoader";
    
    /**
     * 根据图片地址获取Bitmap图像
     * 
     * @param path 缓存路径
     * @param url 图片地址
     * @param width 计划宽度
     * @param height 计划高度
     * @param force 强制更新：为True时 即使缓存存在，也从网络获取图片
     * @return
     */
    public static Bitmap getBitmap(String path, String url, int width,
            int height, boolean force) {
        Bitmap bitmapNet = null;
        Bitmap bitmap = null;
        if(!force) {
            bitmap = ImageAsyncFile.readCache(path, url, width, height);
        }
        if(bitmap == null) {
            bitmapNet = getBitmap(path, url);
            bitmap = ImageAsyncFile.readCache(path, url, width, height);
            Log.i(TAG, "Read from net:" + url);
        } else {
            Log.i(TAG, "Read from cache:" + url);
        }
        if(bitmap == null) {
            bitmap = bitmapNet;
            Log.i(TAG, "Write image to cache error:" + url);
        }
        return bitmap;
    }
    
    /**
     * 根据图片地址获取Bitmap图像
     * 
     * @param path 缓存路径
     * @param url 图片地址
     * @param force 强制更新：为True时 即使缓存存在，也从网络获取图片
     * @return
     */
    public static Bitmap getBitmap(String path, String url, boolean force) {
        Bitmap bitmapNet = null;
        Bitmap bitmap = null;
        if(!force) {
            bitmap = ImageAsyncFile.readCache(path, url);
        }
        if(bitmap == null) {
            bitmapNet = getBitmap(path, url);
            bitmap = ImageAsyncFile.readCache(path, url);
            Log.i(TAG, "Read from net:" + url);
        } else {
            Log.i(TAG, "Read from cache:" + url);
        }
        if(bitmap == null) {
            bitmap = bitmapNet;
            Log.i(TAG, "Write image to cache error:" + url);
        }
        return bitmap;
    }

    /**
     * 根据图片地址获取Bitmap图像
     * 
     * @param path 缓存路径
     * @param url 图片地址
     * @return
     */  
    private static Bitmap getBitmap(String path, String url) {
        Bitmap bitmap = null;
        try {
            URL imageUri = new URL(url);
            HttpURLConnection conn
                = (HttpURLConnection) imageUri.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
            ImageAsyncFile.writeCache(path, url, bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
} 