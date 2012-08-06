/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2012 by Michel Marti

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.ecmdroid;


import org.ecmdroid.activity.AboutActivity;
import org.ecmdroid.activity.ActiveTestsActivity;
import org.ecmdroid.activity.DataChannelActivity;
import org.ecmdroid.activity.EEPROMActivity;
import org.ecmdroid.activity.PrefsActivity;
import org.ecmdroid.activity.SetupActivity;
import org.ecmdroid.activity.TroubleCodeActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public abstract class Utils
{
	private static final String TAG = "Utils";

	public static boolean createOptionsMenu(Activity activity, Menu menu) {
		MenuInflater mi = activity.getMenuInflater();
		mi.inflate(R.menu.main, menu);
		return true;
	}

	public static void prepareOptionsMenu(Activity activity, Menu menu) {
		Class<? extends Activity> cls = activity.getClass();
		int id = 0;
		if (cls == MainActivity.class) {
			id = R.id.main;
		} else if (cls == DataChannelActivity.class) {
			id = R.id.data;
		} else if (cls == LogActivity.class) {
			id = R.id.log;
		} else if (cls == TroubleCodeActivity.class) {
			id = R.id.troublecodes;
		} else if (cls == EEPROMActivity.class) {
			id = R.id.eeprom;
		} else if (cls == SetupActivity.class) {
			id = R.id.setup;
		} else if (cls == AboutActivity.class) {
			id = R.id.about;
		}
		if (id > 0) {
			MenuItem item  = menu.findItem(id);
			if (item != null) {
				//item.setVisible(false);
				item.setEnabled(false);
			}
		}
	}

	public static boolean handleOptionsItemSelected(Activity activity, MenuItem item) {
		Log.v(TAG, "Menu " + item.getTitle() + " selected.");
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.tests:
			if (activity.getClass() != ActiveTestsActivity.class) {
				intent = new Intent(activity, ActiveTestsActivity.class);
			}
			break;
		case R.id.main:
			if (activity.getClass() != MainActivity.class) {
				intent = new Intent(activity, MainActivity.class);
			}
			break;
		case R.id.data:
			if (activity.getClass() != DataChannelActivity.class) {
				intent = new Intent(activity, DataChannelActivity.class);
			}
			break;
		case R.id.log:
			if (activity.getClass() != LogActivity.class) {
				intent = new Intent(activity, LogActivity.class);
			}
			break;
		case R.id.troublecodes:
			if (activity.getClass() != TroubleCodeActivity.class) {
				intent = new Intent(activity, TroubleCodeActivity.class);
			}
			break;
		case R.id.eeprom:
			if (activity.getClass() != EEPROMActivity.class) {
				intent = new Intent(activity, EEPROMActivity.class);
			}
			break;
		case R.id.setup:
			if (activity.getClass() != SetupActivity.class) {
				intent = new Intent(activity, SetupActivity.class);
			}
			break;
		case R.id.prefs:
			if (activity.getClass() != PrefsActivity.class) {
				intent = new Intent(activity, PrefsActivity.class);
			}
			break;
		case R.id.about:
			if (activity.getClass() != AboutActivity.class) {
				intent = new Intent(activity, AboutActivity.class);
			}
			break;
		}
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			return true;
		}
		return false;
	}

	public static String hexdump(byte[] data, int offset, int len) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; (i  + offset) < Math.min(data.length, len); i++) {
			sb.append(":").append(String.format("%02X", data[i+offset]&0xFF));
		}
		return sb.length() > 0 ? sb.substring(1) : "<empty>";
	}

	public static String hexdump(byte[] bytes) {
		return hexdump(bytes, 0, bytes.length);
	}

	public static boolean isEmptyString(Object str) {
		return (str == null || str.toString().trim().length() == 0);
	}

	public static String toHex(int i, int... width) {
		String fmt = "%0" + (width.length == 1 ? width[0] : 2) + "X";
		return String.format(fmt, i);
	}

	public static int freezeOrientation(Activity context) {
		int result = context.getRequestedOrientation();

		switch (context.getResources().getConfiguration().orientation)
		{
		case Configuration.ORIENTATION_LANDSCAPE:
			context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		case Configuration.ORIENTATION_PORTRAIT:
			context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		default:
			context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}
		return result;
	}


}
