package ym.async.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageAsyncFile {
    private static final String TAG = "ImageAsynFile";
    
    /**
     * 缓存名称算法
     * 
     * @param url 远程图片地址
     * @return
     */
    public static String cacheName(String url) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(url.getBytes());
            byte[] m = md5.digest();
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < m.length; i ++) {
                sb.append(m[i]);  
            }  
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 not available!");
            return "DEFAULT";
        }
    }
    
    /**
     * 写入图片缓存
     * 
     * @param path 缓存路径
     * @param url 图片地址
     * @param bitmap 图片数据
     */
    public static void writeCache(String path, String url, Bitmap bitmap) {
        String filePath = path + File.separator + cacheName(url);
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 读取缓存图片
     * 
     * @param path 缓存路径
     * @param url 图片地址

     * @return
     */
    public static Bitmap readCache(String path, String url) {
        return readCache(path, url, 0, 0);
    }
    
    /**
     * 读取缓存图片
     * 
     * @param path 缓存路径
     * @param url 图片地址
     * @param width 计划宽度
     * @param height 计划高度
     * @return
     */
    public static Bitmap readCache(String path, String url, int width,
            int height) {
        Bitmap bitmap = null;
        String filePath = path + File.separator + cacheName(url);
        File cacheFile = new File(filePath);
        if(cacheFile.exists()) {
            if(width > 0 && height > 0) readBitmap(filePath, width, height);
            else bitmap = readBitmap(filePath);
        }
        return bitmap;
    }
    
    /**
     * 读出图片
     * 
     * @param path 图片路径
     * @return
     */
    private static Bitmap readBitmap(String path) {
        Bitmap bitmap = null;
        try {
            FileInputStream fis = new FileInputStream(path);
            bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
    
    /**
     * 读出图片
     * 
     * @param path 图片路径
     * @param width 计划宽度
     * @param height 计划高度
     * @return
     */
    private static Bitmap readBitmap(String path, int width, int height) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream fis = new FileInputStream(path);
            BitmapFactory. decodeStream(fis, null, o);
            fis.close();
            int widthIn = o.outWidth, heightIn = o.outHeight;
            int scale = 1;
            while(true) {
                if(widthIn / 2 < width || heightIn / 2 < height) break;
                widthIn  /= 2;
                heightIn /= 2;
                scale *= 2;
            }
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inSampleSize = scale;
            fis = new FileInputStream(path);
            bitmap = BitmapFactory.decodeStream(fis, null, op);
            fis.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}  