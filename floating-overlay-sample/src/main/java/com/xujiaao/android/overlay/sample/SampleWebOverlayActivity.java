package com.xujiaao.android.overlay.sample;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.VideoView;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.xujiaao.android.overlay.FloatingOverlay;

import org.json.JSONException;
import org.json.JSONObject;

public class SampleWebOverlayActivity extends AppCompatActivity {

    private FloatingOverlay mFloatingOverlay;
    private FloatingOverlay.LayoutParams mFloatingLayoutParams;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sample_web_overlay_act);
        initComponent();
    }

    @SuppressLint("InflateParams")
    private void initComponent() {
        final BridgeWebView webView = findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/sample.html");

        webView.registerHandler("show", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                try {
                    final JSONObject json = new JSONObject(data);

                    if (mFloatingOverlay == null) {
                        mFloatingOverlay = FloatingOverlay.create(webView);

                        final View view = mFloatingOverlay.inflate(R.layout.video, true);
                        mFloatingLayoutParams =
                                (FloatingOverlay.LayoutParams) view.getLayoutParams();

                        mFloatingOverlay.findViewById(R.id.start)
                                .setOnClickListener(mOnClickListener);
                        mFloatingOverlay.findViewById(R.id.pause)
                                .setOnClickListener(mOnClickListener);
                    }

                    final VideoView videoView = mFloatingOverlay.findViewById(R.id.videoView);
                    videoView.stopPlayback();
                    videoView.setVideoURI(Uri.parse(json.getString("uri")));
                    videoView.start();

                    updateBounds(new JSONObject(data));
                    mFloatingOverlay.show();
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });

        webView.registerHandler("update", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                if (mFloatingOverlay != null) {
                    try {
                        updateBounds(new JSONObject(data));
                        mFloatingOverlay.update();
                    } catch (JSONException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        });

        webView.registerHandler("close", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                if (mFloatingOverlay != null) {
                    mFloatingOverlay.dismiss();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mFloatingOverlay != null) {
            final VideoView videoView = mFloatingOverlay.findViewById(R.id.videoView);
            videoView.stopPlayback();
        }
    }

    private void updateBounds(JSONObject json) throws JSONException {
        final float density = getResources().getDisplayMetrics().density;
        mFloatingLayoutParams.x = (int) (json.getInt("left") * density);
        mFloatingLayoutParams.y = (int) (json.getInt("top") * density);
        mFloatingLayoutParams.width = (int) (json.getInt("width") * density);
        mFloatingLayoutParams.height = (int) (json.getInt("height") * density);
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.start: {
                    mFloatingOverlay.<VideoView>findViewById(R.id.videoView).start();
                    break;
                }
                case R.id.pause: {
                    mFloatingOverlay.<VideoView>findViewById(R.id.videoView).pause();
                    break;
                }
            }
        }
    };
}