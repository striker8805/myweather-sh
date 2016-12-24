package cn.edu.pku.ss.shenhao.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;



import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.ss.shenhao.adapter.ViewPagerAdapter;

/**
 * Created by striker on 2016/12/24.
 */
public class Guide extends Activity implements ViewPager.OnPageChangeListener {
    private ViewPagerAdapter vpAdapter;
    private ViewPager vp;
    private List<View> views;

    private ImageView[] dots;
    private int[] ids = {R.id.guide_iv1, R.id.guide_iv2, R.id.guide_iv3};

    private Button btn;

    boolean isFirstIn = false;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("first_pref",MODE_PRIVATE);

        isFirstIn = preferences.getBoolean("isFirstIn", true);
//        isFirstIn = true;
        if(isFirstIn==true) {
            setContentView(R.layout.guide);
            initView();
            initDots();
//            SharedPreferences preferences = getSharedPreferences(
//                    "first_pref", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstIn", false);
            editor.commit();


            btn = (Button) views.get(2).findViewById(R.id.guide_btn);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Guide.this, MainActivity.class);
                    startActivity(i);
                    finish();
                }
            });
        }else{
            Intent i = new Intent(Guide.this, MainActivity.class);
            startActivity(i);
            finish();
        }
    }

    void initDots() {
        dots = new ImageView[views.size()];
        for (int i = 0; i < views.size(); i++) {
            dots[i] = (ImageView) findViewById(ids[i]);
        }
    }

    private void initView() {
        LayoutInflater inflatr = LayoutInflater.from(this);
        views = new ArrayList<>();
        views.add(inflatr.inflate(R.layout.page1, null));
        views.add(inflatr.inflate(R.layout.page2, null));
        views.add(inflatr.inflate(R.layout.page3, null));
        vpAdapter = new ViewPagerAdapter(views, this);
        vp = (ViewPager) findViewById(R.id.guide_viewpager);
        vp.setAdapter(vpAdapter);
        vp.addOnPageChangeListener(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        for (int a = 0; a < ids.length; a++) {
            if (a == position) {
                dots[a].setImageResource(R.drawable.page_indicator_focused);
            } else {
                dots[a].setImageResource(R.drawable.page_indicator_unfocused);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
