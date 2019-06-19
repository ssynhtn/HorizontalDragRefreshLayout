package com.ssynhtn.horizontaldraglayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * Created by huangtongnao on 2019/4/9.
 * Email: huangtongnao@gmail.com
 */
public class HorizontalDragRefreshLayout extends FrameLayout {
    private static final float MAX_OFFSET_DP = 64;
    private static final String TAG = HorizontalDragRefreshLayout.class.getSimpleName();

    private boolean isDisabled = false;

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    public interface OnDragListener {
        void onLeftDragTriggered();
        void onLeftDragAnimationFinished();
        void onRightDragTriggered();
        void onRightDragAnimationFinished();
    }


    int touchSlop;

    float downX;
    float downY;
    float motionX;
    float dragDist; // 手指拖动距离

    private static final int STATE_IDLE = 0;
    private static final int STATE_DRAGGING = 1;
    private static final int STATE_SETTLING = 2;

    private static final int DIRECTION_LEFT = 0;
    private static final int DIRECTION_RIGHT = 1;
    private int dragDirection;

    int state = STATE_IDLE;

    View child;
    int currentChildOffset;

    OnDragListener onDragListener;
    boolean dragNoticeSent;

    float halfMaxOffset;  // child被拖动最大距离的一半

    public HorizontalDragRefreshLayout(@NonNull Context context) {
        this(context, null);
    }

    public HorizontalDragRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HorizontalDragRefreshLayout);
        halfMaxOffset = typedArray.getDimension(R.styleable.HorizontalDragRefreshLayout_maxDragDistance, displayMetrics.density * MAX_OFFSET_DP) / 2;
        typedArray.recycle();

    }


    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (this.child == null) {
            this.child = child;
        }

        super.addView(child, index, params);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isDisabled) {
            return super.onInterceptTouchEvent(ev);
        }

        if (child == null) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                Log.d(TAG, "intercept action down, record downX");
                downX = ev.getX();
                downY = ev.getY();
                dragNoticeSent = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float touchX = ev.getX();
                float touchY = ev.getY();
                float dist = touchX - downX;
                float distY = touchY - downY;
                Log.d(TAG, "intercept action move");

                if (state == STATE_IDLE && isChildAtLeftEnd()) {
                    Log.d(TAG, "move dist " + dist + ", slop " + touchSlop);
                    if (dist >= touchSlop && Math.abs(dist) >= Math.abs(distY)) {
                        Log.d(TAG, "got ya! start dragging at left");
                        state = STATE_DRAGGING;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        motionX = downX + touchSlop;
                        dragDirection = DIRECTION_LEFT;
                        return true;
                    }
                }

                if (state == STATE_IDLE && isChildAtRightEnd()) {
                    Log.d(TAG, "move dist " + dist + ", slop " + touchSlop);
                    if (dist <= -touchSlop && Math.abs(dist) >= Math.abs(distY)) {
                        Log.d(TAG, "got ya! start dragging at right");
                        state = STATE_DRAGGING;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        motionX = downX - touchSlop;
                        dragDirection = DIRECTION_RIGHT;
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                Log.d(TAG, "intercept action " + (ev.getActionMasked() == MotionEvent.ACTION_UP ? "up" : "cancel"));
                state = STATE_IDLE;
                break;
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isDisabled) {
            return super.onTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                Log.d(TAG, "action down");
                dragNoticeSent = false;
                if (state == STATE_IDLE) {
                    downX = ev.getX();
                    downY = ev.getY();
                    Log.d(TAG, "state idle, record downX");
                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                Log.d(TAG, "action move");
                float touchX = ev.getX();

                if (state == STATE_DRAGGING) {
                    Log.d(TAG, "is dragging");
                    if (dragDirection == DIRECTION_LEFT) {
                        float dx = touchX - motionX;
                        if (dx < 0) {
                            dx = 0;
                        }
                        moveChildBasedOnDragDistance(dx);
                    } else {
                        float dx = touchX - motionX;
                        if (dx > 0) {
                            dx = 0;
                        }
                        moveChildBasedOnDragDistance(dx);
                    }

                    final boolean isDragTriggered = Math.abs(dragDist) >= halfMaxOffset * 3;
                    if (isDragTriggered && onDragListener != null && !dragNoticeSent) {
                        dragNoticeSent = true;
                        if (dragDirection == DIRECTION_LEFT) {
                            onDragListener.onLeftDragTriggered();
                        } else {
                            onDragListener.onRightDragTriggered();
                        }
                    }

                    return true;
                }

                break;

            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                final boolean isUp = ev.getActionMasked() == MotionEvent.ACTION_UP;
                Log.d(TAG, "action " + (ev.getActionMasked() == MotionEvent.ACTION_UP ? "up" : "cancel"));
                if (state == STATE_DRAGGING) {
                    Log.d(TAG, "is dragging, now settling");
                    state = STATE_SETTLING;
                    final boolean isDragTriggered = Math.abs(dragDist) >= halfMaxOffset * 3;
                    ValueAnimator animator = ValueAnimator.ofFloat(currentChildOffset, 0);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float value = (float) animation.getAnimatedValue();
                            moveChildToTarget((int) value);
                        }
                    });
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            state = STATE_IDLE;
                            Log.d(TAG, "animation end, set state to idle");

                            if (onDragListener != null & isDragTriggered && isUp) {
                                if (dragDirection == DIRECTION_LEFT) {
                                    onDragListener.onLeftDragAnimationFinished();
                                } else {
                                    onDragListener.onRightDragAnimationFinished();
                                }
                            }

                        }
                    });
                    animator.setInterpolator(new DecelerateInterpolator());
                    animator.start();
                }

                break;
            }

        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (child == null) return;

        final int childLeft = getPaddingLeft() + currentChildOffset;
        final int childTop = getPaddingTop();
        final int childWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        final int childHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }


    private boolean isChildAtLeftEnd() {
        return !child.canScrollHorizontally(-1);
    }

    private boolean isChildAtRightEnd() {
        return !child.canScrollHorizontally(1);
    }

    /**
     * 参考SwipeRefreshLayout中关于手指移动距离和child view移动距离的关系
     * @param x drag distance
     */
    private void moveChildBasedOnDragDistance(float x) {
        dragDist = x;

        float r = Math.abs(x / halfMaxOffset);
        float y;
        if (r < 1) {
            y = r;
        } else if (r < 3) {
            float d = r - 1;
            y = 1 + d - d * d / 4;
        } else {
            y = 2;
        }

        moveChildToTarget((int) (y * halfMaxOffset * Math.signum(x)));
    }

    private void moveChildToTarget(int target) {
        int offset = target - currentChildOffset;
        child.offsetLeftAndRight(offset);
        currentChildOffset = child.getLeft();
    }

    public void setOnDragListener(OnDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }
}

