package cn.edu.pku.ss.shenhao.miniweather;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.edu.pku.ss.shenhao.util.NetUtil;
import cn.edu.pku.ss.shenhao.bean.TodayWeather;
import cn.edu.pku.ss.shenhao.adapter.ViewPagerAdapter;
import cn.edu.pku.ss.shenhao.app.MyApplication;
import cn.edu.pku.ss.shenhao.bean.City;
import cn.edu.pku.ss.shenhao.bean.FutureWeather;
import cn.edu.pku.ss.shenhao.service.UpdateService;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

/**
 * Created by striker on 2016/11/29.
 */
public class MainActivity extends Activity implements View.OnClickListener, ViewPager.OnPageChangeListener{
    private static final int UPDATE_TODAY_WEATHER = 1;
    // HashMap存储天气与天气图片对应的值
    private static final HashMap<String, Integer> weatherhash = new HashMap<>();

    static {
        weatherhash.put("晴", R.drawable.biz_plugin_weather_qing);
        weatherhash.put("暴雪", R.drawable.biz_plugin_weather_baoxue);
        weatherhash.put("暴雨", R.drawable.biz_plugin_weather_baoyu);
        weatherhash.put("大暴雨", R.drawable.biz_plugin_weather_dabaoyu);
        weatherhash.put("大雪", R.drawable.biz_plugin_weather_daxue);
        weatherhash.put("大雨", R.drawable.biz_plugin_weather_dayu);
        weatherhash.put("多云", R.drawable.biz_plugin_weather_duoyun);
        weatherhash.put("雷阵雨", R.drawable.biz_plugin_weather_leizhenyu);
        weatherhash.put("雷阵雨冰雹", R.drawable.biz_plugin_weather_leizhenyubingbao);
        weatherhash.put("沙尘暴", R.drawable.biz_plugin_weather_shachenbao);
        weatherhash.put("特大暴雨", R.drawable.biz_plugin_weather_tedabaoyu);
        weatherhash.put("雾", R.drawable.biz_plugin_weather_wu);
        weatherhash.put("小雪", R.drawable.biz_plugin_weather_xiaoxue);
        weatherhash.put("小雨", R.drawable.biz_plugin_weather_xiaoyu);
        weatherhash.put("阴", R.drawable.biz_plugin_weather_yin);
        weatherhash.put("雨夹雪", R.drawable.biz_plugin_weather_yujiaxue);
        weatherhash.put("阵雪", R.drawable.biz_plugin_weather_zhenxue);
        weatherhash.put("阵雨", R.drawable.biz_plugin_weather_zhenyu);
        weatherhash.put("中雪", R.drawable.biz_plugin_weather_zhongxue);
        weatherhash.put("中雨", R.drawable.biz_plugin_weather_zhongyu);
    }

    private ImageView mCitySelect, mLocataBtn, mUpdateBtn;
    private TextView temperatureTv, climateTv, windTv, cityNameTv, currentTempTv,
            cityTv, timeTv, humidityTv, weekTv, pmValueTv, pmQualityTv;

    private ImageView weatherImg, pmImg;
    private String currentCityCode;
    private TodayWeather currentWeather;

    private IntentFilter intentFilter;

    private ViewPagerAdapter vpAdapter;
    private ViewPager vp;
    private List<View> views;
    private List<FutureWeather> futureDaysWeather;

    private ImageView[] dots;
    private int[] ids = {R.id.FUTURE_DAYS_WEATHER_iv1, R.id.FUTURE_DAYS_WEATHER_iv2};

    private List<City> mCityList;





    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);

        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        mLocataBtn = (ImageView) findViewById(R.id.title_location);
        mLocataBtn.setOnClickListener(this);

        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);

        initView();

        // 判断是不是首次进入
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        if (savedInstanceState != null) {
            currentWeather = (TodayWeather) savedInstanceState.getSerializable("currentWeather");
            currentCityCode = sharedPreferences.getString("current_city", "101010100");
            updateTodayWeather(currentWeather);
        } else {
            currentCityCode = sharedPreferences.getString("current_city", "101010100");
            Log.d("MyWeather", currentCityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NetworkState.NETWORK_NONE) {
                Log.d("MyWeather", "网络OK，这是onCreate");
                queryWeatherCode(currentCityCode);
            } else {
                Log.d("MyWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }

        // 启动自动刷新的service
        startService(new Intent(getBaseContext(), UpdateService.class));
        intentFilter = new IntentFilter();
        intentFilter.addAction("Update");
        registerReceiver(intentReceiver, intentFilter);

        // 启动百度地图定位
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(mLocationListener);
        initLocation();

        // 获取城市列表和代码
        mCityList = ((MyApplication) getApplication()).getmCityList();
    }

    //异步更新天气信息
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    TodayWeather todayWeather = (TodayWeather) msg.obj;

                    if (todayWeather.getWendu() == null) {
                        Toast.makeText(MainActivity.this, "该地区目前没有天气信息！", Toast.LENGTH_LONG).show();
                        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
                        currentCityCode = sharedPreferences.getString("current_city", "101010100");
                    } else {
                        currentWeather = todayWeather;
                        updateTodayWeather(todayWeather);
                        Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_LONG).show();
                        Log.d("MyWeather","这是Handler");

                        // 更新配置文件，以便下次读取
                        SharedPreferences.Editor editor = getSharedPreferences("config", MODE_PRIVATE).edit();
                        editor.putString("current_city", currentCityCode);
                        editor.commit();
                    }

                    mUpdateBtn.setAnimation(null);
                    mUpdateBtn.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    };
    //设置接受广播的Receiver，定时更新数据
    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetUtil.getNetworkState(getBaseContext()) != NetUtil.NetworkState.NETWORK_NONE) {
                Log.d("MyWeather", "网络OK，这是BroadcastReceiver");
                queryWeatherCode(currentCityCode);
            } else {
                Log.d("MyWeather", "网络挂了,这是BroadcastReceiver");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    };
    //百度地图定位服务
    public LocationClient mLocationClient = null;
    public BDLocationListener mLocationListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            String location = bdLocation.getDistrict();
            Log.d("百度地图", "定位到" + location);
            for (City city : mCityList) {
                if (location.contains(city.getCity())) {
                    Toast.makeText(MainActivity.this, "定位成功！", Toast.LENGTH_LONG).show();
                    currentCityCode = city.getNumber();
                    queryWeatherCode(currentCityCode);
                }
            }
            mLocationClient.stop();
        }
    };

    void initView() {
        cityNameTv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmValueTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        currentTempTv = (TextView) findViewById(R.id.current_temp);
        cityNameTv.setText("N/A");
        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        pmValueTv.setText("N/A");
        pmQualityTv.setText("N/A");
        weekTv.setText("N/A");
        temperatureTv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");
        currentTempTv.setText("N/A");

        // 初始化底部 ViewPager
        LayoutInflater inflater = LayoutInflater.from(this);
        views = new ArrayList<>();
        futureDaysWeather = new ArrayList<>();
        views.add(inflater.inflate(R.layout.future_weather, null));
        views.add(inflater.inflate(R.layout.future_weather, null));
        vpAdapter = new ViewPagerAdapter(views, this);
        vp = (ViewPager) findViewById(R.id.view_pager);
        vp.setAdapter(vpAdapter);
        vp.addOnPageChangeListener(this);

        for (int i = 0; i < views.size(); i++) {
            LinearLayout view = (LinearLayout) views.get(i);
            for (int k = 0; k < view.getChildCount(); k++) {
                View subview = view.getChildAt(k);
                for (int j = 0; j < 3; j++) {
                    ((TextView) subview.findViewById(R.id.tomorrow_date)).setText("N/A");
                    ((TextView) subview.findViewById(R.id.tomorrow_temp)).setText("N/A");
                    ((TextView) subview.findViewById(R.id.tomorrow_weather)).setText("N/A");
                    ((TextView) subview.findViewById(R.id.tomorrow_wind)).setText("N/A");
                }
            }
        }

        // 初始化底部小圆点
        dots = new ImageView[views.size()];
        for (int i = 0; i < views.size(); i++) {
            dots[i] = (ImageView) findViewById(ids[i]);
        }
    }

    void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);   // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");// 可选，默认gcj02，设置返回的定位结果坐标系
        int span=1000;
        option.setScanSpan(span);   // 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);   // 可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);   // 可选，默认false,设置是否使用gps
        option.setLocationNotify(true);   // 可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);   // 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);   // 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);   // 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);   // 可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);   // 可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            // 响应选择城市按钮点击
            case R.id.title_city_manager:
                Intent i = new Intent(this, SelectCity.class);
                i.putExtra("cityName", cityNameTv.getText());
                startActivityForResult(i, 1);
                break;

            // 响应定位按钮点击
            case R.id.title_location:
                mLocationClient.start();
                break;

            // 响应刷新按钮点击
            case R.id.title_update_btn:
                RotateAnimation rotate = new RotateAnimation(0.0f, 360.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setInterpolator(new LinearInterpolator());
                rotate.setRepeatCount(Animation.INFINITE);
                rotate.setDuration(400);

                mUpdateBtn.setEnabled(false);
                mUpdateBtn.startAnimation(rotate);

                if (NetUtil.getNetworkState(this) != NetUtil.NetworkState.NETWORK_NONE) {
                    Log.d("MyWeather", "网络OK");
                    queryWeatherCode(currentCityCode);
                } else {
                    Log.d("MyWeather", "网络挂了");
                    Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    private void queryWeatherCode(String cityCode) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("MyWeather", address);

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather;
                try {
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);

                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        response.append(str);
                    }
                    String responseStr = response.toString();
                    Log.d("MyWeather", responseStr);
                    todayWeather = parseXML(responseStr);
                    Log.d("MyWeather", todayWeather.toString());

                    Message msg = new Message();
                    msg.what = UPDATE_TODAY_WEATHER;
                    msg.obj = todayWeather;
                    mHandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
        }).start();
    }

    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = new TodayWeather();
        int fengxiangCount = 0;
        int fengliCount = 0;
        int dateCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int typeCount = 0;

        futureDaysWeather.clear();
        FutureWeather futureWeather = null;
        int startFurture = 0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));

            int eventType = xmlPullParser.getEventType();
            Log.d("MyWeather", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {

                    // 判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    // 判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("city")) {
                            xmlPullParser.next();
                            todayWeather.setCity(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("updatetime")) {
                            xmlPullParser.next();
                            todayWeather.setUpdatetime(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("shidu")) {
                            xmlPullParser.next();
                            todayWeather.setShidu(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("wendu")) {
                            xmlPullParser.next();
                            todayWeather.setWendu(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("pm25")) {
                            xmlPullParser.next();
                            todayWeather.setPm25(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("quality")) {
                            xmlPullParser.next();
                            todayWeather.setQuality(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setFengxiang(xmlPullParser.getText());
                            fengxiangCount++;

                        } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setFengli(xmlPullParser.getText());
                            fengliCount++;

                        } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setDate(xmlPullParser.getText());
                            dateCount++;

                            futureWeather = new FutureWeather();
                            startFurture = 1;
                            futureWeather.setDate(xmlPullParser.getText());
                        } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                            xmlPullParser.next();

                            // 分解出温度数字部分, "高温 20℃" -> "20℃"
                            todayWeather.setHigh(xmlPullParser.getText().split(" ")[1]);
                            futureWeather.setHigh(xmlPullParser.getText().split(" ")[1]);
                            highCount++;

                        } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setLow(xmlPullParser.getText().split(" ")[1]);
                            futureWeather.setLow(xmlPullParser.getText().split(" ")[1]);
                            lowCount++;

                        } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setType(xmlPullParser.getText());
                            futureWeather.setType(xmlPullParser.getText());
                            typeCount++;

                        } else if ((xmlPullParser.getName().equals("date") || xmlPullParser.getName().equals("date_1")) && startFurture == 0) {
                            futureWeather = new FutureWeather();
                            startFurture = 1;

                            xmlPullParser.next();
                            futureWeather.setDate(xmlPullParser.getText());
                        } else if ((xmlPullParser.getName().equals("high") || xmlPullParser.getName().equals("high_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setHigh(xmlPullParser.getText().split(" ")[1]);

                        } else if ((xmlPullParser.getName().equals("low") || xmlPullParser.getName().equals("low_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setLow(xmlPullParser.getText().split(" ")[1]);

                        } else if ((xmlPullParser.getName().equals("type") || xmlPullParser.getName().equals("type_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setType(xmlPullParser.getText());

                        } else if ((xmlPullParser.getName().equals("fengxiang") || xmlPullParser.getName().equals("fx_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setFengxiang(xmlPullParser.getText());

                        } else if ((xmlPullParser.getName().equals("fengli") || xmlPullParser.getName().equals("fl_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setFengli(xmlPullParser.getText());

                            startFurture = 0;
                            if (futureWeather.getDate() != null)
                                futureDaysWeather.add(futureWeather);
                        }
                        break;
                    // 判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                // 进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    void updateTodayWeather(TodayWeather todayWeather) {
        cityNameTv.setText(todayWeather.getCity() + "天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime() + "发布");
        humidityTv.setText("湿度：" + todayWeather.getShidu());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getLow() + "~" + todayWeather.getHigh());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力：" + todayWeather.getFengli());
        currentTempTv.setText(todayWeather.getWendu() + "℃");
        if (weatherhash.get(todayWeather.getType()) != null) {
            weatherImg.setImageResource(weatherhash.get(todayWeather.getType()));
        }

        if (todayWeather.getPm25() != null) {
            pmValueTv.setText(todayWeather.getPm25());
            pmQualityTv.setText(todayWeather.getQuality());

            int pm25 = Integer.parseInt(todayWeather.getPm25());
            if (pm25 <= 50) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
            } else if (pm25 <= 100) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
            } else if (pm25 <= 150) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
            } else if (pm25 <= 200) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
            } else if (pm25 <= 300) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
            } else {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
            }
        } else {
            pmValueTv.setText("N/A");
            pmQualityTv.setText("N/A");
        }

        // 顺便更新六日天气
        for (int i = 0; i < views.size(); i++) {
            LinearLayout view = (LinearLayout) views.get(i);
            for (int j = 0; j < view.getChildCount(); j++) {
                View subview = view.getChildAt(j);
                FutureWeather futureWeather = futureDaysWeather.get(i * view.getChildCount() + j);

                ((TextView) subview.findViewById(R.id.tomorrow_date)).setText(futureWeather.getDate());
                ((ImageView) subview.findViewById(R.id.weather_img)).setImageResource(weatherhash.get(todayWeather.getType()));
                ((TextView) subview.findViewById(R.id.tomorrow_temp)).setText(futureWeather.getLow() + "~" + futureWeather.getHigh());
                ((TextView) subview.findViewById(R.id.tomorrow_weather)).setText(futureWeather.getType());
                ((TextView) subview.findViewById(R.id.tomorrow_wind)).setText(futureWeather.getFengli());

            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            currentCityCode = data.getStringExtra("cityCode");
            Log.d("MyWeather", "选择的城市代码为" + currentCityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NetworkState.NETWORK_NONE) {
                Log.d("MyWeather", "网络OK");
                queryWeatherCode(currentCityCode);
            } else {
                Log.d("MyWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // 在 Activity 可能被摧毁之前保存目前状态
        outState.putSerializable("currentWeather", currentWeather);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(getBaseContext(), UpdateService.class));
        unregisterReceiver(intentReceiver);

        mLocationClient.unRegisterLocationListener(mLocationListener);
        mLocationClient.stop();

        super.onDestroy();
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        for (int i = 0; i< ids.length; i++) {
            if (i == position) {
                dots[i].setImageResource(R.drawable.page_indicator_focused);
            } else {
                dots[i].setImageResource(R.drawable.page_indicator_unfocused);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}