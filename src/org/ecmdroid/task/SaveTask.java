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
package org.ecmdroid.task;

import java.io.File;
import java.io.FileOutputStream;

import org.ecmdroid.Constants;
import org.ecmdroid.EEPROM;
import org.ecmdroid.R;
import org.ecmdroid.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Task for saving EEPROM data
 */
public class SaveTask extends AsyncTask<Void, Void, Exception> {

	private EEPROM eeprom;
	private String filename;
	private File file;
	private Activity context;

	public SaveTask(Activity context, EEPROM eeprom) {
		this.context = context;
		this.eeprom = eeprom;
	}

	public void start() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.save_eeprom_as);
		builder.setIcon(android.R.drawable.ic_menu_save);
		if (!Utils.isExternalStorageAvailable()) {
			builder.setMessage(R.string.no_ext_storage);
			builder.create().show();
			return;
		}
		final EditText input = new EditText(context);
		String fn = String.format("%s_%s%s", eeprom.getId(), DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()), Constants.EEPROM_FILE_SUFFIX);
		input.setText(fn);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		builder.setView(input);
		builder.setPositiveButton(android.R.string.ok, null);
		builder.setNegativeButton(android.R.string.cancel, null);
		final AlertDialog d = builder.create();
		d.setOnShowListener(new OnShowListener() {
			public void onShow(DialogInterface dialog) {
				Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						filename = input.getText().toString();
						if (!Utils.isEmptyString(filename)) {
							file = new File(context.getApplication().getExternalFilesDir(context.getString(R.string.eeprom_dir)), filename);
							// Check if file already exists
							if (file.exists()) {
								Builder builder = new AlertDialog.Builder(context);
								builder.setIcon(android.R.drawable.ic_dialog_alert);
								builder.setMessage(context.getString(R.string.file_already_exists_overwrite, filename));
								builder.setNegativeButton(android.R.string.cancel, null);
								builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										d.dismiss();
										execute();
									}
								});
								builder.show();
							} else {
								d.dismiss();
								execute();
							}
						}
					}
				});
			}
		});
		d.show();
	}

	@Override
	protected Exception doInBackground(Void... params) {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			out.write(eeprom.getBytes());
		} catch (Exception e) {
			return e;
		} finally {
			try {
				if (out != null) out.close();
			} catch (Exception e2) {}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Exception result) {
		if (result == null) {
			eeprom.saved();
			Toast.makeText(context, R.string.eeprom_saved, Toast.LENGTH_LONG).show();
		}
	}
}
