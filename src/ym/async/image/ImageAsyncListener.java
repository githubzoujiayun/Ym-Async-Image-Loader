package ym.async.image;

import android.graphics.Bitmap;

public interface ImageAsyncListener {
    void onImageLoaded(String url, Bitmap bitmap);  
}