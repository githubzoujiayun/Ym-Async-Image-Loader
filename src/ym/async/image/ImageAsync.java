package ym.async.image;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

public class ImageAsync {
    private static final String TAG = "ImageAsyn";
    // 缓存位置
    private final String cachePath;
    // 内存缓存的Map
    private Map<String, SoftReference<Bitmap>> caches;
    // 任务队列
    private List<ImageAsyncTask> taskQueue;
    private boolean isRunning = false;
    
    public ImageAsync(Context context, String path) {
        String dir = null;
        if(Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            dir = context.getCacheDir().getAbsolutePath();
        }
        cachePath = dir + File.separator + path;
        init();
    }
    
    public ImageAsync(String path) {
        cachePath = path;
        init();
    }
    
    private void init() {
        File cacheDir = new File(cachePath);
        if(!cacheDir.exists()) cacheDir.mkdirs();
        // 初始化变量
        caches = new HashMap<String, SoftReference<Bitmap>>();
        taskQueue = new ArrayList<ImageAsyncTask>();
        // 启动图片下载线程
        isRunning = true;
        Thread thread = new Thread(daemon);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 关闭加载器
     */
    public void close() {
        isRunning = false;
        synchronized(daemon) {
            daemon.notify();
        }
    }
    
    /**
     * 异步加载图片
     * 
     * @param url 图片地址
     * @param callback 回调监听
     * @return
     */
    public void loadImageAsync(String url, ImageAsyncListener callback) {
        if(caches.containsKey(url)) {
            SoftReference<Bitmap> rf = caches.get(url);
            Bitmap bitmap = rf.get();
            if(bitmap == null){  
                caches.remove(url);  
            } else {
                Log.i(TAG, "Read image from memory:" + url);
                if(callback != null) callback.onImageLoaded(url, bitmap);
                return;  
            }  
        }
        ImageAsyncTask task = new ImageAsyncTask();
        task.url = url;
        task.callback = callback;
        if(!taskQueue.contains(task)) {
            taskQueue.add(task);
            synchronized(daemon) {
                daemon.notify();
            }
        }
    }
    
    /**
     * 基础图片回调
     * 
     * @param imageView 需要延迟加载图片的对象
     * @param url 图片的URL地址
     * @param resId 图片加载过程中显示的图片资源
     */  
    public void showImageAsync(ImageView imageView, String url, int resId) {
        imageView.setTag(url);
        loadImageAsync(url, getImageCallback(imageView, resId));
        imageView.setImageResource(resId);
    }
    
    /**
     * 获取基础加载回调
     * 
     * @param imageView 需要延迟加载图片的对象
     * @param resId 图片加载过程中以及失败后显示的图片资源
     * @return
     */
    private ImageAsyncListener getImageCallback(final ImageView imageView,
            final int resId) {  
        return new ImageAsyncListener() {
            @Override  
            public void onImageLoaded(String url, Bitmap bitmap) {  
                if(url.equals(imageView.getTag().toString())) {  
                    imageView.setImageBitmap(bitmap);  
                } else {  
                    imageView.setImageResource(resId);  
                }  
            }  
        };  
    }
    
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ImageAsyncTask task = (ImageAsyncTask) msg.obj;
            if(task.callback != null) task.callback.onImageLoaded(task.url,
                    task.bitmap);
        }
    };
    
    private Runnable daemon = new Runnable() {
        @Override
        public void run() {
            while(isRunning) {  
                while(taskQueue.size() > 0) {
                    ImageAsyncTask task = taskQueue.remove(0);
                    task.bitmap = ImageAsyncLoader.getBitmap(cachePath,
                            task.url, task.force);
                    // TODO : 这里出现一个小问题，在内存较小的手机上，缓存图片会很快耗光内存，
                    // 而SoftReference却没有自动释放，导致新图片无法装载进来。
                    // caches.put(task.url,
                    //     new SoftReference<Bitmap>(task.bitmap));  
                    if(handler != null) {
                        Message msg = handler.obtainMessage();  
                        msg.obj = task;
                        handler.sendMessage(msg);
                    }  
                }
                synchronized(this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {}
                }
            }
        }
    };
    

    /**
     * 放大缩小图片
     * 
     * @param bitmap 原始图片
     * @param width 目标跨度度
     * @param height 目标高度
     * @return
     */
    public static Bitmap getZoomImage(Bitmap bitmap, int width, int height) {  
        int w = bitmap.getWidth();  
        int h = bitmap.getHeight();  
        Matrix matrix = new Matrix();  
        float scaleWidht = ((float) width / w);  
        float scaleHeight = ((float) height / h);  
        matrix.postScale(scaleWidht, scaleHeight);  
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);  
        return newbmp;  
    }  
  
    /**
     * 将Drawable转化为Bitmap
     * 
     * @param drawable Drawable
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                drawable.getOpacity() != PixelFormat.OPAQUE
                ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);  
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
    
    /**
     * 获得圆角图片
     * 
     * @param bitmap 原始图片
     * @param scale 圆角比例
     * @return
     */
    public static Bitmap getRadiusImage(Bitmap bitmap, float centX, float centY) {
        if(bitmap == null) return null;
        return getRoundImage(bitmap, bitmap.getWidth() * centX, bitmap.getHeight() * centY);
    }
  
    /**
     * 获得圆角图片
     * 
     * @param bitmap 原始图片
     * @param roundPx 圆角像素
     * @param roundPy 圆角像素
     * @return
     */
    public static Bitmap getRoundImage(Bitmap bitmap, float roundPx,
            float roundPy) {
        if(bitmap == null) return null;
          
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Config.ARGB_8888);  
        Canvas canvas = new Canvas(output);
  
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPy, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
  
    /**
     * 获得带倒影的图片
     * 
     * @param bitmap 原始图片
     * @return
     */
    public static Bitmap getReflectionImage(Bitmap bitmap) {  
        final int reflectionGap = 4;  
        int width = bitmap.getWidth();  
        int height = bitmap.getHeight();  
  
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);
        
        Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, height / 2,
                width, height / 2, matrix, false);
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width,
                (height + height / 2), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapWithReflection);
        canvas.drawBitmap(bitmap, 0, 0, null);
        Paint deafalutPaint = new Paint();
        canvas.drawRect(0, height, width, height + reflectionGap,
                deafalutPaint);
        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0,
                bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff,
                0x00ffffff, TileMode.CLAMP);
        paint.setShader(shader);
        // Set the Transfer mode to be porter duff and destination in
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        // Draw a rectangle using the paint with our linear gradient
        canvas.drawRect(0, height, width, bitmapWithReflection.getHeight()
                + reflectionGap, paint);
        return bitmapWithReflection;
    }
}

class ImageAsyncTask {
    boolean force;
    String url;
    int width;
    int height;
    Bitmap bitmap;
    ImageAsyncListener callback;
      
    @Override
    public boolean equals(Object o) {
        ImageAsyncTask task = (ImageAsyncTask) o;
        return task.url.equals(url);
    }
}
