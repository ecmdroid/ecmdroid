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
package org.ecmdroid.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import org.ecmdroid.Constants;
import org.ecmdroid.ECM;
import org.ecmdroid.EEPROM;
import org.ecmdroid.EEPROMAdapter;
import org.ecmdroid.R;
import org.ecmdroid.Utils;
import org.ecmdroid.Variable;
import org.ecmdroid.task.BurnTask;
import org.ecmdroid.task.FetchTask;
import org.ecmdroid.task.SaveTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class EEPROMActivity extends Activity {

	public static final String ACTION_BURN = "BURN";

	private static final int COLS = 5;
	@SuppressWarnings("unused")
	private static final String TAG = "EEPROM";

	private ECM ecm = ECM.getInstance(this);
	private TextView offsetHex, offsetDec;
	private TextView byteValHex, byteValDec;
	private TextView hiShortHex, hiShortDec;
	private TextView loShortHex, loShortDec;
	private TextView cellInfo;
	private EEPROMAdapter adapter;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.eeprom_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
		menu.findItem(R.id.fetch).setEnabled(ecm.isConnected());
		menu.findItem(R.id.burn).setEnabled(ecm.isConnected() && pm.getBoolean("enable_burn_eeprom", Boolean.FALSE));
		menu.findItem(R.id.save).setEnabled(ecm.isEepromRead());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.fetch:
			new FetchTask(this).start();
			break;
		case R.id.burn:
			new BurnTask(this).start();
			break;
		case R.id.save:
			new SaveTask(this, ecm.getEEPROM()).start();
			break;
		case R.id.load:
			final StringBuilder result = new StringBuilder();
			Dialog dlg = createLoadDialog(result);
			dlg.setOnDismissListener(new OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					if (result.length() > 0) {
						if (ecm.isEepromRead() && ecm.getEEPROM().isTouched()) {
							AlertDialog.Builder builder = new AlertDialog.Builder(EEPROMActivity.this);
							builder.setTitle(R.string.load_eeprom)
							.setMessage(R.string.overwrite_changes)
							.setCancelable(true)
							.setNegativeButton(android.R.string.cancel, null)
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									loadEEPROM(new File(result.toString()));
								}
							}).show();
						} else {
							loadEEPROM(new File(result.toString()));
						}
					}
				}
			});
			dlg.show();
			break;
		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eeprom);
		offsetHex  = (TextView) findViewById(R.id.offsetHex);
		offsetDec  = (TextView) findViewById(R.id.offsetDec);
		byteValHex = (TextView) findViewById(R.id.byteValHex);
		byteValDec = (TextView) findViewById(R.id.byteValDec);
		hiShortHex = (TextView) findViewById(R.id.hiShortHex);
		hiShortDec = (TextView) findViewById(R.id.hiShortDec);
		loShortHex = (TextView) findViewById(R.id.loShortHex);
		loShortDec = (TextView) findViewById(R.id.loShortDec);
		cellInfo   = (TextView) findViewById(R.id.cellInfo);

		GridView gridview = (GridView) findViewById(R.id.eepromGrid);
		adapter = new EEPROMAdapter(this, ecm.getEEPROM(), COLS);
		gridview.setAdapter(adapter);
		// TODO: Chose a nice drawable for currently selected cell
		//gridview.setSelector(android.R.drawable.edit_text);

		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				if (pos % COLS != 0) {
					int offset = pos - (pos / COLS + 1);
					Variable var = ecm.getEEPROMValueNearOffset(offset);
					if ((var.getOffset() + var.getSize() - 1) < offset) {
						// Unknown area
						var = null;
					}

					cellInfo.setText(var == null ? "" : var.getLabel() != null ? var.getLabel() : var.getName());
					cellInfo.setEnabled(var != null && var.getOffset() == offset);
					byte[] bytes = ecm.getEEPROM().getBytes();
					int val = bytes[offset] & 0xFF;
					offsetHex.setText(Utils.toHex(offset, 3));
					offsetDec.setText(Integer.toString(offset));
					byteValHex.setText(Utils.toHex(val, 2));
					byteValDec.setText(Integer.toString(val));
					if (offset == 0) {
						hiShortHex.setText("");
						hiShortDec.setText("");
					} else {
						int hival = val << 8 | (bytes[offset -1] & 0xff);
						hiShortHex.setText(Utils.toHex(hival, 4));
						hiShortDec.setText(Integer.toString(hival));
					}

					if (offset + 1 >= bytes.length) {
						loShortHex.setText("");
						loShortDec.setText("");
					} else {
						int loval = (bytes[offset+1] & 0xff) << 8 | val;
						loShortHex.setText(Utils.toHex(loval, 4));
						loShortDec.setText(Integer.toString(loval));
					}
				}
			}
		});

		if (ACTION_BURN.equals(getIntent().getAction())) {
			new BurnTask(this).start();
		}
	}

	private Dialog createLoadDialog(final StringBuilder result) {
		final File dir = getApplicationContext().getExternalFilesDir(getString(R.string.eeprom_dir));
		final String[] files = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(Constants.EEPROM_FILE_SUFFIX);
			}
		});
		Arrays.sort(files);
		Builder builder = new Builder(this);
		builder.setItems(files, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String selected = files[which];
				if (result != null) {
					result.setLength(0);
					result.append(new File(dir, selected).getPath());
				}
			}
		});
		builder.setTitle(R.string.load_eeprom);
		return builder.create();
	}

	private void loadEEPROM(File file)
	{
		try {
			FileInputStream in = new FileInputStream(file);
			EEPROM eeprom = EEPROM.load(this, ecm.getId(), in);
			if (ecm.isConnected() && !eeprom.getId().equals(ecm.getId())) {
				throw new IOException(getString(R.string.incompatible_version_disconnect_first));
			}
			ecm.setEEPROM(eeprom);
			Toast.makeText(EEPROMActivity.this, "EEPROM loaded sucessfully", Toast.LENGTH_LONG).show();
			GridView gridview = (GridView) findViewById(R.id.eepromGrid);
			adapter = new EEPROMAdapter(EEPROMActivity.this, ecm.getEEPROM(), COLS);
			gridview.setAdapter(adapter);
		} catch (IOException e) {
			Toast.makeText(EEPROMActivity.this, "Unable to load EEPROM. " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
