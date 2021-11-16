package com.example.tb_data_query_demo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextClock;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyIoTTest";

    private static final String BASE_URL = "https://demo.thingsboard.io";
    private static final String USERNAME = "<username>";
    private static final String PASSWORD = "<password>";
    public String DEVICE_ID = "";
    public String SELECT_DEVICE_NAME = "<devicename>";
    private static final MediaType TYPE_NULL = MediaType.parse("");
    private TextView tvDeviceName, tvTemperature, tvHumidity;
    public String JWT_TOKEN = "", temperature = "", humidity = "";
    private TextClock tClock;
    private Handler UIHandler = null;
    private int mInterval = 5000;
    private Handler mHandler;

    public class Timeseries {
        private List<Keys> temperature;
        private List<Keys> humidity;

        public class Keys {
            private String ts;
            private String value;
        }
    }

    public class JWTDecoded {
        //private String sub;
        private String userId;
        private String tenantId;
        private String customerId;
    }

    public class deviceIDs {
        @SerializedName("data")
        private ArrayList<datas> data;
        private int totalElements;

        public class datas {
            private ids id;
            //private ids tenantId;
            //private ids ustomerId;
            private String name;

            public class ids {
                private String entityType;
                private String id;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvDeviceName = (TextView) findViewById(R.id.tvDeviceName);
        tvTemperature = (TextView) findViewById(R.id.tvTemperature);
        tvHumidity = (TextView) findViewById(R.id.tvHumidity);
        tClock = (TextClock) findViewById(R.id.textClock1);
        getJWTToken();
        UIHandler =new Handler();
        mHandler = new Handler();
        startRepeatingTask();
    }

    private void getJWTToken() {
        // create a shared instance with custom settings
        OkHttpClient client = new OkHttpClient().newBuilder()
                // An OkHttp interceptor which logs request and response information.
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        // create your json here
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", USERNAME);
            jsonObject.put("password", PASSWORD);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "jsonObject:" + jsonObject.toString());
        RequestBody body = RequestBody.Companion.create(jsonObject.toString(), TYPE_NULL);
        Log.d(TAG, "RequestBody:" + body.toString());

        // An HTTP request
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/auth/login")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(body)
                .build();
        Log.d(TAG, "request:" + request);

        Call call = client.newCall(request);
        // Schedules the request to be executed at some point in the future
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // Display failure message
                Log.d(TAG, "onFailure:" + e.getMessage());
                //Toast.makeText(MainActivity.this, "onFailure:" +
                        //e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                // Display response message
                String tokenResponse = response.body().string();
                Log.d(TAG, "onResponse:" + tokenResponse);
                JWT_TOKEN = (((tokenResponse.split(","))[0].split(":"))[1])
                        .replace("\"", "");
                Log.d(TAG, "JWT_TOKEN:" + JWT_TOKEN);
                parseJWTToken();
                //Toast.makeText(MainActivity.this, "JWT_TOKEN:" + JWT_TOKEN, Toast.LENGTH_LONG).show();
                if (DEVICE_ID == null || DEVICE_ID.isEmpty()) {
                    parseJWTToken();
                }else{
                    getLatestTelemetry();
                }
            }
        });
    }

    // refer to https://www.baeldung.com/java-jwt-token-decode
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decodeToken(String jwttoken) throws JSONException {
        String[] chunks = jwttoken.split("\\.");
        Base64.Decoder decoder = Base64.getDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        return payload;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void parseJWTToken(){
        try {
            String decodeToken = decodeToken(JWT_TOKEN);
            Log.d(TAG, "decodeJWTToken decodeToken:" + decodeToken);
            Gson gson = new Gson();
            JWTDecoded jwtdecoded = gson.fromJson(decodeToken, JWTDecoded.class);
            if(USERNAME.contains("tenant")) {
                Log.d(TAG, "parseJWTToken customerId:" + jwtdecoded.tenantId);
                queryDeviceID("tenant", "");
            } else{
                Log.d(TAG, "parseJWTToken customerId:" + jwtdecoded.customerId);
                queryDeviceID("customer/", jwtdecoded.customerId);
            }

        } catch (JSONException e) {
            Log.d(TAG, "parseJWTToken onFailure:" + e.getMessage());
        }
    }

    private void queryDeviceID(String controlType, String controlTypeId){
        String QUERY_DEVICE_URL = BASE_URL + "/api/" + controlType + controlTypeId +
                "/devices?pageSize=10&page=0";
        OkHttpClient clientDeviceId = new OkHttpClient().newBuilder()
                // An OkHttp interceptor which logs request and response information.
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();
        Request request = new Request.Builder()
                .url(QUERY_DEVICE_URL)
                .addHeader("X-Authorization", "Bearer " + JWT_TOKEN)
                .get()
                .build();
        Log.d(TAG, "queryDeviceID request:" + request);
        Call callDeviceID = clientDeviceId.newCall(request);
        callDeviceID.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d(TAG, "queryDeviceID onFailure:" + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String deviceIDResponse = response.body().string();
                Log.d(TAG, "queryDeviceID onResponse:" + deviceIDResponse);
                Gson gson = new Gson();
                deviceIDs deviceDecoded = gson.fromJson(deviceIDResponse, deviceIDs.class);
                Log.d(TAG, "queryDeviceID totalElements:" + deviceDecoded.totalElements);
                for (int i=0; i < deviceDecoded.totalElements; i++) {
                    Log.d(TAG, "queryDeviceID device name:" + deviceDecoded.data.get(i).name);
                    if(deviceDecoded.data.get(i).name.equals(SELECT_DEVICE_NAME)){
                        DEVICE_ID = deviceDecoded.data.get(i).id.id.toString();
                    }
                }
                Log.d(TAG, "queryDeviceID deviceID:" + DEVICE_ID);
            }
        });
    }

    private void getLatestTelemetry() {

        String TELEMETRY_URL = BASE_URL + "/api/plugins/telemetry/DEVICE/" +
                DEVICE_ID + "/values/timeseries?keys=temperature,humidity";
        Log.d(TAG, "getLatestTelemetry JWT_TOKEN:" + JWT_TOKEN);

        OkHttpClient clientTelemetry = new OkHttpClient().newBuilder()
                // An OkHttp interceptor which logs request and response information.
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();

        Request request = new Request.Builder()
                .url(TELEMETRY_URL)
                .addHeader("X-Authorization", "Bearer " + JWT_TOKEN)
                .get()
                .build();
        Log.d(TAG, "request:" + request);

        Call call = clientTelemetry.newCall(request);
        // Schedules the request to be executed at some point in the future
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // Display failure message
                Log.d(TAG, "getLatestTelemetry onFailure:" + e.getMessage());
                temperature = "";
                humidity = "";
                //Toast.makeText(MainActivity.this, "getLatestTelemetry onFailure:" +
                        //e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                // Display response message
                String telemetryResponse = response.body().string();
                Log.d(TAG, "Latest Telemetry onResponse:" + telemetryResponse);
                Gson gson = new Gson();
                Timeseries timeseries = gson.fromJson(telemetryResponse, Timeseries.class);
                temperature = timeseries.temperature.get(0).value;
                humidity = timeseries.humidity.get(0).value;
                new Thread(){
                    public void run(){
                        UIHandler.post(runnableUi);
                    }
                }.start();

                Log.d(TAG, "Latest Telemetry temperature:" + temperature + ", humidity:" + humidity);
                //Toast.makeText(MainActivity.this, "Latest Telemetry temperature:" + temperature +
                        // ", humidity:" + humidity, Toast.LENGTH_LONG).show();
            }
        });
    }
    Runnable runnableUi = new  Runnable() {
        public void run() {
            tvDeviceName.setText("Device: "+"SN00000002");
            tvTemperature.setText(temperature);
            tvHumidity.setText(humidity);
        }
    };

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                getJWTToken(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    protected void onStop() {
        super.onStop();
        stopRepeatingTask();
    }
}