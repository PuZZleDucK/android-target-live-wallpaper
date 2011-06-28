/*
 * Copyright (C) 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package puzzleduck.targetLiveWallpaper;

//import de.devmil.common.ui.color.ColorSelectorDialog;
//import afzkl.development.mColorPicker.ColorPickerActivity;
import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class TargetLiveWallpaperSettings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle newBundle) {
        super.onCreate(newBundle);
        getPreferenceManager().setSharedPreferencesName(TargetLiveWallpaper.SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.target_lwp_settings);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override 
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
    	
//Action: 
//    	org.openintents.action.PICK_COLOR 
//      startActivityForResult() to launch this color picker with a default color specified in the integer 
//      Intent extra "org.openintents.extra.COLOR". The chosen color will be returned in an Intent the 
//      same way
//    	int x = 0;
//    	Intent i = new Intent("org.openintents.action.PICK_COLOR");
//    	i.putExtra("org.openintents.extra.COLOR", x);
//      startActivityForResult(i, x);
    	
    	
//    	Object initialColor = 0;
//		AmbilWarnaDialog dialog = AmbilWarnaDialog(this, initialColor, new OnAmbilWarnaListener() {
////            @Override
//            public void onOk(AmbilWarnaDialog dialog, int color) {
//                    // color is the color selected by the user.
//            }
//                    
////            @Override
//            public void onCancel(AmbilWarnaDialog dialog) {
//                    // cancel was selected by the user
//            }
//    });
//
//    dialog.show();
    	
    	
    	
    }

//	private AmbilWarnaDialog AmbilWarnaDialog(
//			TargetLiveWallpaperSettings targetLiveWallpaperSettings,
//			Object initialColor, OnAmbilWarnaListener onAmbilWarnaListener) {
//		// TODO Auto-generated method stub
//		return null;
//	}

    

    
//
//	private final static int ACTIVITY_COLOR_PICKER_REQUEST_CODE = 1000;
////	@Override
//	public boolean onPreferenceClick(Preference preference) throws NameNotFoundException {
//
//		final SharedPreferences prefs = PreferenceManager
//				.getDefaultSharedPreferences(TargetLiveWallpaperSettings.this);
//		String key = preference.getKey();
//		
//		//in ADB i get nothing
//		//should get:
//		//I/ActivityManager(  582): Starting: Intent { cmp=afzkl.development.mColorPicker/.ColorPickerActivity (has extras) } from pid 3553
//		// trying another approach
//		if (key.equals("activity")) {
//			Intent i = new Intent(this, ColorPickerActivity.class);
//			i.putExtra(ColorPickerActivity.INTENT_DATA_INITIAL_COLOR, prefs
//					.getInt("activity", 0xff000000));
//			startActivityForResult(i, ACTIVITY_COLOR_PICKER_REQUEST_CODE);
//			return true;
//		}
//		
//		return false;
//	}

    
    
}
