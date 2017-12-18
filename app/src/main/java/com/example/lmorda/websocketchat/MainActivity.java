package com.example.lmorda.websocketchat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    private TextView tvOutput;

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvOutput = findViewById(R.id.output);
        Request request = new Request.Builder().url("ws://10.0.2.2:8080/websocket/chat").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        OkHttpClient okHttpClient = new OkHttpClient();
        WebSocket webSocket = okHttpClient.newWebSocket(request, listener);
        okHttpClient.dispatcher().executorService().shutdown();
    }

    private final class EchoWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            String json = "{\"type\":\"chat\",\"message\":\"im online, whats up\"}";
            output("Tx: " + json);
            webSocket.send(json);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            output("Rx: " + text);
            // Check the WebSocket message type
            if(text.contains("chat")) {
                // do something chat related
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Rx bytes: " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closed: " + code + " / " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("Error: " + t.getMessage());
        }
    }

    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("websocketchat", txt);
                tvOutput.setText(tvOutput.getText().toString() + "\n" + txt);
            }
        });
    }

}
