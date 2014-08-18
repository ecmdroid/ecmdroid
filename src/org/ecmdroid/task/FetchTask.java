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
package org.ecmdroid.task;

import org.ecmdroid.ECM;
import org.ecmdroid.EEPROM;
import org.ecmdroid.EEPROM.Page;
import org.ecmdroid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * Asynchronous Task for fetching EEPROM data from the ECM.
 */
public class FetchTask extends ProgressDialogTask
{
	private ECM ecm;

	public FetchTask(Activity ctx) {
		super(ctx, ctx.getString(R.string.fetch_eeprom));
		ecm = ECM.getInstance(ctx);
	}

	/**
	 * Start fetching data, asking the user for confirmation if
	 * there are unsaved changes.
	 */
	public void start() {
		if (ecm.getEEPROM().isTouched()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			AlertDialog alert = builder.setTitle(R.string.fetch_eeprom)
					.setMessage(R.string.overwrite_changes)
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
		} else {
			execute();
		}
	}

	@Override
	protected Exception doInBackground(Void... params) {
		publishProgress(context.getString(R.string.reading_ecm_identification));
		try {
			publishProgress("Reading ECM Identification...");
			ecm.setupEEPROM();
			if (ecm.getEEPROM() != null) {
				EEPROM eeprom = ecm.getEEPROM();
				ecm.readRTData();
				int i = 1;
				for (Page pg : ecm.getEEPROM().getPages()) {
					publishProgress(context.getString(R.string.fetch_progress, i++, eeprom.getPageCount()));
					ecm.readEEPromPage(pg);
				}
				eeprom.setEepromRead(true);
			}
		} catch (Exception e) {
			return e;
		}
		return null;
	}
}
