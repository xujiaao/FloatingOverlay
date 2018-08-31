# FloatingOverlay

山寨版 ViewOverlay :bowtie:. 支持浮层滑动/点击 (Android 4.0 及以上可用).


## 用途

替换 WebView 视频播放器, 并实现小窗播放.

[下载示例](https://github.com/xujiaao/FloatingOverlay/releases/latest)


## 用法

复制 [FloatingOverlay.java](floating-overlay/src/main/java/com/xujiaao/android/overlay/FloatingOverlay.java)

````java
final FloatingOverlay floatingOverlay = FloatingOverlay.create(webView);

final View view = floatingOverlay.inflate(R.layout.video, true);
final FloatingOverlay.LayoutParams layoutParams = (FloatingOverlay.LayoutParams) view.getLayoutParams();
layoutParams.x = 100;
layoutParams.y = 100;
layoutParams.width = 100;
layoutParams.height = 100;

floatingOverlay.show();
````