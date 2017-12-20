package com.example.lmorda.websocketchat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import okhttp3.Request;
import okhttp3.Response;

public class ServerSentEventActivity extends AppCompatActivity {

    private TextView tvOutput;

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvOutput = findViewById(R.id.output);

        Request request = new Request.Builder().url("http://10.0.2.2:8080/movies/5a39a9aba50b2a0570adac86/events").build();
        OkSse okSse = new OkSse();
        ServerSentEvent sse = okSse.newServerSentEvent(request, new ServerSentEvent.Listener() {
            @Override
            public void onOpen(ServerSentEvent sse, Response response) {
                // When the channel is opened
            }

            @Override
            public void onMessage(ServerSentEvent sse, String id, String event, String message) {
                // When a message is received
                output(message);
            }

            @Override
            public void onComment(ServerSentEvent sse, String comment) {
                // When a comment is received
            }

            @Override
            public boolean onRetryTime(ServerSentEvent sse, long milliseconds) {
                return true; // True to use the new retry time received by SSE
            }

            @Override
            public boolean onRetryError(ServerSentEvent sse, Throwable throwable, Response response) {
                return true; // True to retry, false otherwise
            }

            @Override
            public void onClosed(ServerSentEvent sse) {
                // Channel closed
            }

            @Override public Request onPreRetry(ServerSentEvent sse, Request originalRequest) {
                return null;
            };
        });
    }

    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("websocketchat", txt);
                tvOutput.setText(tvOutput.getText().toString() + "\n\n" + txt);
            }
        });
    }

}
