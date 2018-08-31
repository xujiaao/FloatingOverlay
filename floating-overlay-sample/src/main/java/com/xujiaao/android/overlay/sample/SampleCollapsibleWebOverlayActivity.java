package com.xujiaao.android.overlay.sample;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.VideoView;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.xujiaao.android.overlay.FloatingOverlay;

import org.json.JSONException;
import org.json.JSONObject;

public class SampleCollapsibleWebOverlayActivity extends AppCompatActivity {

    private VideoOverlayHolder mVideoOverlayHolder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sample_collapsible_web_overlay_act);
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

                    if (mVideoOverlayHolder == null) {
                        mVideoOverlayHolder = new VideoOverlayHolder(webView);
                    }

                    updateBounds(mVideoOverlayHolder.bounds, json);
                    mVideoOverlayHolder.show(
                            json.optString("title"),
                            json.getString("uri"));
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });

        webView.registerHandler("update", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                if (mVideoOverlayHolder != null) {
                    try {
                        updateBounds(mVideoOverlayHolder.bounds, new JSONObject(data));
                        mVideoOverlayHolder.update();
                    } catch (JSONException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        });

        webView.registerHandler("close", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                if (mVideoOverlayHolder != null) {
                    mVideoOverlayHolder.dismiss();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mVideoOverlayHolder != null) {
            mVideoOverlayHolder.dismiss();
        }
    }

    private void updateBounds(Rect bounds, JSONObject json) throws JSONException {
        final float density = getResources().getDisplayMetrics().density;
        bounds.left = (int) (json.getInt("left") * density);
        bounds.top = (int) (json.getInt("top") * density);
        bounds.right = bounds.left + (int) (json.getInt("width") * density);
        bounds.bottom = bounds.top + (int) (json.getInt("height") * density);
    }

    private class VideoOverlayHolder implements
            View.OnClickListener,
            FloatingOverlay.OnUpdateListener {

        final Rect bounds = new Rect();

        private final View mWebView;

        private final FloatingOverlay mFloatingOverlay;
        private final View mVideoContainer;
        private final View mVideoController;
        private final VideoView mVideoView;
        private final FloatingOverlay.LayoutParams mVideoLayoutParams;

        private final View mHeaderContainer;
        private final View mHeaderPlaceholder;
        private final TextView mHeaderTitle;

        private boolean mFirstUpdate;
        private boolean mCollapsed;

        VideoOverlayHolder(View webView) {
            mWebView = webView;

            mFloatingOverlay = FloatingOverlay.create(webView);
            mFloatingOverlay.setOnUpdateListener(this);

            mVideoContainer = mFloatingOverlay.inflate(R.layout.video, true);

            mVideoController = mFloatingOverlay.findViewById(R.id.videoController);
            mVideoController.findViewById(R.id.start).setOnClickListener(this);
            mVideoController.findViewById(R.id.pause).setOnClickListener(this);

            mVideoView = mVideoContainer.findViewById(R.id.videoView);
            mVideoLayoutParams = (FloatingOverlay.LayoutParams) mVideoContainer.getLayoutParams();

            final ViewStub stub = findViewById(R.id.headerContainer);
            mHeaderContainer = stub.inflate();
            mHeaderContainer.setVisibility(View.INVISIBLE);
            mHeaderContainer.findViewById(R.id.headerClose).setOnClickListener(this);

            mHeaderPlaceholder = mHeaderContainer.findViewById(R.id.headerPlaceholder);
            mHeaderTitle = mHeaderContainer.findViewById(R.id.headerTitle);
        }

        void show(String title, String uri) {
            mHeaderTitle.setText(title);
            mVideoView.setVideoURI(Uri.parse(uri));
            mVideoView.start();

            mFirstUpdate = false;
            mFloatingOverlay.show();
        }

        void update() {
            mFirstUpdate = false;
            mFloatingOverlay.update();
        }

        void dismiss() {
            mHeaderContainer.setVisibility(View.INVISIBLE);
            mVideoView.stopPlayback();

            mFloatingOverlay.dismiss();
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.start: {
                    mVideoView.start();
                    break;
                }
                case R.id.pause: {
                    mVideoView.pause();
                    break;
                }
                case R.id.headerClose: {
                    dismiss();
                    break;
                }
            }
        }

        @Override
        public void beforeUpdating() {
            final int scrollY = mWebView.getScrollY();

            final boolean collapsed = scrollY > bounds.bottom
                    || scrollY + mWebView.getHeight() < bounds.top;
            if (!mFirstUpdate || mCollapsed != collapsed) {
                mFirstUpdate = true;
                mCollapsed = collapsed;

                if (collapsed) {
                    mHeaderContainer.setVisibility(View.VISIBLE);
                    mVideoController.setVisibility(View.GONE);

                    mVideoLayoutParams.x = mHeaderPlaceholder.getLeft();
                    mVideoLayoutParams.y = mHeaderPlaceholder.getTop();
                    mVideoLayoutParams.width = mHeaderPlaceholder.getWidth();
                    mVideoLayoutParams.height = mHeaderPlaceholder.getHeight();
                    mVideoLayoutParams.scrolling = false;
                } else {
                    mHeaderContainer.setVisibility(View.INVISIBLE);
                    mVideoController.setVisibility(View.VISIBLE);

                    mVideoLayoutParams.x = bounds.left;
                    mVideoLayoutParams.y = bounds.top;
                    mVideoLayoutParams.width = bounds.width();
                    mVideoLayoutParams.height = bounds.height();
                    mVideoLayoutParams.scrolling = true;
                }
            }
        }
    }
}