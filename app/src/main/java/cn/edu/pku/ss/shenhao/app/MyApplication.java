package cn.edu.pku.ss.shenhao.app;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.ss.shenhao.bean.City;
import cn.edu.pku.ss.shenhao.db.CityDB;

/**
 * Created by striker on 2016/12/22.
 */
public class MyApplication extends Application {
    private static final String TAG = "MyApp";
    private static MyApplication myApplication;
    private CityDB mCityDB;
    private List<City> mCityList;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyApplication -> onCreate");

        myApplication = this;
        mCityDB = openCityDB();
        initCityList();
    }

    //获取数据库内容
    private CityDB openCityDB() {
        String path = "/data"
                + Environment.getDataDirectory().getAbsolutePath()
                + File.separator + getPackageName()
                + File.separator + "databases1"
                + File.separator
                + CityDB.CITY_DB_NAME;
        File db = new File(path);
        Log.d(TAG, path);

        // 如果不存在数据库路径和文件则从 Assets 中写入到指定位置
        if (!db.exists()) {
            String pathfolder = "/data"
                    + Environment.getDataDirectory().getAbsolutePath()
                    + File.separator + getPackageName()
                    + File.separator + "databases1"
                    + File.separator;
            File dirFirstFolder = new File(pathfolder);
            if (!dirFirstFolder.exists()) {
                dirFirstFolder.mkdirs();
                Log.i(TAG, "Make database directory");
            }
            try {
                InputStream is = getAssets().open("city.db");
                FileOutputStream fos = new FileOutputStream(db);
                int len;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    fos.flush();
                }
                fos.close();
                is.close();
                Log.i(TAG, "Create db file here");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        return new CityDB(this, path);
    }

    // 使用多线程进行初始化
    private void initCityList() {
        mCityList = new ArrayList<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mCityList = mCityDB.getAllCity();
            }
        }).start();
    }

    public List<City> getmCityList() {
        return mCityList;
    }
}