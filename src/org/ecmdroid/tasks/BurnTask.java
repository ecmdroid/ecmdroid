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
package org.ecmdroid.tasks;

import java.io.IOException;

import org.ecmdroid.ECM;
import org.ecmdroid.EEPROM;
import org.ecmdroid.EEPROM.Page;
import org.ecmdroid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

/**
 * Asynchronous Task for burning current EEPROM data
 */
public class BurnTask extends ProgressDialogTask
{
	private static final String TAG = "BurnTask";
	private ECM ecm;

	public BurnTask(Activity context) {
		super(context, context.getString(R.string.burn_eeprom));
		ecm = ECM.getInstance(context);
	}

	public void start() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		AlertDialog alert = builder.setTitle(R.string.burn_eeprom)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.burn_warning)
				.setCancelable(true)
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						execute();
					}
				}).create();
		alert.show();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// Check if EEPROM is compatible...
		String id = ecm.getId();
		String version = null;
		try {
			version = ecm.readVersion();
		} catch (IOException e) {
		}
		Log.d(TAG, "Current ID: " + id + ", connected ECM version: " + version);
		if (id == null || version == null || !version.startsWith(id)) {
			Log.w(TAG, "EEPROM ID ('" + id + "') does not match ECM Version ('" + version + "').");
			cancel(true);
		}
	}
	@Override
	protected Exception doInBackground(Void... params) {
		try {
			EEPROM eeprom = ecm.getEEPROM();
			if (ecm.getEEPROM() != null) {
				int i = 0;
				int count = eeprom.getPageCount();
				for (Page pg : ecm.getEEPROM().getPages()) {
					if (pg.nr() == 0) {
						// TODO: We don't handle page 0 for now...
						count--;
						continue;
					}
					publishProgress(context.getString(R.string.burn_progress, ++i, count));
					ecm.writeEEPromPage(pg);
				}
			}
		} catch (Exception e) {
			return e;
		}
		return null;
	}

	@Override
	protected void onPostExecute(Exception result) {
		super.onPostExecute(result);
		if (result == null) {
			ecm.getEEPROM().saved();
			Toast.makeText(context, R.string.burn_success, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		Toast.makeText(context, "Error: ECM ID does not match current EEPROM ID!", Toast.LENGTH_LONG).show();
	}
}
