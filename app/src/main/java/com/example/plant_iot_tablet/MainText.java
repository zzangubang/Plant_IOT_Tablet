package com.example.plant_iot_tablet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainText#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainText extends Fragment {
    TextView tempValue, humiValue, illuValue, levelValue;
    TextView errTempT, errHumiT;

    String getValueURL = "http://aj3dlab.dothome.co.kr/Plant_value_Android.php";
    GetValue gValue;

    String model = "";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Timer timer;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MainText() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainText.
     */
    // TODO: Rename and change types and number of parameters
    public static MainText newInstance(String param1, String param2) {
        MainText fragment = new MainText();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main_text, container, false);

        sharedPreferences = this.getActivity().getSharedPreferences("PlantInform", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        model = sharedPreferences.getString("name", "");

        tempValue = (TextView) v.findViewById(R.id.tempValue);
        humiValue = (TextView) v.findViewById(R.id.humiValue);
        illuValue = (TextView) v.findViewById(R.id.illuValue);
        levelValue = (TextView) v.findViewById(R.id.levelValue);

        errTempT = (TextView) v.findViewById(R.id.errTemp);
        errHumiT = (TextView) v.findViewById(R.id.errHumi);

        if (!model.equals("")) {
            gValue = new GetValue();
            gValue.execute(getValueURL);
        }
        timeTask();

        return v;
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
        timer.schedule(task, 10000, 10000); //10초마다 실행
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
            String temp = "", humi = "", illu = "", level= "";
            double tempF = 0, humiF = 0;
            String errTemp = "", errHumi = "";
            try {
                JSONObject jsonObject = new JSONObject(str);
                JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);

                    temp = item.getString("temp");
                    humi = item.getString("humi");
                    illu = item.getString("illu");
                    level = item.getString("level");
                    errTemp = item.getString("errTemp");
                    errHumi = item.getString("errHumi");
                }
                tempF = Float.valueOf(temp);
                humiF = Float.valueOf(humi);

                errTempT.setText(errTemp);
                errHumiT.setText(errHumi);

                tempValue.setText(String.format("%.2f", tempF));
                humiValue.setText(String.format("%.2f", humiF));
                illuValue.setText(illu);
                if(level.equals("FULL")) {
                    levelValue.setText("적절");
                }
                else if(level.equals("EMPTY")) {
                    levelValue.setText("부족");
                }
                else{
                    levelValue.setText("에러");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}