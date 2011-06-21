/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//
//Sweet... modding by PuZZleDucK, I'll take some credit but most goes to 
//the fantastic ppls at the big G.
//
//V.1.0: tracking at top and left... more to come surely
//
// might try to change models now...
//




package com.example.puzzleduck.targetLiveWallpaper;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/*
 * This animated wallpaper draws a rotating wireframe shape. It is similar to
 * example #1, but has a choice of 2 shapes, which are user selectable and
 * defined in resources instead of in code.
 */


public class TargetLiveWallpaper extends WallpaperService {

    public static final String SHARED_PREFS_NAME="cube2settings";

    
    
    //yrst
    static class ThreeDPoint {
        float x;
        float y;
        float z;
    }

    static class ThreeDLine {
        int startPoint;
        int endPoint;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new CubeEngine();
    }

    class CubeEngine extends Engine 
        implements SharedPreferences.OnSharedPreferenceChangeListener {

        private final Handler mHandler = new Handler();

        ThreeDPoint [] mOriginalPoints;
        ThreeDPoint [] mRotatedPoints;
        ThreeDLine [] mLines;

//        ThreeDLine [] mTargetLines;
        
        private final Paint mPaint = new Paint();
//        private float mOffset;
        private float mTouchX = -1;
        private float mTouchY = -1;
//        private long mStartTime;
        private float mCenterX1;
        private float mCenterY1;

        private float mLeftTargetX;
        private float mLeftTargetY;

        private float mTopTargetX;
        private float mTopTargetY;
        
        private float mLastTouchX = 0;
        private float mLastTouchY = 0;
        
        private int mPulseN = 8;

        private final Runnable mDrawCube = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private boolean mVisible;
        private SharedPreferences mPrefs;

        CubeEngine() {
            // Create a Paint to draw the lines for our cube
            final Paint paint = mPaint;
            paint.setColor(0xffffffff);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

//            mStartTime = SystemClock.elapsedRealtime();

            mPrefs = TargetLiveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

            String shape = prefs.getString("cube2_shape", "cube");

            // read the 3D model from the resource
            readModel(shape);
        }

        private void readModel(String prefix) {
            // Read the model definition in from a resource.

            // get the resource identifiers for the arrays for the selected shape
            int pid = getResources().getIdentifier(prefix + "points", "array", getPackageName());
            int lid = getResources().getIdentifier(prefix + "lines", "array", getPackageName());
//            int pid = getResources().getIdentifier("diamondpoints", "array", getPackageName());

            String [] p = getResources().getStringArray(pid);
            int numpoints = p.length;
            mOriginalPoints = new ThreeDPoint[numpoints];
            mRotatedPoints = new ThreeDPoint[numpoints];

            for (int i = 0; i < numpoints; i++) {
                mOriginalPoints[i] = new ThreeDPoint();
                mRotatedPoints[i] = new ThreeDPoint();
                String [] coord = p[i].split(" ");
                mOriginalPoints[i].x = Float.valueOf(coord[0]);
                mOriginalPoints[i].y = Float.valueOf(coord[1]);
                mOriginalPoints[i].z = Float.valueOf(coord[2]);
            }

            String [] l = getResources().getStringArray(lid);
            int numlines = l.length;
            
            mLines 		 = new ThreeDLine[numlines];

            for (int i = 0; i < numlines; i++) {
                mLines[i] = new ThreeDLine();
                String [] idx = l[i].split(" ");
                mLines[i].startPoint = Integer.valueOf(idx[0]);
                mLines[i].endPoint = Integer.valueOf(idx[1]);
            }
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawCube);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawCube);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mCenterX1 = 0;//width/2; 
            mCenterY1 = 0;//height/2;
            mLeftTargetX = width/2;
            mLeftTargetY = height/2;
            mTopTargetX = width/2;
            mTopTargetY = height/2;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawCube);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
//            mOffset = xOffset;
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mTouchX = event.getX();
                mTouchY = event.getY();
            } else {
                mTouchX = -1;
                mTouchY = -1;
            }
            super.onTouchEvent(event);
        }

        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here. This example draws a wireframe cube.
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
                    
                    drawTouchPoint(c);
//                    drawConkey(c);
                    drawTopTarget(c);
                    drawLeftTarget(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(mDrawCube);
            if (mVisible) {
                mHandler.postDelayed(mDrawCube, 1000 / 25);
            }
        }

        void drawTopTarget(Canvas c) {
            c.save();
            c.drawColor(0x00000000);
            int oldColor = mPaint.getColor();
            mPaint.setColor(0xff00ff00);

            long now = SystemClock.elapsedRealtime();
            float xrot = (float)0;
            float yrot = (float)now/400;
            rotateAndProjectPoints(xrot, yrot);

            mTopTargetX = mLastTouchX;
            mTopTargetY = 60;
	        c.translate(mTopTargetX, mTopTargetY);
	        drawLines(c);
            mPaint.setColor(oldColor);
          	c.restore(); 
       }
        
        
        void drawLeftTarget(Canvas c) {

            c.save();
            c.drawColor(0x00000000);
            int oldColor = mPaint.getColor();

            long now = SystemClock.elapsedRealtime();
            float xrot = (float) now/400;
            float yrot = (float) 0;
            rotateAndProjectPoints2(xrot, yrot);

            
            mPaint.setColor(0xFFFFFFFF-(0x00000001 * ((int)now)/5 ));
            
            mPaint.setColor(Color.argb(255,255-((int)now/5%200),0,0));       //0-255 or 0xAARRGGBB
            //argb(int alpha, int red, int green, int blue)
            
            mLeftTargetX = 20;
            mLeftTargetY = mLastTouchY;
	        c.translate(mLeftTargetX, mLeftTargetY);
	        drawLines(c);
            mPaint.setColor(oldColor);
          	c.restore();

       }

        
        
        void rotateAndProjectPoints(float xrot, float yrot) {
            int n = mOriginalPoints.length;
            for (int i = 0; i < n; i++) {
                // rotation around X-axis
                ThreeDPoint p = mOriginalPoints[i];
                float x = p.x;
                float y = p.y;
                float z = p.z;
                float newy = (float)(Math.sin(xrot) * z + Math.cos(xrot) * y);
                float newz = (float)(Math.cos(xrot) * z - Math.sin(xrot) * y);

                // rotation around Y-axis
                float newx = (float)(Math.sin(yrot) * newz + Math.cos(yrot) * x);
                newz = (float)(Math.cos(yrot) * newz - Math.sin(yrot) * x);

                // 3D-to-2D projection
                float screenX = newx / (4 - newz / 400);
                float screenY = newy / (4 - newz / 400);

                mRotatedPoints[i].x = screenX;
                mRotatedPoints[i].y = screenY;
                mRotatedPoints[i].z = 0;
            }
        }        
        void rotateAndProjectPoints2(float xrot, float yrot) {
            int n = mOriginalPoints.length;
            for (int i = 0; i < n; i++) {
                // rotation around X-axis
                ThreeDPoint p = mOriginalPoints[i];
                float y = p.x;
                float x = p.y;
                float z = p.z;
                float newy = (float)(Math.sin(xrot) * z + Math.cos(xrot) * y);
                float newz = (float)(Math.cos(xrot) * z + Math.sin(xrot) * y);

                // rotation around Y-axis
                float newx = (float)(Math.sin(yrot) * newz + Math.cos(yrot) * x);
                newz = (float)(Math.cos(yrot) * newz + Math.sin(yrot) * x);

                // 3D-to-2D projection
                float screenX = newx / (4 - newz / 400);
                float screenY = newy / (4 - newz / 400);

                mRotatedPoints[i].x = screenX;
                mRotatedPoints[i].y = screenY;
                mRotatedPoints[i].z = 0;
            }
        }

        void drawLines(Canvas c) {
            int n = mLines.length;
            for (int i = 0; i < n; i++) {
                ThreeDLine l = mLines[i];
                ThreeDPoint start = mRotatedPoints[l.startPoint];
                ThreeDPoint end = mRotatedPoints[l.endPoint];
                c.drawLine(start.x, start.y, end.x, end.y, mPaint);
            }
        }

        void drawTouchPoint(Canvas c) {

            c.drawColor(0xff000000);
            int oldColor = mPaint.getColor();
            for(int i = 0; i < 6; i++)
            {
                mPaint.setColor(0xffff0000-(0x22000000 * ((i-mPulseN)%8) ));
                c.drawCircle(mLastTouchX, mLastTouchY, 12 * i, mPaint);
            }
            mPaint.setColor(oldColor);

            if (mTouchX >=0 && mTouchY >= 0) {                

            	if(--mPulseN <= 0)
            		mPulseN = 8;
                    
                // get relative dirs
                float diffX = mTouchX - mLastTouchX;
                float diffY = mTouchY - mLastTouchY;

                mCenterY1 = mCenterY1 + diffY;
                mCenterX1 = mCenterX1 + diffX;
                
                //store for next
                mLastTouchX = mTouchX;
                mLastTouchY = mTouchY;

                
                c.drawText("diffX = " + diffX, 5, 650, mPaint);
                c.drawText("diffY = " + diffY, 5, 670, mPaint);
            }else{
            	mPulseN = 0;
            }
            
        }
        

        void drawConkey(Canvas c) {
            c.drawColor(0x00000000);

            int oldColor = mPaint.getColor();

            mPaint.setColor(0xffff0000);
            //mPaint.setTypeface(null);
            
            c.drawText("Last touch point: (" + (int)mLastTouchX + "," + (int)mLastTouchX + ")", 		5, 100, mPaint);
            c.drawText("Up: " + SystemClock.uptimeMillis(), 		5, 120, mPaint);
            c.drawText("Now: " + SystemClock.elapsedRealtime(), 		5, 140, mPaint);
            c.drawText("This thread: " + SystemClock.currentThreadTimeMillis(), 		5, 160, mPaint);
            c.drawText("This is preview: " + this.isPreview(), 		5, 180, mPaint);
            c.drawText("This is viible: " + this.isVisible(), 		5, 200, mPaint);
            c.drawText("This is viible: " , 		5, 220, mPaint);
            
        	c.drawText("color = " + mPaint.getColor(), 		5, 590, mPaint);
        	c.drawText("mTouchX = " + mTouchX, 		5, 610, mPaint);
            c.drawText("mTouchY = " + mTouchY, 		5, 630, mPaint);
            c.drawText("mCenterX1= " + mCenterX1, 5, 690, mPaint);
            c.drawText("mCenterY1= " + mCenterY1, 5, 710, mPaint);
            mPaint.setColor(oldColor);
            
        }
        
    }
    
    
    
}
