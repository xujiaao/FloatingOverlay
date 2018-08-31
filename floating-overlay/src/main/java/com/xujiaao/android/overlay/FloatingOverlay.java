package com.xujiaao.android.overlay;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public class FloatingOverlay {

    private final OverlayWrapper mWrapper;

    private LayoutInflater mInflater;

    private FloatingOverlay(OverlayWrapper wrapper) {
        mWrapper = wrapper;
    }

    @SuppressWarnings("WeakerAccess")
    public static FloatingOverlay create(@NonNull ViewGroup host, @NonNull View content) {
        return new FloatingOverlay(new OverlayWrapper(host.getContext(), host, content));
    }

    public static FloatingOverlay create(@NonNull View content) throws IllegalStateException {
        return create(getHost(content), content);
    }

    private static ViewGroup getHost(View content) throws IllegalStateException {
        View parent = content;
        while (parent != null) {
            if (parent.getId() == android.R.id.content && parent instanceof ViewGroup) {
                return (ViewGroup) parent;
            }

            if (parent.getParent() instanceof ViewGroup) {
                parent = (ViewGroup) parent.getParent();
            }
        }

        throw new IllegalStateException("Cannot find host view with id 'android.R.id.content'");
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        mWrapper.setOnUpdateListener(listener);
    }

    public void show() {
        mWrapper.show();
    }

    public void dismiss() {
        mWrapper.dismiss();
    }

    public void update() {
        mWrapper.update();
    }

    public View inflate(@LayoutRes int layout, boolean add) {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(mWrapper.getContext());
        }

        final View view = mInflater.inflate(layout, mWrapper, false);
        if (add) {
            add(view);
        }

        return view;
    }

    public void add(View child) {
        if (child != null) {
            mWrapper.addView(child);
        }
    }

    @SuppressWarnings("unused")
    public void remove(View child) {
        if (child != null) {
            mWrapper.removeView(child);
        }
    }

    public final <T extends View> T findViewById(@IdRes int id) {
        return mWrapper.findViewById(id);
    }

    // ---------------------------------------------------------------------------------------------
    // OverlayWrapper
    // ---------------------------------------------------------------------------------------------

    private static class OverlayWrapper extends ViewGroup {

        private static boolean sIgnoreTouchEvents;

        private final int[] mTempLocation = new int[2];
        private final Rect mVisibleBounds = new Rect();

        private final int mTouchSlop;
        private final View mContent;
        private final ViewGroup mHost;

        private boolean mShown;
        private boolean mShownRequested;

        private boolean mDisallowIntercept;
        private int mDownX;
        private int mDownY;
        private View mDownTarget;

        private OnUpdateListener mOnUpdateListener;

        OverlayWrapper(Context context, ViewGroup host, View content) {
            super(context);

            mHost = host;
            mContent = content;
            mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        void setOnUpdateListener(OnUpdateListener listener) {
            mOnUpdateListener = listener;
        }

        void show() {
            if (!setShownRequested(true)) {
                updateInternal();
            }
        }

        void dismiss() {
            setShownRequested(false);
        }

        void update() {
            updateInternal();
        }

        @Override
        protected boolean checkLayoutParams(LayoutParams layoutParams) {
            return layoutParams instanceof FloatingOverlay.LayoutParams;
        }

        @Override
        public FloatingOverlay.LayoutParams generateLayoutParams(AttributeSet attrs) {
            return new FloatingOverlay.LayoutParams(getContext(), attrs);
        }

        @Override
        protected FloatingOverlay.LayoutParams generateLayoutParams(LayoutParams layoutParams) {
            return new FloatingOverlay.LayoutParams(layoutParams);
        }

        @Override
        protected LayoutParams generateDefaultLayoutParams() {
            return new FloatingOverlay.LayoutParams();
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            updateInternal();
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (mShown) {
                canvas.clipRect(mVisibleBounds);
            }

            super.dispatchDraw(canvas);
        }

        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);

            mDisallowIntercept = disallowIntercept;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (!mShown || sIgnoreTouchEvents) {
                return false;
            }

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    mDisallowIntercept = false;

                    final int x = (int) event.getX();
                    final int y = (int) event.getY();

                    if (mVisibleBounds.contains(x, y)) {
                        for (int index = getChildCount() - 1; index >= 0; index--) {
                            final View child = getChildAt(index);
                            if (child.getVisibility() == VISIBLE
                                    && x >= child.getLeft()
                                    && x <= child.getRight()
                                    && y >= child.getTop()
                                    && y <= child.getBottom()) {
                                if (dispatchTransformedTouchEvent(child, event, false)) {
                                    mDownX = x;
                                    mDownY = y;
                                    mDownTarget = child;
                                }
                            }
                        }
                    }

                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mDownTarget != null) {
                        dispatchTransformedTouchEvent(mDownTarget, event, false);

                        if (!mDisallowIntercept) {
                            final int x = (int) event.getX();
                            final int y = (int) event.getY();
                            final int dx = x - mDownX;
                            final int dy = y - mDownY;
                            if ((dx * dx) + (dy * dy) > mTouchSlop * mTouchSlop) {
                                dispatchTransformedTouchEvent(mDownTarget, event, true);
                                mDownTarget = null;

                                sIgnoreTouchEvents = true;
                                final MotionEvent transformedEvent = MotionEvent.obtain(event);
                                try {
                                    transformedEvent.offsetLocation(
                                            mHost.getScrollX() - getLeft(),
                                            mHost.getScrollY() - getTop());

                                    transformedEvent.setAction(MotionEvent.ACTION_CANCEL);
                                    mHost.dispatchTouchEvent(transformedEvent);

                                    transformedEvent.setAction(MotionEvent.ACTION_DOWN);
                                    transformedEvent.offsetLocation(-dx, -dy);
                                    mHost.dispatchTouchEvent(transformedEvent);

                                    transformedEvent.setAction(MotionEvent.ACTION_MOVE);
                                    transformedEvent.offsetLocation(dx, dy);
                                    mHost.dispatchTouchEvent(transformedEvent);
                                } finally {
                                    sIgnoreTouchEvents = false;
                                    transformedEvent.recycle();
                                }

                                return true;
                            }
                        }
                    }

                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (mDownTarget != null) {
                        dispatchTransformedTouchEvent(mDownTarget, event, false);
                        mDownTarget = null;
                        mDisallowIntercept = false;

                        return true;
                    }

                    break;
                }
                default: {
                    if (mDownTarget != null) {
                        dispatchTransformedTouchEvent(mDownTarget, event, false);
                    }
                }
            }

            return mDownTarget != null;
        }

        private boolean dispatchTransformedTouchEvent(@NonNull View child,
                                                      @NonNull MotionEvent event,
                                                      boolean cancel) {
            final boolean handled;

            final MotionEvent transformedEvent = MotionEvent.obtain(event);
            try {
                if (cancel) {
                    transformedEvent.setAction(MotionEvent.ACTION_CANCEL);
                }

                transformedEvent.offsetLocation(
                        -child.getLeft(),
                        -child.getTop());

                handled = child.dispatchTouchEvent(transformedEvent);
            } finally {
                transformedEvent.recycle();
            }

            return handled;
        }

        private boolean setShownRequested(boolean shownRequested) {
            boolean changed = false;
            if (mShownRequested != shownRequested) {
                mShownRequested = shownRequested;
                changed = calculateShownState();

                if (shownRequested) {
                    final ViewTreeObserver observer = mContent.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.addOnGlobalLayoutListener(mOnGlobalLayoutListener);
                    }
                } else {
                    final ViewTreeObserver observer = mContent.getViewTreeObserver();
                    if (observer.isAlive()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            observer.removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
                        } else {
                            observer.removeGlobalOnLayoutListener(mOnGlobalLayoutListener);
                        }
                    }
                }
            }

            return changed;
        }

        private boolean calculateShownState() {
            final boolean shown = mShownRequested && mContent.isShown();

            final boolean changed = mShown != shown;
            if (changed) {
                mShown = shown;

                if (shown) {
                    if (getParent() == null) {
                        mHost.addView(this);
                    }

                    final ViewTreeObserver observer = mContent.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.addOnScrollChangedListener(mOnScrollChangedListener);
                    }
                } else {
                    if (getParent() == mHost) {
                        mHost.removeView(this);
                    }

                    final ViewTreeObserver observer = mContent.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.removeOnScrollChangedListener(mOnScrollChangedListener);
                    }
                }
            }

            return changed;
        }

        private void updateInternal() {
            if (!mShown) {
                return;
            }

            if (mOnUpdateListener != null) {
                mOnUpdateListener.beforeUpdating();
            }

            final int[] tempLocation = mTempLocation;

            getLocationOnScreen(tempLocation);
            final int wrapperScreenX = tempLocation[0];
            final int wrapperScreenY = tempLocation[1];

            final View content = mContent;
            content.getLocationOnScreen(tempLocation);
            final int contentLeft = tempLocation[0] - wrapperScreenX + content.getPaddingLeft();
            final int contentTop = tempLocation[1] - wrapperScreenY + content.getPaddingTop();
            final int contentRight = contentLeft + content.getWidth() - content.getPaddingRight();
            final int contentBottom = contentTop + content.getHeight() - content.getPaddingBottom();

            final int scrollX = content.getScrollX();
            final int scrollY = content.getScrollY();
            final int availableWidth = Math.max(contentRight - contentLeft, 0);
            final int availableHeight = Math.max(contentBottom - contentTop, 0);

            boolean visible = false;

            final Rect visibleBounds = mVisibleBounds;
            if (content.getLocalVisibleRect(visibleBounds)) {
                visible = true;
                visibleBounds.offset(contentLeft - scrollX, contentTop - scrollY);
            } else {
                visibleBounds.setEmpty();
            }

            if (visible) {
                for (int index = 0, count = getChildCount(); index < count; index++) {
                    final View child = getChildAt(index);
                    if (child.getVisibility() != GONE) {
                        final FloatingOverlay.LayoutParams layoutParams =
                                (FloatingOverlay.LayoutParams) child.getLayoutParams();

                        final int widthMeasureSpec =
                                generateMeasureSpec(layoutParams.width, availableWidth);
                        final int heightMeasureSpec =
                                generateMeasureSpec(layoutParams.height, availableHeight);
                        if (child.isLayoutRequested()
                                || layoutParams.widthMeasureSpec != widthMeasureSpec
                                || layoutParams.heightMeasureSpec != heightMeasureSpec) {
                            layoutParams.widthMeasureSpec = widthMeasureSpec;
                            layoutParams.heightMeasureSpec = heightMeasureSpec;

                            child.measure(widthMeasureSpec, heightMeasureSpec);
                        }

                        int x = layoutParams.x + contentLeft;
                        int y = layoutParams.y + contentTop;

                        if (layoutParams.scrolling) {
                            x -= scrollX;
                            y -= scrollY;
                        }

                        final int measuredWidth = child.getMeasuredWidth();
                        final int measuredHeight = child.getMeasuredHeight();
                        if (child.isLayoutRequested()
                                || child.getWidth() != measuredWidth
                                || child.getHeight() != measuredHeight) {
                            child.layout(x, y, x + measuredWidth, y + measuredHeight);
                        } else {
                            final int dx = x - child.getLeft();
                            final int dy = y - child.getTop();

                            child.offsetLeftAndRight(dx);
                            child.offsetTopAndBottom(dy);

                            if (dx != 0 || dy != 0) {
                                invalidate();
                            }
                        }
                    }
                }
            }
        }

        private static int generateMeasureSpec(int layoutSize, int availableSize) {
            if (layoutSize == LayoutParams.MATCH_PARENT) {
                return MeasureSpec.makeMeasureSpec(availableSize, MeasureSpec.EXACTLY);
            }

            if (layoutSize == LayoutParams.WRAP_CONTENT) {
                return MeasureSpec.makeMeasureSpec(availableSize, MeasureSpec.AT_MOST);
            }

            return MeasureSpec.makeMeasureSpec(Math.max(layoutSize, 0), MeasureSpec.EXACTLY);
        }

        private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener =
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        if (!calculateShownState()) {
                            updateInternal();
                        }
                    }
                };

        private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener =
                new android.view.ViewTreeObserver.OnScrollChangedListener() {

                    @Override
                    public void onScrollChanged() {
                        updateInternal();
                    }
                };
    }

    // ---------------------------------------------------------------------------------------------
    // LayoutParams
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("WeakerAccess")
    public static class LayoutParams extends ViewGroup.LayoutParams {

        private static final int[] LAYOUT_ATTRS = new int[]{
                android.R.attr.layout_x,
                android.R.attr.layout_y
        };

        public boolean scrolling = true;
        public int x;
        public int y;

        int widthMeasureSpec;
        int heightMeasureSpec;

        public LayoutParams() {
            super(WRAP_CONTENT, WRAP_CONTENT);
        }

        LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

            final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.x = a.getDimensionPixelSize(0, this.x);
            this.y = a.getDimensionPixelSize(1, this.y);

            a.recycle();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // OnUpdateListener
    // ---------------------------------------------------------------------------------------------

    public interface OnUpdateListener {

        void beforeUpdating();
    }
}