package com.ongoza.camsControlClient;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;


public class WebSocketActivity extends AppCompatActivity {

    private TextView tvOutput;
    private TextView outputProgress;
    private ProgressBar bar;
    private static final String TAG = "CamsControlClient";
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    public Camera mCamera = null;
    private int cameraId = 0;
    public WebSocket webSocket;
    public boolean is_server_available = false;
    private Request request;
    private String serverTemplate = ":9090/cam";
    private EchoWebSocketListener listener = new EchoWebSocketListener();
    private OkHttpClient okHttpClient;
    private String my_name;
    private String server_ip = "";
    private long lastTime;
    private SharedPreferences sharedPref;
    public boolean isStream = false;
    public boolean isShowLog = true;
    private int width;
    private  int height;
    private  SurfaceView surface;
    public String subnet = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvOutput = findViewById(R.id.output);
        outputProgress = findViewById(R.id.outputProgress);
        bar = findViewById(R.id.progBar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        checkPermission();
        sharedPref = getSharedPreferences("myPref", MODE_PRIVATE);
        server_ip = sharedPref.getString("ServerIP", "");
        my_name = Build.MODEL;
        surface = findViewById(R.id.surfaceView);
        ViewGroup.LayoutParams params = surface.getLayoutParams();
        params.height = 1280; // portrait mode only
        surface.setLayoutParams(params);
        boolean isOk = true;
        try{mCamera = Camera.open(cameraId);}
        catch (Exception e){
            isOk = false;
            Log.d(TAG,"can not open camera=");
            outputProgress.setVisibility(View.VISIBLE);
            outputProgress.setText("Can not open camera");
            if(mCamera!= null){ mCamera.release();}
            mCamera = null;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() { System.exit(0); }
            }, 5000);

        }
        if(isOk){
            try {mCamera.setPreviewDisplay(surface.getHolder()); }
            catch (Exception e){Log.d(TAG, "onCreate: preview get holder error"); }
            Log.d(TAG,"server="+server_ip);
            Log.d(TAG, "BRAND: " + Build.BRAND );
            Log.d(TAG, "MANUFACTURER: " + Build.MANUFACTURER );
            Log.d(TAG, "MODEL: " + Build.MODEL );
            Log.d(TAG, "PRODUCT: " + Build.PRODUCT );
            if(!server_ip.isEmpty()){
                Log.d(TAG,"2server="+server_ip);
                request = new Request.Builder().url(server_ip).build();
                okHttpClient = new OkHttpClient().newBuilder().readTimeout(1000, TimeUnit.MILLISECONDS).build();
                webSocket = okHttpClient.newWebSocket(request, listener);
                okHttpClient.dispatcher().executorService().shutdown();
                SystemClock.sleep(1200);
            }
            if(!is_server_available){ findServer();}
            if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                Log.e(TAG,"No camera on this device");
                Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG).show();
                outputProgress.setVisibility(View.VISIBLE);
                outputProgress.setText("No camera on this device");
            } else {
                if (cameraId < 0) {
                    Log.e(TAG, "No front facing camera found.");
                    Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        //                    mCamera = Camera.open(cameraId);
                        Camera.Parameters param = mCamera.getParameters();
                        //                    List<Camera.Size> sz = param.getSupportedPreviewSizes();
                        //                    if (sz != null && !sz.isEmpty()) {
                        //                        Camera.Size item = sz.get(sz.size()-1);
                        //                        param.setPreviewSize(item.width,item.height);
                        param.setPreviewSize(1280, 720);
                        mCamera.setParameters(param);
                        SystemClock.sleep(40);
                        //                    }
                        Camera.Size size = mCamera.getParameters().getPreviewSize();
                        width = size.width;
                        height = size.height;
                        Log.d(TAG, "size=" + width + " " + height);
                        //                    Camera.Parameters param = mCamera.getParameters();
                        //                    Log.d(TAG,param);
                        //                    preview-size-values=176x144,320x240,352x288,480x320,480x368,640x480,720x480,800x480,800x600,864x480,960x540,1280x720;
                        //                    picture-size=3264x2448;  video-size=640x480;
                        //                    preview-size=2048x1536
                        //                    video-size-values=176x144,480x320,640x480,864x480,1280x720,1920x1080;
                        //                    picture-size-values=256x144,320x240,512x288,640x480,1280x720,1024x768,1536x864,1280x960,1792x1008,1600x1200,2304x1296,2048x1536,2816x1584,2560x1920,3584x2016,3264x2448;
                        Log.d(TAG, "camera is ready");
                        showLog("camOk");
                        //                    Log.d(TAG,parameters.flatten());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "camera is not ready");
                        showLog("camNot");
                    }
                }
            }
        }

        final Handler pingHandler = new Handler();
        Runnable pingRunnable = new Runnable() {
            @Override public void run() {
//                Log.d(TAG,"check: "+is_server_available);
                if(is_server_available){
//                      webSocket.send("test");
//                    Log.d(TAG,"check: ok");
                }else{
                    if(webSocket != null){
//                        webSocket.
//                        showLog("|nw1|");
                        webSocket.cancel();
                        webSocket.close(NORMAL_CLOSURE_STATUS, null);
                        webSocket = null;
                        okHttpClient = null;
                    }
                    okHttpClient = new OkHttpClient().newBuilder().readTimeout(1000, TimeUnit.MILLISECONDS).build();;
                    webSocket = okHttpClient.newWebSocket(request, listener);
                    okHttpClient.dispatcher().executorService().shutdown();
                    Log.d(TAG,"Reconnect ot server");
//                    showLog("|nw2|");
                }
                pingHandler.postDelayed(this, 3000);

            }
        };
        pingHandler.postDelayed(pingRunnable, 3000);

    }
    private void checkPermission(){
        if(Build.VERSION.SDK_INT<23){
            Toast.makeText(this, "Please give permission to a camera.", Toast.LENGTH_LONG).show();
        }else{
//
            int camera = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
            Log.d(TAG,"Permission="+camera);
            if(camera == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG,"permissio is ok.");
            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
    }

    public void showLog(String txt){
        if(isShowLog){
            if(tvOutput.getText().toString().length()>150){
                tvOutput.setText(tvOutput.getText().toString().substring(txt.length())+"->"+txt);
            }else{tvOutput.append("->"+txt); }}
    }


    private void findServer(){
        final Handler handler = new Handler();
        Log.d(TAG,"Start find server IP");
        showLog("startScanIP");
        try{
            WifiManager mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = mWifiManager.getDhcpInfo().gateway;
            if(ip!=0){
                Log.d(TAG,"ip="+ip);
                subnet = String.format("%d.%d.%d",(ip & 0xff),(ip >> 8 & 0xff),( ip >> 16 & 0xff));
                Log.d(TAG,"net="+subnet);
                if (bar.getVisibility() != bar.VISIBLE){
                    bar.setVisibility(View.VISIBLE);
                    bar.setProgress(2);
                    outputProgress.setVisibility(View.VISIBLE);
                    }
                outputProgress.setText("Looking for the server. (2/255)");
                new Thread(new Runnable() {
                    int progressStatus = 2;
                    public void run() {
                        while (!is_server_available && (progressStatus < 255)) {
                            try{
                                Log.d(TAG, "try ip: "+progressStatus);
                                doWork(progressStatus);
                                handler.post(new Runnable(){public void run(){ bar.setProgress(progressStatus); }});
                                handler.post(new Runnable(){public void run(){ outputProgress.setText("Looking for the server. ("+progressStatus+"/255)"); }});
                                Thread.sleep(1000);
                            }catch (Exception e){ Log.d(TAG, "exception: find server"); }
                            progressStatus++;
                        }
                        handler.post(new Runnable(){public void run(){ bar.setVisibility(View.INVISIBLE); }});
                        if(!is_server_available){
                            Log.d(TAG, "Did not find the server!");
                            handler.post(new Runnable(){public void run(){ outputProgress.setText("Can not find the server!"); }});
                        }else{ handler.post(new Runnable(){public void run(){outputProgress.setVisibility(View.INVISIBLE); }});
                            }
                    }

                }).start();
            }else{Log.d(TAG, "Wifi; checkHosts() :: Not valid IP : ");}
        }catch (Exception e) {
            Toast.makeText(this, "Can not connect to Wi-Fi!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Wifi; checkHosts() :: IOException e : "+e);
            e.printStackTrace();
        }
    }

    public void doWork(int i) {
        String host = subnet + "." + i + serverTemplate;
        Log.d(TAG, "doWork: "+ host);
        if(webSocket != null){
            webSocket.cancel();
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            webSocket = null;
            okHttpClient = null;
        }
        try {
            server_ip = "ws://"+host;
            request = new Request.Builder().url(server_ip).build();
            okHttpClient = new OkHttpClient();//.newBuilder().readTimeout(1000, TimeUnit.MILLISECONDS).build();;
            webSocket = okHttpClient.newWebSocket(request, listener);
            okHttpClient.dispatcher().executorService().shutdown();

            Log.d(TAG, "checkHosts() :: "+host + " is reachable "+is_server_available+" " + i);
        } catch (Exception e) {
            Log.d(TAG, "doWork: exception");
//                            e.printStackTrace();
        }
        }

        @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the contacts-related task you need to do.
                    Log.d(TAG, "onRequestPermissionsResult: Ok");
                } else {
                    Toast.makeText(this, "No access to a camera on this device", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the functionality that depends on this permission.
                }
            }
            // other 'case' lines to check for other permissions this app might request
        }
    }

    private final class EchoWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG,"open socket " );
            is_server_available = true;
            sharedPref.edit().putString("ServerIP", server_ip).apply();
            Log.d(TAG,"WebSocket connected to "+server_ip);
            webSocket.send("__name="+my_name);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d(TAG,"Rx: " + text);
            switch(text){
                case "photo":
                    if(!isStream){ doClick();
                    }else{ Log.d(TAG,"Camera is streaming!!!");} break;
                case "stream": isStream = true; doClick();  break; // Start record Video
                case "stop":  isStream = false; break; // Stop record Video
                case "test": Log.d(TAG,"Test is ok."); break;
                default : Log.d(TAG,"Command not found.");
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            isStream = false;
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.d(TAG,"Closed: " + code + " / " + reason);
            is_server_available = false;
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
//            showLog("w_e");
            Log.d(TAG,"Error: " + t.getMessage());
            is_server_available = false;
//            isStream = false;
        }
    }


    public void doClick(){
        try {
            if (mCamera != null) {
//                Log.d(TAG,"start click");
                try{mCamera.setPreviewDisplay(surface.getHolder()); }
                catch (Exception e){Log.d(TAG,"error get holder");}
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewCallback(
                        new Camera.PreviewCallback(){
                            @Override
                            public void onPreviewFrame(byte[] data, Camera mCamera){
//                                Log.d(TAG,"click1");
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                YuvImage yuv = new YuvImage(data, ImageFormat.NV21, width, height, null);
                                yuv.compressToJpeg(new Rect(0, 0, width, height), 80, out);
                                webSocket.send(ByteString.of(out.toByteArray()));
//                                Log.d(TAG,"click12="+out.size()+"===");

                                if(!isStream){
//                                    Log.d(TAG,"stopPrev");
                                    mCamera.stopPreview();
                                    mCamera.setPreviewCallback(null);
                                }
                                else{
                                    long delta = (System.currentTimeMillis() - lastTime);
                                    lastTime = System.currentTimeMillis();
        //                            Log.d(TAG, "delta="+Long.toString(delta));
                                    if(delta<200){
                                        int d = (int)(200-delta);
//                                        Log.d(TAG, "sleep="+d);
                                        SystemClock.sleep(d);
                                    }
                                }
                    }});


//                Log.d(TAG,"click2");
                mCamera.startPreview();
//                Log.d(TAG,"click3");
//                mCamera.takePicture(null, null,  new PhotoHandler(this));
            }else{
                showLog("cNull");
                Log.e(TAG,"Camera is not ready. Try open and take picture.");
                mCamera = Camera.open(cameraId);
                Camera.Size size = mCamera.getParameters().getPreviewSize();
                width = size.width;
                height = size.height;
                mCamera.setPreviewCallback(new Camera.PreviewCallback(){
                    public void onPreviewFrame(byte[] data, Camera mCamera){
//                        Log.d(TAG,"pres");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        YuvImage yuv = new YuvImage(data, ImageFormat.NV21, width, height, null);
                        yuv.compressToJpeg(new Rect(0, 0, width, height), 80, out);
                        webSocket.send(ByteString.of(out.toByteArray()));
                        if(!isStream){ mCamera.stopPreview();
                        }else{
                            long delta = (System.currentTimeMillis() - lastTime);
                            lastTime = System.currentTimeMillis();
                            Log.d(TAG, "delta="+Long.toString(delta));
                            if(delta<200){SystemClock.sleep(200-delta);}
                        }
                    }});
                mCamera.startPreview();
//                mCamera.takePicture(null, null,  new PhotoHandler(this));
            }

        }catch (Exception e){
            e.printStackTrace();
//            showLog("cl_e");
            Log.e(TAG,"Error click.");
        }
    }


    public void onClick(View view) { doClick(); }

    @Override
    protected void onDestroy(){
        isStream = false;
        if(mCamera!=null){
            mCamera.stopPreview();
//            preview.setCamera(null);
//            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        if(webSocket!=null){
            webSocket.cancel();
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            webSocket = null;
            okHttpClient = null;
        }
        Log.d(TAG, "onDestroy: 1 ok");
        super.onDestroy();
    }

//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        isStream = false;
//        if(mCamera!=null){
//            mCamera.stopPreview();
//            mCamera.setPreviewCallback(null);
//            mCamera.release();
//            mCamera = null;
//        }
//        if(webSocket!=null){
//            webSocket.cancel();
//            webSocket.close(NORMAL_CLOSURE_STATUS, null);
//            webSocket = null;
//            okHttpClient = null;
//        }
//        Log.d(TAG, "onDestroySurface: 1 ok");
//
//    }

}
