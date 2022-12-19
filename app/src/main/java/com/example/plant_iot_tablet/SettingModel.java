package com.example.plant_iot_tablet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class SettingModel extends Activity {
    // 입력.
    EditText modelEdit;
    ImageView editNull;

    // 버튼.
    Button cancelBTN, applyBTN;

    String model = "";
    GetModelCheck gModel;
    String getModelURL = "http://aj3dlab.dothome.co.kr/Plant_checkInform_Android.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_setting_model);

        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Intent getIntent = getIntent();
        model = getIntent.getStringExtra("model");

        // 입력.
        modelEdit = (EditText) findViewById(R.id.modelEdit);
        if(!model.equals("")) {
            modelEdit.setText(model);
        }
        editNull = (ImageView) findViewById(R.id.editNull);
        editNull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                modelEdit.setText(null);
            }
        });

        // 버튼.
        cancelBTN = (Button) findViewById(R.id.cancelBTN);
        cancelBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        applyBTN = (Button) findViewById(R.id.applyBTN);
        applyBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(modelEdit.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "모델명을 입력해주세요", Toast.LENGTH_SHORT).show();
                }
                else {
                    model = modelEdit.getText().toString();
                    gModel = new GetModelCheck();
                    gModel.execute(getModelURL);
                }
            }
        });
    }


    // 입력한 모델명 존재하는지 체크.
    class GetModelCheck extends AsyncTask<String, Integer, String> {
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
            String check = "";
            try {
                JSONObject jsonObject = new JSONObject(str);
                JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);

                    check = item.getString("check");
                }

                if(check.equals("OK")) {
                    Intent intent = new Intent();
                    intent.putExtra("model", model);
                    setResult(RESULT_OK, intent);
                    finish();
                    Toast.makeText(getApplicationContext(), model, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "입력하신 모델이 존재하지 않습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 바깥 영역 터치해도 안닫히게.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            return false;
        }
        return true;
    }


}
