package com.checkmate.android.listener;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnSwipeTouchListener implements OnTouchListener 
{
	private Context context = null;
	private boolean own_procces = true;
	private GestureListener listener = null;
    private GestureDetector gestureDetector = null;//new GestureDetector(listener);

    // We can be in one of these 2 states
    static final int NONE = 0;
    static final int ZOOM = 1;
    static final int MOVE = 2;
    int mode = NONE;

    static final int MIN_FONT_SIZE = 10;
    static final int MAX_FONT_SIZE = 50;

    double oldDist = 1f;
    double scale = 1f;
    double prevDist = 0f;
    
    public OnSwipeTouchListener(Context context)
    {
    	this.own_procces = true;
    	this.context = context;
    	
    	this.listener = new GestureListener();
    	this.gestureDetector = new GestureDetector(context, listener);
    }

    public OnSwipeTouchListener(Context context, boolean own_procces)
    {
    	this.own_procces = own_procces;
    	this.context = context;
    	
    	this.listener = new GestureListener();
    	this.gestureDetector = new GestureDetector(context, listener);
    }
    
    public boolean onTouch(final View view, final MotionEvent motionEvent) 
    {
     	touch(motionEvent);
    	checkPinch(view, motionEvent);
    	
    	if (mode == ZOOM)
    		return true;
    	
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) 
        {
            case MotionEvent.ACTION_UP:
            {
            	listener.lastScrollX = 0.0f;
            	listener.lastScrollY = 0.0f;
            	listener.firstY = true;
            }
        }
    	
        if (own_procces)
        {
        	boolean ret = gestureDetector.onTouchEvent(motionEvent);
        	
        	checkTouchMove(view, motionEvent);
        	return ret;
        }
        
        gestureDetector.onTouchEvent(motionEvent);
    	checkTouchMove(view, motionEvent);
        return false;
    }

    private double spacing(MotionEvent event)
    {
    	if (event == null || (event.getPointerCount() < 2))
    		return 0.0f;
    		
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }
    
    public boolean checkPinch(View v, MotionEvent event) 
    {
        switch (event.getAction() & MotionEvent.ACTION_MASK) 
        {
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);

                if (oldDist > 10f) 
                {
                	mode = ZOOM;
                	prevDist = oldDist;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (mode == ZOOM) 
                {
                	if (scale > 1) 
                		pinchOut();
                    else 
                        pinchIn();
                }
                mode = NONE;
                prevDist = 0f;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ZOOM) 
                {
                	double newDist = spacing(event);
                    if (newDist > 10f) 
                    {
                        scale = newDist / oldDist;

                        if (scale > 1) 
                            scale = 1.1f;
                        else 
                        	if (scale < 1) 
                                scale = 0.95f;
                        
                    }
                    
                   	pinchMove(prevDist < newDist);
                   	prevDist = newDist;
                }
                break;
        }
        return false;
    }
    
//    void printSamples(MotionEvent ev) {
//        final int historySize = ev.getHistorySize();
//        final int pointerCount = ev.getPointerCount();
//        for (int h = 0; h < historySize; h++) {
//        	if (Debug.LOG) Log.e(TAG, "Touch Move: At time " + ev.getHistoricalEventTime(h));
//            for (int p = 0; p < pointerCount; p++) {
//            	if (Debug.LOG) Log.e(TAG, "Touch Move: pointer "+ 
//                    ev.getPointerId(p) + ": (" + ev.getHistoricalX(p, h) + "," + ev.getHistoricalY(p, h) + ")");
//            }
//        }
//       	if (Debug.LOG) Log.e(TAG, "Touch Move: At time " + ev.getEventTime());
//                for (int p = 0; p < pointerCount; p++) {
//                	if (Debug.LOG) Log.e(TAG, "Touch Move: pointer "+ 
//                            ev.getPointerId(p) + ": (" + ev.getX() + "," + ev.getY() + ")");
//        }
//    }
    
    
    public boolean checkTouchMove(View v, MotionEvent e) 
    {
    	if (mode != NONE && mode != MOVE)
    		return false;
    	
//    	printSamples(e);
    	
        float x = e.getX();
        float y = e.getY();
    	switch (e.getAction() & MotionEvent.ACTION_MASK) 
        {
            case MotionEvent.ACTION_DOWN:
            	mode = MOVE;
            	touchDown((int)x, (int)y, (int)e.getRawX(), (int)e.getRawY());
                break;
            case MotionEvent.ACTION_UP:
            	touchUp((int)x, (int)y, (int)e.getRawX(), (int)e.getRawY());
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                //if (Debug.LOG) Log.d(TAG, "Touch Move: (" + x + "," + y + ")");
                touchMove((int)x, (int)y, (int)e.getRawX(), (int)e.getRawY());
                break;
        }
        return false;
    }
    
    private final class GestureListener extends SimpleOnGestureListener 
    {

        private static final float SWIPE_THRESHOLD = 50.0f;
        private static final float SWIPE_VELOCITY_THRESHOLD = 100.0f;

        private static final float SCROLL_THRESHOLD = 10.0f;
        public float lastScrollX = 0.0f;
        public float lastScrollY = 0.0f;
        public boolean firstY = true;
        
        @Override
        public boolean onDown(MotionEvent e) 
        {
        	lastScrollX = 0.0f;
        	lastScrollY = 0.0f;
        	firstY = true;

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) 
        {
            float x = e.getX();
            float y = e.getY();

            doubleTap((int)x, (int)y);
            return true;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) 
        {
            //if (Debug.LOG) Log.d("onScroll", "scroll at: (" + ((e2.getY() - lastScrollY)) + "," + SCROLL_THRESHOLD + ")");
            if (lastScrollX != 0.0f && lastScrollY != 0.0f)
            {
            	if (firstY)
            	{
            		firstY = false;
            		return true;
            	}	
            	
            	float deltaX = (e2.getX() - lastScrollX);
            	float deltaY = (e2.getY() - lastScrollY);
            	
            	if (Math.abs(deltaX) < Math.abs(deltaY))
            	{	
                	if (deltaY > SCROLL_THRESHOLD)
	                	scrollDown((int)e2.getX(), (int)e2.getY(), (int)e2.getRawX(), (int)e2.getRawY());
	                else
	                    if (deltaY < (-SCROLL_THRESHOLD))
	                    	scrollUp((int)e2.getX(), (int)e2.getY(), (int)e2.getRawX(), (int)e2.getRawY());
	                    else
	                    	return true;
            	}
            }
            
            lastScrollX = e2.getX();
            lastScrollY = e2.getY();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) 
        {
            float x = e.getX();
            float y = e.getY();

            longPress((int)x, (int)y);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) 
        {
            float x = e.getX();
            float y = e.getY();

            singleTap((int)x, (int)y);
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) 
        {
        	
            boolean result = false;
            try 
            {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) 
                {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) 
                    {
                        if (diffX > 0.0f) 
                        {
                            swipeRight((int)e2.getX(), (int)e2.getY());
                        } 
                        else 
                        {
                            swipeLeft((int)e2.getX(), (int)e2.getY());
                        }
                        return true;
                    }
                } 
                else 
                {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) 
                    {
                        if (diffY > 0) 
                        {
                            swipeBottom();
                        } 
                        else 
                        {
                            swipeTop();
                        }
                        return true;
                    }
                }
                //onSingleTapConfirmed(e1);
            } 
            catch (Exception exception) 
            {
            //    exception.printStackTrace();
            }
            return result;
        }
    }

    public void touch(MotionEvent event){};
    public void touchDown(int x, int y, int rawx, int rawy){};
    public void touchMove(int x, int y, int rawx, int rawy){};
    public void touchUp(int x, int y, int rawx, int rawy){};
    public void swipeLeft(int x, int y){};
    public void swipeRight(int x, int y){};
    public void swipeTop(){};
    public void swipeBottom(){};
    public void doubleTap(int x, int y){};
    public void singleTap(int x, int y){};
    public void longPress(int x, int y){};
    public void pinchOut(){};
    public void pinchIn(){};
    public void pinchMove(boolean isGrow){};
    public void scrollUp(int x, int y, int rawx, int rawy){};
    public void scrollDown(int x, int y, int rawx, int rawy){};
    
	final public static String TAG = "OnSwipeTouchListener";
}	
