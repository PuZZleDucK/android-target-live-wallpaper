


package com.puzzleduck.targetLiveWallpaper;


import java.net.URL;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.TableRow;



//This is the "app" part of the LWP
public class TargetLiveActivity extends Activity {
    public static final String SHARED_PREFS_NAME="target_lwp_settings";


    private static final String TAG = "TargetLiveActivity";


	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		return super.dispatchTouchEvent(ev);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	
//		this.getApplicationContext().setWallpaper(data)
		
	
		this.setContentView(R.layout.activity);

		WebView thisWebView = (WebView)findViewById(R.id.webView1);
		thisWebView.getSettings().setJavaScriptEnabled(true);
	    thisWebView.loadUrl("www.google.com.au/search?q=%22target+live+wallpaper%22+%22puzzleduck+Industries%22");
//		thisWebView.reload();
		
	}//onCreate

	//     * Invoked when the Activity loses user focus.
	@Override
	protected void onPause() {
		super.onPause();
		//        mLunarView.getThread().pause(); // pause game when Activity pauses
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return super.onMenuItemSelected(featureId, item);
	}


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub

		

//		this.setContentView(R.layout.activity);

		WebView thisWebView = (WebView)findViewById(R.id.webView1);
//		thisWebView.reload();		
	    thisWebView.loadUrl("www.google.com.au/search?q=%22target+live+wallpaper%22+%22puzzleduck+Industries%22");
		
		
		return super.onTouchEvent(event);


	
	}


	@Override
	public Drawable peekWallpaper() {
		// TODO Auto-generated method stub
		return super.peekWallpaper();
	}

    
}//class    
