package com.qrcode.demo;

import android.content.Context;
import android.os.Environment;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.thinkcore.TApplication;

import java.io.File;

public class MyApplication extends TApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader(this);
    }

    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you
        // may tune some of them,
        // or you can create default configuration by
        // ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(
                context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB

        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app

        File file = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (file != null && file.exists()) {
            String filePath = file.getAbsolutePath();
            config.discCache(new UnlimitedDiskCache(file));// 自定义缓存路径,图片缓存到sd卡
        }
        // if(TStorageUtils.)
        // TFilePath filePath = new TFilePath();
        // File cacheDir = ;
        // if (!TStringUtils.isEmpty(filePath)) {
        // cacheDir = StorageUtils.getOwnCacheDirectory(mContext, filePath);
        // }
        // else {
        // cacheDir = StorageUtils.getCacheDirectory(mContext);
        // }
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }
}
