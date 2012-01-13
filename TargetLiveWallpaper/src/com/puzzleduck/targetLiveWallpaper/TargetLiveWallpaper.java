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

package com.puzzleduck.targetLiveWallpaper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;


import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;




//This animated wallpaper draws many user selectable items... target, trackers, cursor, etc
public class TargetLiveWallpaper extends WallpaperService {
    public static final String SHARED_PREFS_NAME="target_lwp_settings";


    private static final String TAG = "TargetLiveWallpaper";

    private ArrayList<FlareData> flareList;
    
    
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
        return new TargetEngine();
    }

    class TargetEngine extends Engine 
        implements SharedPreferences.OnSharedPreferenceChangeListener {
 
        private final Handler mHandler = new Handler();

        ThreeDPoint [] mOriginalPoints;
        ThreeDPoint [] mRotatedPoints;
        ThreeDLine [] mLines;

        private final Paint mPaint = new Paint();
        private float mTouchX = -1;
        private float mTouchY = -1;

        private float mCenterX1;
        private float mCenterY1;

        private float mLeftTargetX;
        private float mLeftTargetY;

        private float mTopTargetX;
        private float mTopTargetY;
        
        private float mLastTouchX = 239;//indent initial display
        private float mLastTouchY = 239;
        

        private boolean leftOn = true;
        private boolean topOn = true;
        private boolean quadOn = true;
        private boolean pulse3dOn = true;
    	private String shape = "diamond";
        
        private boolean discOn = true;
        private int discStyle = 1;
        private boolean pulseOn = true;
        private int spacingOfRings = 15;
        private int numberOfRings = 16;
        private int mPulseN = 0;
        
        private boolean flareOn = true;
        
        private boolean mouseOn = false;
        String cursor = "debianswirl";//cursor_typenames
        private Bitmap mCursorImage;

        //        private boolean conkeyOn = false;
        
        private final Runnable mDrawCube = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private boolean mVisible;
        private SharedPreferences mPrefs;

        TargetEngine() {
            // Create a Paint to draw the lines for our 3D shape
            final Paint paint = mPaint;
            paint.setColor(0xffffffff);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(2);//increased stroke... better thin
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE); 
            
            mPrefs = TargetLiveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);
        }

       
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        	//3d targets
        	shape = prefs.getString("target_shape", "diamond");
            quadOn = prefs.getBoolean("target_quad_on", true);
            leftOn = prefs.getBoolean("target_left_on", true);
            topOn = prefs.getBoolean("target_top_on", true);
            pulse3dOn =  prefs.getBoolean("target_dpulse_on", true);
            
            //rotating targets
            discOn = prefs.getBoolean("target_disc_on", true);
            discStyle = Integer.valueOf(prefs.getString("target_disc_type", "1"));
            
            //static targets
            mouseOn = prefs.getBoolean("target_mouse_on", false);
            cursor = prefs.getString("cursor_type", "debianswirl");//cursor_typenames
            
            //pulse settings:
            pulseOn = prefs.getBoolean("target_pulse_on", true);
            spacingOfRings = Integer.valueOf(prefs.getString("target_pulse_width", "15"));
            numberOfRings = Integer.valueOf(prefs.getString("target_pulse_number", "16"));
            
            //flare settings:
            flareOn = prefs.getBoolean("target_flare_on", true);

           // read the 3D model from the resource
            readModel(shape);
            
            //from sdk... think i get it now
            Resources myResources;
            myResources = getBaseContext().getResources();
            mCursorImage = BitmapFactory.decodeResource(myResources, getResources().getIdentifier( getPackageName() + ":drawable/"+cursor, null, null));

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
           
            //init flare list
            flareList = new ArrayList<FlareData>(1);
            
            //maybe just if null??? .. using mPrefs now... hopefully this will be resolved now
            SharedPreferences prefs = mPrefs;            
        	//3d targets
        	shape = prefs.getString("target_shape", "diamond");
            quadOn = prefs.getBoolean("target_quad_on", true);
            leftOn = prefs.getBoolean("target_left_on", true);
            topOn = prefs.getBoolean("target_top_on", true);
            pulse3dOn =  prefs.getBoolean("target_dpulse_on", true);
            
            //rotating targets
            discOn = prefs.getBoolean("target_disc_on", true);
            discStyle = Integer.valueOf(prefs.getString("target_disc_type", "1"));
            
            //static targets
            mouseOn = prefs.getBoolean("target_mouse_on", false);
            cursor = prefs.getString("cursor_type", "debianswirl");//cursor_typenames

            //pulse settings:
            pulseOn = prefs.getBoolean("target_pulse_on", true);
            spacingOfRings = Integer.valueOf(prefs.getString("target_pulse_width", "15"));
            numberOfRings = Integer.valueOf(prefs.getString("target_pulse_number", "16"));
 

            //flare settings:
            flareOn = prefs.getBoolean("target_flare_on", true);
 
            
            
            
            
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
            mCenterX1 = width/2; 
            mCenterY1 = height/2;
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
                updateTouchPoint(c);
//DEBUG
//                drawConkey(c);
//Select modes                
                if(pulseOn)
                  {
                        drawTouchPointPulse(c);
                  }
                  if(discOn)
                  {
                      drawTouchDisc(c);
                  }   
                  
                    if(topOn)
                    {
                    	drawTopTarget(c);
                    }
                    if(leftOn)
                    {
                    	drawLeftTarget(c);
                    }

                    if(quadOn)
                    {
                    	drawQuadTarget(c);	
                    }
                   

                    if(mouseOn)
                    {
                    	drawStaticTarget(c);	
                    }

                    if(flareOn)
                    {
                    	drawTouchPointFlare(c);
                    }
                }
            } finally { 
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(mDrawCube);
            if (mVisible) {
                mHandler.postDelayed(mDrawCube, 1000 / 25);
            }
        }
        
        void drawQuadTarget(Canvas c) {
            c.save();
            c.drawColor(0x00000000);
            int oldColor = mPaint.getColor();
            mPaint.setColor(0xff00ff00);

            long now = SystemClock.elapsedRealtime();

            float xrot = (float) now/400;
            float yrot = (float) now/400;
//            rotateAndProjectPoints3(xrot, yrot);
            
            mPaint.setColor(0xFFFFFFFF-(0x000001 * ((int)now)/5 ));
            //mPaint.setColor(Color.argb(255,255-((int)now/5%200),0,0));       //0-255 or 0xAARRGGBB
            //argb(int alpha, int red, int green, int blue)
            
            mLeftTargetX = mLastTouchX;
            mLeftTargetY = mLastTouchY;
            int targeDistance = 190;
	        

	        rotateAndProjectPointsRight(xrot, 0);            
            c.translate(mLeftTargetX+targeDistance, mLeftTargetY);
	        drawLines(c);//RIGHT            

	        rotateAndProjectPointsLeft(xrot, 0);            
            c.translate(-targeDistance*2, 0);
	        drawLines(c);//left
	        

	        rotateAndProjectPointsBottom(0, yrot);            
            c.translate(targeDistance, targeDistance);
	        drawLines(c);//low
	        rotateAndProjectPointsTop(0, yrot);            
            c.translate(0, -targeDistance*2);
	        drawLines(c);//high
	        

            mPaint.setColor(oldColor);
          	c.restore(); 
       }
        
        void drawTopTarget(Canvas c) {
            c.save();
            c.drawColor(0x00000000);
            int oldColor = mPaint.getColor();
            long now = SystemClock.elapsedRealtime();
            
            if(!pulse3dOn)
            {
                //static
            	mPaint.setColor(0xff00ff00);
            }else{
                //pulse
                mPaint.setColor(Color.argb(255,255-((int)now/5%200),0,0));     	
            }
            float xrot = (float)0;
            float yrot = (float)now/400;
            rotateAndProjectPointsTop(xrot, yrot);

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
            rotateAndProjectPointsLeft(xrot, yrot);


            if(!pulse3dOn)
            {
                //static
            	mPaint.setColor(0xff00ff00);
            }else{
                //pulse
                mPaint.setColor(Color.argb(255,255-((int)now/5%200),0,0));     	
            }
            
            
            mLeftTargetX = 20;
            mLeftTargetY = mLastTouchY;
	        c.translate(mLeftTargetX, mLeftTargetY);
	        drawLines(c);
            mPaint.setColor(oldColor);
          	c.restore();

       }

        
        
        void rotateAndProjectPointsTop(float xrot, float yrot) {
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
        void rotateAndProjectPointsBottom(float xrot, float yrot) {
            int n = mOriginalPoints.length;
//            xrot-=1.0f;
            for (int i = 0; i < n; i++) {
                // rotation around X-axis
                ThreeDPoint p = mOriginalPoints[i];
                float x = p.x;
                float y = 1-p.y;
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
        void rotateAndProjectPointsLeft(float xrot, float yrot) {
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
        void rotateAndProjectPointsRight(float xrot, float yrot) {
            int n = mOriginalPoints.length;
            for (int i = 0; i < n; i++) {
                // rotation around X-axis
                ThreeDPoint p = mOriginalPoints[i];
                float y = p.x;
                float x = 1-p.y;
                float z = 1-p.z;
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
        void rotateAndProjectPoints3(float xrot, float yrot) {
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

        
        
        
        void updateTouchPoint(Canvas c) {
        	   if (mTouchX >=0 && mTouchY >= 0) {                

	        	// get relative dirs
	                float diffX = mTouchX - mLastTouchX;
	                float diffY = mTouchY - mLastTouchY;
	                mCenterY1 = mCenterY1 + diffY;
	                mCenterX1 = mCenterX1 + diffX;
	                
	                //store for next
	                mLastTouchX = mTouchX;
	                mLastTouchY = mTouchY;            
        	   }
        	   //pre draw canvas clearing.... do not remove (again).
        	   c.drawColor(0xff000000);   
        }
        

        
        void drawTouchPointPulse(Canvas c) {
            for(int i = 0; i <= numberOfRings; i++)
            {// want to do configurable color one day...
                mPaint.setColor(0xffff0000-(0x09000000 * ((i-mPulseN)%numberOfRings) ));
                c.drawCircle(mLastTouchX, mLastTouchY, spacingOfRings * i, mPaint);
            }
// conditional to finish animation.
            if (mPulseN > 0)
            {
            	--mPulseN;
            }
            
            if (mTouchX >=0 && mTouchY >= 0) {         
            	if(mPulseN <= 0)
            		mPulseN = numberOfRings;
            }
        	

        }//pulse


        void drawTouchPointFlare(Canvas c) {

			Random rng = new Random();
//            Log.d(TAG, "Start Flare");
        	//add after flare...viral...crack
//new flare 
            if (mTouchX >=0 && mTouchY >= 0) {
                //max 5 flares
            	if (flareList.size() < 5) {
            		//1 second delay
                	if (flareList.size() > 0 ) {
                		if(flareList.get(flareList.size()-1).getTime() > 10)
                		{
                			flareList.add(new FlareData(mTouchX, mTouchY, rng.nextInt(20)-10, 0xFF00FF00, 0xFFFF0000, 0, 30 + rng.nextInt(20), 20 + rng.nextInt(10), 10+rng.nextInt(20)));
                		}
                	}else
                	{
                		flareList.add(new FlareData(mTouchX, mTouchY, rng.nextInt(20)-10, 0xFF00FF00, 0xFFFF0000, 0, 30 + rng.nextInt(20), 20 + rng.nextInt(10), 10+rng.nextInt(20)));
                			
                	}
                }
            }

        	//flare list:   now incorporating move old flare/virs
        		FlareData thisFlare;
				for (Iterator<FlareData> fIterator = flareList.iterator(); fIterator.hasNext();) 
				{
					thisFlare = fIterator.next();

					//move  old flare/virs
					if (thisFlare.getTime() < thisFlare.getStage1Time()) 
					{
						thisFlare.setY((float) (thisFlare.getY()
								+ Math.sin(SystemClock.elapsedRealtime()) - 0.2 * thisFlare.getTime() ));
						thisFlare.setX((float) thisFlare.getX()
								+ (float) Math.sin(SystemClock.elapsedRealtime()) + (( thisFlare.getTilt() * thisFlare.getTime())/80)  );
					} 
//					else {
//					}
//						if (thisFlare.getTime() > thisFlare.getStage1Time()*3) 
//						{
//							//
//						}
						
					thisFlare.setTime(thisFlare.getTime() + 1);

					//render
					if (thisFlare.getTime() < thisFlare.getStage1Time()) {
						//stage 1
//			            Log.d(TAG, "S1");
//						mPaint.setColor(0xFF00FF00);
						mPaint.setColor(0xFFDDDD00);
//						c.drawCircle(thisFlare.getX(), thisFlare.getY(), 3, mPaint);
						c.drawRoundRect(new RectF(thisFlare.getX(), thisFlare.getY(), thisFlare.getX()+3, thisFlare.getY()+9), 0, 0, mPaint);
					} else {
						if (thisFlare.getTime() < thisFlare.getStage1Time() + thisFlare.getStage2Time()) {
							//stage 2
//				            Log.d(TAG, "S2");
							
							if(thisFlare.getExplosionCount() == 0) //new explosion
							{
								thisFlare.setExplosionCount(4 + rng.nextInt(16));
								Log.d(TAG, "\n\nFlare count: " + thisFlare.getExplosionCount());

								for(int i = thisFlare.getExplosionCount(); i > 0; i--)
								{	
									Log.d(TAG, "   -flare" + i +": " + thisFlare.getExplosionCount());
									Log.d(TAG, "        i/count -" + (double)i/(double)thisFlare.getExplosionCount() );
									Log.d(TAG, "        i/count*360 -" + (double)(i/thisFlare.getExplosionCount())*360.0 );
									
								}
							}
							
							mPaint.setColor(thisFlare.getColor1());
							thisFlare.incrementExplosionRadius();
							
							for(int i = thisFlare.getExplosionCount(); i > 0; i--)
							{
								c.drawCircle(
										(float)(thisFlare.getExplosionRadius() * (Math.cos((double)((double)i/(double)thisFlare.getExplosionCount())*360.0)) + thisFlare.getX()),
										(float)(thisFlare.getExplosionRadius() * (Math.sin((double)((double)i/(double)thisFlare.getExplosionCount())*360.0)) + thisFlare.getY()),
										2,
										mPaint);
//								c.drawCircle(
//										(float) (thisFlare.getX() + (Math.cos( (i/thisFlare.getExplosionCount())*360 ) * thisFlare.getExplosionRadius())),
//										(float) (thisFlare.getY() + (Math.sin((i/thisFlare.getExplosionCount())*360) * thisFlare.getExplosionRadius())),
//										2,
//										mPaint);
							}
							
//							c.drawCircle(
//									thisFlare.getX() - 10,
//									thisFlare.getY() - 10,
//									1 * (thisFlare.getTime() - thisFlare.getStage1Time() ),
//									mPaint);
//							c.drawCircle(
//									thisFlare.getX() - 10,
//									thisFlare.getY() + 10,
//									1 * (thisFlare.getTime() - thisFlare.getStage1Time()),
//									mPaint);
//							c.drawCircle(
//									thisFlare.getX() + 10,
//									thisFlare.getY() - 10,
//									1 * (thisFlare.getTime() - thisFlare.getStage1Time()),
//									mPaint);
//							c.drawCircle(
//									thisFlare.getX() + 10,
//									thisFlare.getY() + 10,
//									1 * (thisFlare.getTime() - thisFlare.getStage1Time()),
//									mPaint);
						}else
						{
							if(thisFlare.getTime() < thisFlare.getStage1Time() + thisFlare.getStage2Time() + thisFlare.getStage3Time())
							{
								//stage 3
//					            Log.d(TAG, "S3");
								mPaint.setColor(thisFlare.getColor2());
								c.drawCircle(
										thisFlare.getX() - 10-5,
										thisFlare.getY() - 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() - 10-5,
										thisFlare.getY() - 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() - 10+5,
										thisFlare.getY() - 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() - 10+5,
										thisFlare.getY() - 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								
								c.drawCircle(
										thisFlare.getX() - 10-5,
										thisFlare.getY() + 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() - 10-5,
										thisFlare.getY() + 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() - 10+5,
										thisFlare.getY() + 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() - 10+5,
										thisFlare.getY() + 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								
								c.drawCircle(
										thisFlare.getX() + 10-5,
										thisFlare.getY() - 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() + 10-5,
										thisFlare.getY() - 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() + 10+5,
										thisFlare.getY() - 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() + 10+5,
										thisFlare.getY() - 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								
								c.drawCircle(
										thisFlare.getX() + 10-5,
										thisFlare.getY() + 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() + 10-5,
										thisFlare.getY() + 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() + 10+5,
										thisFlare.getY() + 10-5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								c.drawCircle(
										thisFlare.getX() + 10+5,
										thisFlare.getY() + 10+5,
										1 * (thisFlare.getTime() - (thisFlare.getStage1Time() + thisFlare.getStage2Time())),
										mPaint);
								
								
							}else
							{
								//expiring:   ... working :)
								fIterator.remove();
								
							}
						}

					}//else
				}//for

        }//flare
        
        void drawStaticTarget(Canvas c) {
                //what about icons??? duhh... removing cursors and centering target
            c.drawBitmap(mCursorImage, mLastTouchX - (mCursorImage.getHeight()/2), mLastTouchY - (mCursorImage.getWidth()/2), mPaint);
        }

        void drawTouchDisc(Canvas c) {
        	//case: discStyle.... rgb the hard way
            int oldColor = mPaint.getColor();

            int startRings = 48;
            int widthOfRings = 8;
            int widthOfOutline = 1;
            for(int i = startRings-widthOfOutline; i < startRings+widthOfRings+widthOfOutline; i++)
            {
//                mPaint.setColor(0xffff0000-(0x09000000 * ((i-mPulseN)%numberOfRings) ));
//                c.drawCircle(mLastTouchX, mLastTouchY, 8 + i, mPaint);

            	float rotationSpeed = 0.1f;
            	float sweepAngle = 45.00f;
            	if(i <= startRings || i >= startRings+widthOfRings)
            	{//outline
                    mPaint.setARGB(255, 255, 255, 255);
//                	rotationSpeed = 0.05f;
//                	sweepAngle = 45.00f+(2*widthOfOutline);
            	}else
            	{//red
                    mPaint.setARGB(255, 255, 0, 0);
//                    rotationSpeed = 0.1f;
//                	sweepAngle = 45.00f;
                    switch(discStyle)
                    {
                    case 1:
                    	mPaint.setARGB(255, 255, 0, 0);
                    	break;
                    case 2:
                    	mPaint.setARGB(255, 0, 255, 0);
                    	break;
                    case 3:
                    	mPaint.setARGB(255, 0, 0, 255);
                    	break;
                    default:
                    	break;
                    }
            	

            	}
            	boolean useCenter = false;
                //inner.. counter
                c.drawArc(new RectF(mLastTouchX - (i-30),mLastTouchY - (i-30), mLastTouchX + (i-30), mLastTouchY + (i-30)), 360-(rotationSpeed*SystemClock.uptimeMillis()*2%360), sweepAngle*4, useCenter, mPaint);
            	//inner
                c.drawArc(new RectF(mLastTouchX - i,mLastTouchY - i, mLastTouchX + i, mLastTouchY + i), rotationSpeed*SystemClock.uptimeMillis()%360, sweepAngle, useCenter, mPaint);
                //mid
                c.drawArc(new RectF(mLastTouchX - i,mLastTouchY - i, mLastTouchX + i, mLastTouchY + i), (rotationSpeed*SystemClock.uptimeMillis()+180)%360, sweepAngle*2, useCenter, mPaint);
                //outer.. counter
                c.drawArc(new RectF(mLastTouchX - (i+30),mLastTouchY - (i+30), mLastTouchX + (i+30), mLastTouchY + (i+30)), 360-(rotationSpeed*SystemClock.uptimeMillis()*2%360), sweepAngle*4, useCenter, mPaint);
            }
            mPaint.setColor(oldColor);
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
//            c.drawText("This is preview: " + this.isPreview(), 		5, 180, mPaint);
//            c.drawText("This is viible: " + this.isVisible(), 		5, 200, mPaint);
//            c.drawText("This is viible: " , 		5, 220, mPaint);
            
//        	c.drawText("color = " + mPaint.getColor(), 		5, 590, mPaint);
//        	c.drawText("mTouchX = " + mTouchX, 		5, 610, mPaint);
//            c.drawText("mTouchY = " + mTouchY, 		5, 630, mPaint);
//            c.drawText("mCenterX1= " + mCenterX1, 5, 690, mPaint);
//            c.drawText("mCenterY1= " + mCenterY1, 5, 710, mPaint);
            c.drawText("flareCount= " + flareList.size(), 5, 210, mPaint);
            
            mPaint.setColor(oldColor);

//          c.drawText("diffX = " + diffX, 5, 650, mPaint);
//          c.drawText("diffY = " + diffY, 5, 670, mPaint); //oops... this should be in conkey if anywhere.
        }
        
    }
    
    
    
}