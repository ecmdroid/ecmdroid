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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

	private static final String TAG = "DBHelper";
	private static File DB_PATH = new File("/data/data/org.ecmdroid/databases/");
	private static String DB_NAME = "ecmdroid";

	public DBHelper(Context context) {
		super(context, DB_NAME, null, 1);
	}

	public boolean isDbInstalled() {
		File db = new File(DB_PATH, DB_NAME);
		return db.exists();
	}
	public void installDB(Context context) throws IOException {
		// File db = new File(context.getDir("Data", 0), DB_NAME);
		File db = new File(DB_PATH, DB_NAME);
		DB_PATH.mkdirs();
		Log.d(TAG, "Checking if file '" + db.getAbsolutePath() + "' exists...");
		if (!db.exists()) {
			long now = System.currentTimeMillis();
			Log.i(TAG,"Installing Database...");
			AssetManager assets = context.getAssets();
			InputStream in = assets.open(DB_NAME + ".db");
			FileOutputStream out = new FileOutputStream(db);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}
			in.close();
			out.flush();
			out.close();
			Log.d(TAG, "Database installed in " + (System.currentTimeMillis() - now) + "ms.");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate...");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
		Log.d(TAG, "onUpgrade...");
	}

}
