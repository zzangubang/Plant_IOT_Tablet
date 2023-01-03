package com.example.plant_iot_tablet;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // 메뉴.
    RelativeLayout menu_data, menu_timer, menu_wifi, menu_setting;

    // 업데이트 날짜 및 자동/수동 모드.
    TextView lastUpdateT;
    ToggleButton modeBTN;
    String mode = "";

    // 동작 제어.
    RelativeLayout fanBTN, fanEBTN, ledLBTN, ledRBTN, waterBTN, pumpBTN;
    ImageView fanI, fanEI, ledLI, ledRI, waterI, pumpI;
    TextView fanStepT;
    TextView fanIn, fanOut, ledLT, ledRT;
    String fan = "", fanE = "", ledL = "", ledR = "", water = "", pump = "";
    String getValueURL = "http://aj3dlab.dothome.co.kr/Plant_value_Android.php";
    String sendCommandURL = "http://aj3dlab.dothome.co.kr/Plant_command_Android.php";
    GetValue gValue;
    SendActive sActive;

    PagerAdapter adapter;
    ViewPager main_viewpager;
    TabLayout main_tabLayout;

    String model = "";

    Context mContext;
    Toast toast;
    Timer timer;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        Intent getIntent = getIntent();
        model = getIntent.getStringExtra("model");

        // 블루투스 및 와이파이 권한 허용.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_NETWORK_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    1);
        }

        main_viewpager = (ViewPager) findViewById(R.id.main_pager);
        main_tabLayout = (TabLayout) findViewById(R.id.main_tabLayout);

        adapter = new MainPagerAdapter(getSupportFragmentManager());
        main_viewpager.setAdapter(adapter);
        main_tabLayout.setupWithViewPager(main_viewpager);

        // 메뉴.
        menu_data = (RelativeLayout) findViewById(R.id.menu_data);
        menu_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                }
                else {
                    Intent intent = new Intent(getApplicationContext(), Data.class);
                    intent.putExtra("model", model);
                    startActivity(intent);
                }
            }
        });
        menu_timer = (RelativeLayout) findViewById(R.id.menu_timer);
        menu_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                }
                else {
                    Intent intent = new Intent(getApplicationContext(), UserSetting.class);
                    intent.putExtra("model", model);
                    startActivityForResult(intent, 2);
                }
            }
        });
        menu_wifi = (RelativeLayout) findViewById(R.id.menu_wifi);
        menu_wifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                }
                else {
                    Intent intent = new Intent(getApplicationContext(), WifiSetting.class);
                    intent.putExtra("model", model);
                    startActivity(intent);
                }
            }
        });
        menu_setting = (RelativeLayout) findViewById(R.id.menu_setting);
        menu_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        // 마지막 업데이트 날짜 및 자동/수동 모드.
        lastUpdateT = (TextView) findViewById(R.id.lastUpdateT);
        modeBTN = (ToggleButton) findViewById(R.id.modeBTN);
        modeBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                    modeBTN.setChecked(false);
                } else {
                    if (mode.equals("auto")) {
                        mode = "manual";
                        modeBTN.setChecked(false);

                        fanBTN.setEnabled(true);
                        fanEBTN.setEnabled(true);
                        ledLBTN.setEnabled(true);
                        ledRBTN.setEnabled(true);
                        waterBTN.setEnabled(true);
                        pumpBTN.setEnabled(true);
                    } else if (mode.equals("manual")) {
                        mode = "auto";
                        modeBTN.setChecked(true);

                        fanBTN.setEnabled(false);
                        fanEBTN.setEnabled(false);
                        ledLBTN.setEnabled(false);
                        ledRBTN.setEnabled(false);
                        waterBTN.setEnabled(false);
                        pumpBTN.setEnabled(false);
                    } else {
                        if (modeBTN.isChecked()) {
                            mode = "auto";
                            modeBTN.setChecked(true);

                            fanBTN.setEnabled(false);
                            fanEBTN.setEnabled(false);
                            ledLBTN.setEnabled(false);
                            ledRBTN.setEnabled(false);
                            waterBTN.setEnabled(false);
                            pumpBTN.setEnabled(false);
                        } else {
                            mode = "manual";
                            modeBTN.setChecked(false);

                            fanBTN.setEnabled(true);
                            fanEBTN.setEnabled(true);
                            ledLBTN.setEnabled(true);
                            ledRBTN.setEnabled(true);
                            waterBTN.setEnabled(true);
                            pumpBTN.setEnabled(true);
                        }
                    }
                    SendCommand();
                }
            }
        });

        // 동작 제어.
        fanBTN = (RelativeLayout) findViewById(R.id.fanBTN);
        fanI = (ImageView) findViewById(R.id.fanI);
        fanStepT = (TextView) findViewById(R.id.fanStepT);
        fanIn = (TextView) findViewById(R.id.fanIn);
        fanBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                } else {
                    if(fan.equals("off")) {
                        PopupMenu fanPop = new PopupMenu(MainActivity.this, fanBTN);
                        getMenuInflater().inflate(R.menu.menu_fan, fanPop.getMenu());
                        fanPop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                fanIn.setTextColor(Color.parseColor("#ffffff"));
                                fanBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                                fanI.setImageResource(R.drawable.fan_on);
                                switch (menuItem.getItemId()) {
                                    case R.id.fan1:
                                        fan = "on1";
                                        fanStepT.setText("①");
                                        break;
                                    case R.id.fan2:
                                        fan = "on2";
                                        fanStepT.setText("②");
                                        break;
                                    case R.id.fan3:
                                        fan = "on3";
                                        fanStepT.setText("③");
                                        break;
                                }
                                SendCommand();
                                return false;
                            }
                        });
                        fanPop.show();
                    }
                    else {
                        fan = "off";
                        fanIn.setTextColor(Color.parseColor("#9E9E9E"));
                        fanBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                        fanI.setImageResource(R.drawable.fan_off);
                        fanStepT.setText("");
                    }
                    SendCommand();
                }
            }
        });
        fanBTN.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!model.equals("")) {
                    if (fan.equals("on1") || fan.equals("on2") || fan.equals("on3")) {
                        PopupMenu fanPop = new PopupMenu(MainActivity.this, fanBTN);
                        getMenuInflater().inflate(R.menu.menu_fan, fanPop.getMenu());
                        fanPop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                switch (menuItem.getItemId()) {
                                    case R.id.fan1:
                                        fan = "on1";
                                        fanStepT.setText("①");
                                        break;
                                    case R.id.fan2:
                                        fan = "on2";
                                        fanStepT.setText("②");
                                        break;
                                    case R.id.fan3:
                                        fan = "on3";
                                        fanStepT.setText("③");
                                        break;
                                }
                                SendCommand();
                                return false;
                            }
                        });
                        fanPop.show();
                    }
                }
                return true;
            }
        });
        fanEBTN = (RelativeLayout) findViewById(R.id.fanEBTN);
        fanEI = (ImageView) findViewById(R.id.fanEI);
        fanOut = (TextView) findViewById(R.id.fanOut);
        fanEBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                } else {
                    if (fanE.equals("off")) {
                        fanE = "on";
                        fanOut.setTextColor(Color.parseColor("#ffffff"));
                        fanEBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                        fanEI.setImageResource(R.drawable.fan_on);
                    } else {
                        fanE = "off";
                        fanOut.setTextColor(Color.parseColor("#9E9E9E"));
                        fanEBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                        fanEI.setImageResource(R.drawable.fan_off);
                    }
                    SendCommand();
                }
            }
        });
        ledLBTN = (RelativeLayout) findViewById(R.id.ledLBTN);
        ledLI = (ImageView) findViewById(R.id.ledLI);
        ledLT = (TextView) findViewById(R.id.ledLT);
        ledLBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                } else {
                    if (ledL.equals("off")) {
                        ledL = "on";
                        ledLT.setTextColor(Color.parseColor("#ffffff"));
                        ledLBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                        ledLI.setImageResource(R.drawable.led_on);
                    } else {
                        ledL = "off";
                        ledLT.setTextColor(Color.parseColor("#9E9E9E"));
                        ledLBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                        ledLI.setImageResource(R.drawable.led_off);
                    }
                    SendCommand();
                }
            }
        });
        ledRBTN = (RelativeLayout) findViewById(R.id.ledRBTN);
        ledRI = (ImageView) findViewById(R.id.ledRI);
        ledRT = (TextView) findViewById(R.id.ledRT);
        ledRBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                } else {
                    if (ledR.equals("off")) {
                        ledR = "on";
                        ledRT.setTextColor(Color.parseColor("#ffffff"));
                        ledRBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                        ledRI.setImageResource(R.drawable.led_on);
                    } else {
                        ledR = "off";
                        ledRT.setTextColor(Color.parseColor("#9E9E9E"));
                        ledRBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                        ledRI.setImageResource(R.drawable.led_off);
                    }
                    SendCommand();
                }
            }
        });
        waterBTN = (RelativeLayout) findViewById(R.id.waterBTN);
        waterI = (ImageView) findViewById(R.id.waterI);
        waterBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                } else {
                    if (water.equals("off")) {
                        water = "on";
                        waterBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                        waterI.setImageResource(R.drawable.water_on);
                    } else {
                        water = "off";
                        waterBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                        waterI.setImageResource(R.drawable.water_off);
                    }
                    SendCommand();
                }
            }
        });
        pumpBTN = (RelativeLayout) findViewById(R.id.pumpBTN);
        pumpI = (ImageView) findViewById(R.id.pumpI);
        pumpBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.equals("")) {
                    toastShow("설정된 모델이 없습니다. 환경 아이콘을 눌러 모델을 설정해주세요.");
                } else {
                    if (pump.equals("off")) {
                        pump = "on";
                        pumpBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                        pumpI.setImageResource(R.drawable.pump_on);
                    } else {
                        pump = "off";
                        pumpBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                        pumpI.setImageResource(R.drawable.pump_off);
                    }
                    SendCommand();
                }
            }
        });

        // 현재 상태 읽어오기. (기기 상태 업데이트)
        if (!model.equals("")) {
            gValue = new GetValue();
            gValue.execute(getValueURL);
        }
        timeTask();
    }

    public void timeTask() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!model.equals("")) {
                    gValue = new GetValue();
                    gValue.execute(getValueURL);
                }
            }
        };
        timer = new Timer();
        timer.schedule(task, 25000, 25000); //30초마다 실행
    }

    // 메뉴에서 선택 후 돌아올 때.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            if(resultCode == RESULT_OK) {
                SendCommand();
            }
        }
    }

    // 현재 상태 읽어오기.
    class GetValue extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder jsonHtml = new StringBuilder();

            String serverURL = (String) params[0];
            String postParameters = "model=" + model;

            try {
                URL phpUrl = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) phpUrl.openConnection();

                if (conn != null) {
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("POST");
                    conn.connect();

                    OutputStream outputStream = conn.getOutputStream();
                    outputStream.write(postParameters.getBytes("UTF-8"));
                    outputStream.flush();
                    outputStream.close();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                        while (true) {
                            String line = br.readLine();
                            if (line == null)
                                break;
                            jsonHtml.append(line + "\n");
                        }
                        br.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return jsonHtml.toString();
        }

        protected void onPostExecute(String str) {
            String TAG_JSON = "aj3dlab";
            String date = "", time = "";
            String fanT = "", fanET = "", ledLTT = "", ledRTT = "", waterT = "", pumpT = "", modeTT = "";
            try {
                JSONObject jsonObject = new JSONObject(str);
                JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);

                    date = item.getString("date");
                    time = item.getString("time");

                    fanT = item.getString("fan");
                    fanET = item.getString("fanE");
                    ledLTT = item.getString("ledL");
                    ledRTT = item.getString("ledR");
                    waterT = item.getString("water");
                    pumpT = item.getString("pump");
                    modeTT = item.getString("mode").trim();
                }

                lastUpdateT.setText(date + " " + time);
                // 내부팬.
                if (fanT.equals("FANOFF")) {
                    fan = "off";
                    fanBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                    fanIn.setTextColor(Color.parseColor("#9E9E9E"));
                    fanI.setImageResource(R.drawable.fan_off);
                    fanStepT.setText("");
                } else {
                    fanBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                    fanIn.setTextColor(Color.parseColor("#ffffff"));
                    fanI.setImageResource(R.drawable.fan_on);
                    switch (fanT) {
                        case "FANON1":
                            fan = "on1";
                            fanStepT.setText("①");
                            break;
                        case "FANON2":
                            fan = "on2";
                            fanStepT.setText("②");
                            break;
                        case "FANON3":
                            fan = "on3";
                            fanStepT.setText("③");
                            break;
                        default:
                            fanStepT.setText("Err");
                    }
                }

                // 외부팬.
                if (fanET.equals("EXTFOFF")) {
                    fanE = "off";
                    fanEBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                    fanOut.setTextColor(Color.parseColor("#9E9E9E"));
                    fanEI.setImageResource(R.drawable.fan_off);
                } else {
                    fanE = "on";
                    fanEBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                    fanOut.setTextColor(Color.parseColor("#ffffff"));
                    fanEI.setImageResource(R.drawable.fan_on);
                }

                // 전등(좌).
                if (ledLTT.equals("LLAMPOFF")) {
                    ledL = "off";
                    ledLBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                    ledLT.setTextColor(Color.parseColor("#9E9E9E"));
                    ledLI.setImageResource(R.drawable.led_off);
                } else {
                    ledL = "on";
                    ledLBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                    ledLT.setTextColor(Color.parseColor("#ffffff"));
                    ledLI.setImageResource(R.drawable.led_on);
                }

                // 전등(우).
                if (ledRTT.equals("RLAMPOFF")) {
                    ledR = "off";
                    ledRBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                    ledRT.setTextColor(Color.parseColor("#9E9E9E"));
                    ledRI.setImageResource(R.drawable.led_off);
                } else {
                    ledR = "on";
                    ledRBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                    ledRT.setTextColor(Color.parseColor("#ffffff"));
                    ledRI.setImageResource(R.drawable.led_on);
                }

                // 연무기.
                if (waterT.equals("WATEROFF")) {
                    water = "off";
                    waterBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                    waterI.setImageResource(R.drawable.water_off);
                } else {
                    water = "on";
                    waterBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                    waterI.setImageResource(R.drawable.water_on);
                }

                // 펌프.
                if (pumpT.equals("PUMPOFF")) {
                    pump = "off";
                    pumpBTN.setBackgroundResource(R.drawable.work_off_layout_selector);
                    pumpI.setImageResource(R.drawable.pump_off);
                } else {
                    pump = "on";
                    pumpBTN.setBackgroundResource(R.drawable.work_on_layout_selector);
                    pumpI.setImageResource(R.drawable.pump_on);
                }

                // 모드.
                if (modeTT.equals("AUTO")) {
                    mode = "auto";
                    modeBTN.setChecked(true);

                    fanBTN.setEnabled(false);
                    fanEBTN.setEnabled(false);
                    ledLBTN.setEnabled(false);
                    ledRBTN.setEnabled(false);
                    waterBTN.setEnabled(false);
                    pumpBTN.setEnabled(false);

                } else if (modeTT.equals("MANUAL")) {
                    mode = "manual";
                    modeBTN.setChecked(false);

                    fanBTN.setEnabled(true);
                    fanEBTN.setEnabled(true);
                    ledLBTN.setEnabled(true);
                    ledRBTN.setEnabled(true);
                    waterBTN.setEnabled(true);
                    pumpBTN.setEnabled(true);
                } else {
                    mode = "error";

                    fanBTN.setEnabled(false);
                    fanEBTN.setEnabled(false);
                    ledLBTN.setEnabled(false);
                    ledRBTN.setEnabled(false);
                    waterBTN.setEnabled(false);
                    pumpBTN.setEnabled(false);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void SendCommand() {
        String command = fan + ":" + fanE + ":" + ledL + ":" + ledR + ":" + water + ":" + pump + ":" + mode;
        sActive = new SendActive();
        sActive.execute(sendCommandURL, command);
        timer.cancel();
        timeTask();
    }

    // 동작 제어.
    class SendActive extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder jsonHtml = new StringBuilder();

            String serverURL = (String) params[0];
            String postParameters = "model=" + model + "&command=" + (String) params[1];

            try {
                URL phpUrl = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) phpUrl.openConnection();

                if (conn != null) {
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("POST");
                    conn.connect();

                    OutputStream outputStream = conn.getOutputStream();
                    outputStream.write(postParameters.getBytes("UTF-8"));
                    outputStream.flush();
                    outputStream.close();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                        while (true) {
                            String line = br.readLine();
                            if (line == null)
                                break;
                            jsonHtml.append(line + "\n");
                        }
                        br.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return jsonHtml.toString();
        }

        protected void onPostExecute(String str) {
            String TAG_JSON = "aj3dlab";
            try {
                JSONObject jsonObject = new JSONObject(str);
                JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void toastShow(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }

    public void reLoad() {
        finish();//인텐트 종료
        overridePendingTransition(0, 0);//인텐트 효과 없애기
        Intent intent = getIntent(); //인텐트
        startActivity(intent); //액티비티 열기
        overridePendingTransition(0, 0);//인텐트 효과 없애기
    }

    // 뒤로가기.
    @Override
    public void onBackPressed() {
        finish();
    }


}