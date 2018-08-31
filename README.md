# FloatingOverlay

山寨版 ViewOverlay :bowtie: 支持浮层滑动/点击 (Android 4.0 及以上).


## 用途

替换 WebView 视频播放器, 并实现小窗播放 (仿腾讯新闻). [下载示例](https://github.com/xujiaao/FloatingOverlay/releases/latest)

![](doc/sample.gif)


## 用法

复制 [FloatingOverlay.java](floating-overlay/src/main/java/com/xujiaao/android/overlay/FloatingOverlay.java)

````java
final FloatingOverlay.LayoutParams layoutParams = new FloatingOverlay.LayoutParams();
layoutParams.x = 100;
layoutParams.y = 100;
layoutParams.width = 100;
layoutParams.height = 100;

final VideoView videoView = new VideoView(this);
videoView.setLayoutParams(layoutParams);

final FloatingOverlay floatingOverlay = FloatingOverlay.create(webView);
floatingOverlay.add(videoView);
floatingOverlay.show();
````