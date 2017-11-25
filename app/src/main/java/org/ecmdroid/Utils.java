/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2012 by Michel Marti

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.ecmdroid;


import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Environment;
import android.view.Menu;

/**
 * Collection of various static utility methods.
 */
public abstract class Utils {
	private static final String TAG = "Utils";

	public static boolean createOptionsMenu(Activity activity, Menu menu) {
		// MenuInflater mi = activity.getMenuInflater();
		// mi.inflate(R.menu.main, menu);
		return true;
	}

	public static String hexdump(byte[] data, int offset, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; (i + offset) < Math.min(data.length, len); i++) {
			sb.append(":").append(String.format("%02X", data[i + offset] & 0xFF));
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

		switch (context.getResources().getConfiguration().orientation) {
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

	public static String getAppVersion(Context context) {
		String result = null;
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			result = context.getText(R.string.app_name) + " " + pInfo.versionName;
		} catch (NameNotFoundException e1) {
		}
		return result;
	}

	public static boolean isExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}
}
