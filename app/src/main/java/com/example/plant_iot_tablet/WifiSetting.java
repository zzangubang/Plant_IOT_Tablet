package com.example.plant_iot_tablet;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class WifiSetting extends AppCompatActivity {
    TextView bluetoothState;
    ImageView backHome;

    // 블루투스.
    TextView arduinoStateT;
    Button newBTN, regBTN;
    ListView newList, regList;
    BleViewAdapter newAdapter, regAdapter;
    ArrayList<String> newDevice, regDevice, checkDevice;
    ArrayList<BleViewItem> newItem, regItem;
    Set<BluetoothDevice> device;
    String TAG = "WifiSetting";
    ConnectedThread connectThread;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    public BluetoothSocket bluetoothSocket;
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static Handler handler;
    String name = "", pass = "";
    String currentBluetooth = "off", deviceState = "off";

    // 와이파이.
    Button wifiBTN;
    ListView wifiList;
    WifiManager wifiManager;
    WifiListAdapter wifiListAdapter;
    ArrayList<WifiListItem> wifiListItems;

    String getBleURL = "http://aj3dlab.dothome.co.kr/Plant_bleG_Android.php";
    String sendBleURL = "http://aj3dlab.dothome.co.kr/Plant_bleS_Android.php";
    GetBle gBle;
    SendBle sBle;

    String model = "";
    Toast toast;
    Timer timer;
    public static Context mContext;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setting);

        Intent getIntent = getIntent();
        model = getIntent.getStringExtra("model");
        mContext = this;

        // 로딩창.
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("잠시만 기다려주세요");

        bluetoothState = (TextView) findViewById(R.id.bluetoothState);
        backHome = (ImageView) findViewById(R.id.backHome);
        backHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bluetoothSocket == null) {
                }
                else {
                    if (bluetoothSocket.isConnected()) {
                        try {
                            connectThread.write("WIFI:FIN\r\n");
                            bluetoothSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                finish();
            }
        });

        // 와이파이.
        wifiList = (ListView) findViewById(R.id.wifiList);
        wifiListItems = new ArrayList<WifiListItem>();
        wifiListAdapter = new WifiListAdapter(this, wifiListItems);
        wifiList.setAdapter(wifiListAdapter);
        wifiList.setOnItemClickListener(new wifiListItemClickListener());
        wifiScan();
        wifiBTN = (Button) findViewById(R.id.wifiBTN);
        wifiBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiScan();
                wifiListItems.clear();
                wifiListAdapter.notifyDataSetChanged();
            }
        });

        // 블루투스.
        arduinoStateT = (TextView) findViewById(R.id.arduinoStateT);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                toastShow(msg.obj.toString());
            }
        };
        if(bluetoothAdapter == null) { // 기기가 블루투스 지원을 하지 않는 경우.
            toastShow("블루투스 지원을 하지 않는 기기입니다");
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            finish();
        }
        else { // 기기가 블루투스 지원을 하는 경우.
            if(!bluetoothAdapter.isEnabled()) { // 블루투스가 비활성인 경우.
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent);
            }
        }
        // 블루투스 연결 끊김 확인.
        // 핸드폰 자체 블루투스.
        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStateReceiver, stateFilter);
        // 페어링 된 블루투스.
        IntentFilter pairFilter = new IntentFilter();
        pairFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(pairingStateReceiver, pairFilter);

        // 새로운 기기.
        newDevice = new ArrayList<>();
        checkDevice = new ArrayList<>();
        newItem = new ArrayList<BleViewItem>();
        newAdapter = new BleViewAdapter(this, newItem);
        newBTN = (Button) findViewById(R.id.newBTN);
        newBTN.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                if(bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                else {
                    if(bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.startDiscovery();
                        newItem.clear();
                        if(newDevice!=null && newDevice.isEmpty()) {
                            newDevice.clear();
                            checkDevice.clear();
                        }
                        try{
                            unregisterReceiver(receiver);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(receiver, filter);
                    }
                    else {
                        toastShow("블루투스가 켜져 있지 않습니다");
                    }
                }
            }
        });
        newList = (ListView) findViewById(R.id.newList);
        newList.setAdapter(newAdapter);
        newList.setOnItemClickListener(new newItemClickListener());

        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        else {
            if(bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.startDiscovery();
                newItem.clear();
                if(newDevice!=null && newDevice.isEmpty()) {
                    newDevice.clear();
                    checkDevice.clear();
                }
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(receiver, filter);
            }
            else {
                toastShow("블루투스가 켜져 있지 않습니다");
            }
        }

        // 등록된 기기.
        regDevice = new ArrayList<>();
        regItem = new ArrayList<BleViewItem>();
        regAdapter = new BleViewAdapter(this, regItem);
        regBTN = (Button) findViewById(R.id.regBTN);
        regBTN.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                if(regDevice!=null && regDevice.isEmpty()) {
                    regItem.clear();
                    regDevice.clear();
                }
                device = bluetoothAdapter.getBondedDevices();
                if(device.size() > 0) {
                    for(BluetoothDevice device : device) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress();
                        regItem.add(new BleViewItem(deviceName, deviceHardwareAddress));
                        regDevice.add(deviceHardwareAddress);
                    }
                }

            }
        });
        regList = (ListView) findViewById(R.id.regList);
        regList.setAdapter(regAdapter);
        regList.setOnItemClickListener(new regItemClickListener());

        if(regDevice!=null && regDevice.isEmpty()) {
            regDevice.clear();
        }
        device = bluetoothAdapter.getBondedDevices();
        if(device.size() > 0) {
            for(BluetoothDevice device : device) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                regItem.add(new BleViewItem(deviceName, deviceHardwareAddress));
                regDevice.add(deviceHardwareAddress);
            }
        }


        // 처음 실행.
        gBle = new GetBle();
        gBle.execute(getBleURL);
        dialog.show();
        new Handler().postDelayed(new Runnable()
        {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run()
            {
                Handler_ReconnectBluetooth();
            }
        }, 200); // 0.2초 정도 딜레이를 준 후 시작

    }
    public void timeTask() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(currentBluetooth.equals("on") && deviceState.equals("off")) {
                    connectThread.write("WIFI:SET\r\n");
                }
            }
        };
        timer = new Timer();
        timer.schedule(task, 1000, 1000); //1초마다 실행.
    }

    // 블루투스.
    public void ReConnectBluetooth() {
        if (name.equals("null") || pass.equals("null")) {
        } else {
            if (BluetoothAdapter.checkBluetoothAddress(pass)) {
                boolean flag = true;
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(pass);
                bluetoothSocket = null;
                try {
                    bluetoothSocket = createBluetoothSocket(bluetoothDevice);
                    bluetoothSocket.connect();
                } catch (IOException e) {
                    flag = false;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            currentBluetooth = "off";
                            bluetoothState.setText("기기에 연결되지 않음");
                            bluetoothState.setTextColor(Color.parseColor("#AF0000"));
                            e.printStackTrace();
                            arduinoStateT.setText("");
                        }
                    }, 0);
                }
                if (flag) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            currentBluetooth = "on";
                            bluetoothState.setText(name);
                            bluetoothState.setTextColor(Color.parseColor("#0046AF"));

                            connectThread = new ConnectedThread(bluetoothSocket);
                            connectThread.start();
                            timeTask();
                            arduinoStateT.setText("WAIT");
                        }
                    }, 0);
                }
            }
        }
        dialog.dismiss();
    }
    void Handler_ReconnectBluetooth() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ReConnectBluetooth();
            }
        }).start();
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            try {
                BluetoothSocket createSocket = (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
                return createSocket;
            } catch (InvocationTargetException e) {
                e.getTargetException().printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    // 블루투스 상태변화. (핸드폰 자체 블루투스)
    BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (state == BluetoothAdapter.STATE_ON) { // 블루투스 활성화.
            } else if (state == BluetoothAdapter.STATE_TURNING_ON) { // 블루투스 활성화 중.
            } else if (state == BluetoothAdapter.STATE_OFF) { // 블루투스 비활성화.
                currentBluetooth = "off";
                bluetoothState.setText("기기에 연결되지 않음");
                bluetoothState.setTextColor(Color.parseColor("#AF0000"));
                arduinoStateT.setText("");

                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (state == BluetoothAdapter.STATE_TURNING_OFF) { // 블루투스 비활성화 중.

            }
        }
    };



    // 프로토콜 송신.
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        OutputStream tmpOut = null;


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            int readBufferPosition = 0;
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    buffer = null;
                    if (bytes != 0) {
                        buffer = new byte[bytes];
                        mmInStream.read(buffer);
                        for (int i = 0; i < bytes; i++) {
                            byte tempByte = buffer[i];
                            if (33 > tempByte || tempByte > 126) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(buffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String text = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;
                                postToastMessage(text);
                                break;
                            } else {
                                if (tempByte == 0) {
                                    break;
                                }
                                buffer[readBufferPosition] = tempByte;
                                //testByte = tempByte;
                                readBufferPosition += 1;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            toastShow("ArrayIndexOutOfBoundsException");
                        }
                    }, 0);
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    // 프로토콜 수신.
    public void postToastMessage(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                String[] value = message.split(":");

                // OK: 받을 준비
                if (value[0].equals("WIFI")) {
                    if(value[1].trim().equals("OK")) {
                        timer.cancel();
                        deviceState = "on";
                        arduinoStateT.setText("READY");
                    }
                    if(value[1].trim().equals("CHANGE")) {
                        toastShow("설정 완료 되었습니다.");
                    }
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 와이파이 비밀번호.
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                String wifiN = data.getStringExtra("wifiName");
                String wifiP = data.getStringExtra("wifiPass");
                String sendCommand = wifiN + ":" + wifiP;
                connectThread.write("WIFI:INFO:" + sendCommand + "\r\n");
            }
        }
    }
    BroadcastReceiver pairingStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                currentBluetooth = "off";
                bluetoothState.setText("기기에 연결되지 않음");
                bluetoothState.setTextColor(Color.parseColor("#AF0000"));
                arduinoStateT.setText("");
            }
        }
    };
    // ACTION_FOUND를 위한 BroadcastReceiver.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int cnt = 0;
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                // deviceName이 NULL값이면 그냥 넘기기.
                if(deviceName == null || deviceName.equals("")) {
                }
                else { // 그렇지 않다면 중복 체크.
                    for(String check : checkDevice) {
                        if(check.equals(deviceName))
                            cnt += 1;
                    }
                    if(cnt > 0) { // 중복값 존재.
                    }
                    else { // 중복 존재X.
                        newItem.add(new BleViewItem(deviceName, deviceHardwareAddress));
                        checkDevice.add(deviceName);
                        newDevice.add(deviceHardwareAddress);
                        newAdapter.notifyDataSetChanged();
                    }
                }


            }
        }
    };

    // ListView 아이템 클릭 시, 연결시켜주도록 MainActivity에 정보 넘기도록. -- 등록된 기기 中
    public class regItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            dialog.show();
            if(bluetoothSocket == null) {
            }
            else {
                if (bluetoothSocket.isConnected()) {
                    try {
                        connectThread.write("WIFI:FIN\r\n");
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            toastShow(regItem.get(position).getName() + " 연결 시도");

            final String nameB = regItem.get(position).getName(); // get name
            final String addressB = regItem.get(position).getAddress(); // get address
            boolean flag = true;

            name = nameB;
            pass = addressB;
            sBle = new SendBle();
            sBle.execute(sendBleURL);
            Handler_ReconnectBluetooth();
        }
    }
    // -- 새로운 기기 中
    public class newItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            dialog.show();
            if(bluetoothSocket == null) {
            }
            else {
                if (bluetoothSocket.isConnected()) {
                    try {
                        connectThread.write("WIFI:FIN\r\n");
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            toastShow(newItem.get(position).getName()+"연결 시도");

            final String nameB = newItem.get(position).getName(); // get name
            final String addressB = newItem.get(position).getAddress(); // get address
            boolean flag = true;

            name = nameB;
            pass = addressB;
            sBle = new SendBle();
            sBle.execute(sendBleURL);
            Handler_ReconnectBluetooth();
        }
    }

    // 와이파이.
    // 와이파이 스캔.
    public void wifiScan() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    scanFailure();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);

        boolean success = wifiManager.startScan();
        if (!success) {
            scanFailure();
        }
    }
    // 스캔 성공 시.
    private void scanSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult r : results) {
            String wifiName = r.SSID;
            if (wifiName.equals("")) {

            } else {
                wifiListItems.add(new WifiListItem(wifiName));
                wifiListAdapter.notifyDataSetChanged();
            }
        }
    }

    // 스캔 실패 시.
    private void scanFailure() {
        toastShow("와이파이 스캔에 실패하였습니다");
    }

    // 와이파이 리스트 중 Item 클릭 시.
    public class wifiListItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final String name = wifiListItems.get(position).getName(); // get name
            boolean flag = true;

            if(currentBluetooth.equals("off")) {
                toastShow("기기와의 블루투스 연결을 해주세요");
            }
            else {
                if(deviceState.equals("off")) {
                    toastShow("기기에서 아직 요청을 받지 못하였습니다. 잠시 후 다시 시도해주세요.");
                }
                else {
                    Intent intent = new Intent(getApplicationContext(), WifiPass.class);
                    intent.putExtra("wifiName", name);
                    startActivityForResult(intent, 2);
                }
            }
        }
    }

    // 블루투스 정보 가져오기.
    class GetBle extends AsyncTask<String, Integer, String> {
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
            try {
                JSONObject jsonObject = new JSONObject(str);
                JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);

                    name = item.getString("bleN");
                    pass = item.getString("bleP");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    // 블루투스 정보 보내기.
    class SendBle extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder jsonHtml = new StringBuilder();

            String serverURL = (String) params[0];
            String postParameters = "model=" + model + "&name=" + name + "&pass=" + pass;

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

    // 뒤로가기.
    @Override
    public void onBackPressed() {
        if(bluetoothSocket == null) {
        }
        else {
            if (bluetoothSocket.isConnected()) {
                try {
                    connectThread.write("WIFI:FIN\r\n");
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        finish();
    }
}