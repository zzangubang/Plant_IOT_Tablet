package com.example.plant_iot_tablet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;

public class Data extends AppCompatActivity {
    // 상단 바.
    ImageView backHome, pop;

    // Fragment.
    FragmentManager fragmentManager;
    FragmentTransaction transaction;
    Fragment TempData, HumiData, IlluData;

    String model = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        Intent getIntent = getIntent();
        model = getIntent.getStringExtra("model");

        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        TempData = new TempData();
        HumiData = new HumiData();
        IlluData = new IlluData();

        // 상단 바.
        backHome = findViewById(R.id.backHome);
        backHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        pop = findViewById(R.id.pop);
        pop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu dataPop = new PopupMenu(Data.this, pop);
                getMenuInflater().inflate(R.menu.menu_data, dataPop.getMenu());
                dataPop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        transaction = fragmentManager.beginTransaction();
                        switch (menuItem.getItemId()) {
                            case R.id.temp:
                                transaction.replace(R.id.frameLayout, TempData).commitAllowingStateLoss();
                                break;
                            case R.id.humi:
                                transaction.replace(R.id.frameLayout, HumiData).commitAllowingStateLoss();
                                break;
                            case R.id.illu:
                                transaction.replace(R.id.frameLayout, IlluData).commitAllowingStateLoss();
                                break;
                        }
                        return false;
                    }
                });
                dataPop.show();
            }
        });

        transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, TempData).commitAllowingStateLoss();
    }
}