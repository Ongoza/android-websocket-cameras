package com.ongoza.camsControlClient;

import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.WebSocket;
import okio.ByteString;

public class PhotoHandler implements Camera.PictureCallback {
    //    private final Context context;
    private String TAG ="CamsControlClient";
    private WebSocketActivity cur;
    public PhotoHandler( WebSocketActivity act) {
        Log.d(TAG, "init callback.");
        this.cur = act;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera){
        Log.d(TAG, "New Image start sent.");
        cur.webSocket.send(ByteString.of(data));
        Log.d(TAG, "New Image sent size="+data.length);
//        if(cur.isStream && cur.mCamera!=null){
//            cur.showLog("+");
//            cur.doClick(); }
//        else {cur.showLog("-");}
        if(false){
            File pictureFileDir = getDir();
            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                Log.d(TAG, "Can't create directory to save image.");
                //            Toast.makeText(context, "Can't create directory to save image.", Toast.LENGTH_LONG).show();
                return;
            }
            Log.d(TAG, "New Image try save:");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "Picture_" + date + ".jpg";
            String filename = pictureFileDir.getPath() + File.separator + photoFile;
            File pictureFile = new File(filename);
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d(TAG, "New Image saved:" + photoFile);
//                webSocket.send("test");
                Log.d(TAG, "2 New Image saved:" + photoFile);
                //            Toast.makeText(context, "New Image saved:" + photoFile, Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Log.d(TAG, "File" + filename + "not saved: " + error.getMessage());
                //            webSocket.send("test not ok");
                //            Log.d(TAG, "File" + filename + "not saved: " + error.getMessage());
                //            Toast.makeText(context, "Image could not be saved.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private File getDir() {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "CameraAPIDemo");
    }
}