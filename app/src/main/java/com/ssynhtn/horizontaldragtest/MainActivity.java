package com.ssynhtn.horizontaldragtest;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;

import com.ssynhtn.horizontaldraglayout.HorizontalDragRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private HorizontalDragRefreshLayout dragRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager viewPager = findViewById(R.id.view_pager);
        dragRefreshLayout = findViewById(R.id.drag);
        viewPager.setAdapter(new Adapter(getSupportFragmentManager()));

        dragRefreshLayout.setOnDragListener(new HorizontalDragRefreshLayout.OnDragListener() {
            @Override
            public void onLeftDragTriggered() {
                Toast.makeText(MainActivity.this, "onLeft", Toast.LENGTH_SHORT).show();
                vibrate();
            }

            @Override
            public void onLeftDragAnimationFinished() {

            }

            @Override
            public void onRightDragTriggered() {
                Toast.makeText(MainActivity.this, "onRight", Toast.LENGTH_SHORT).show();
                vibrate();
            }

            @Override
            public void onRightDragAnimationFinished() {

            }
        });
    }

    private void vibrate() {
        dragRefreshLayout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING |
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }

    class Adapter extends FragmentPagerAdapter {

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return TextFragment.newInstance("Hello " + i);
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
