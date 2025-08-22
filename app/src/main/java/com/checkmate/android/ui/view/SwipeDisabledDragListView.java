package com.checkmate.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Extended DragListView that completely disables horizontal swipe gestures
 * to prevent accidental navigation in ViewPager2
 */
public class SwipeDisabledDragListView extends DragListView {
    
    private float initialX = 0f;
    private float initialY = 0f;
    private static final float SWIPE_THRESHOLD = 50; // pixels
    
    public SwipeDisabledDragListView(Context context) {
        super(context);
    }
    
    public SwipeDisabledDragListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Always intercept horizontal swipes to prevent ViewPager navigation
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                initialY = ev.getY();
                break;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = Math.abs(ev.getX() - initialX);
                float deltaY = Math.abs(ev.getY() - initialY);
                
                // If horizontal movement is significant, intercept it
                if (deltaX > SWIPE_THRESHOLD && deltaX > deltaY) {
                    // Consume the horizontal swipe
                    return true;
                }
                break;
        }
        
        // For vertical scrolls, use parent's implementation
        return super.onInterceptTouchEvent(ev);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Handle the intercepted horizontal swipes
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            float deltaX = Math.abs(ev.getX() - initialX);
            float deltaY = Math.abs(ev.getY() - initialY);
            
            // If it's a horizontal swipe, consume it but do nothing
            if (deltaX > SWIPE_THRESHOLD && deltaX > deltaY) {
                return true;
            }
        }
        
        return super.onTouchEvent(ev);
    }
}