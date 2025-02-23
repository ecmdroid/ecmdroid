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
package org.ecmdroid.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.ecmdroid.Constants;
import org.ecmdroid.ECM;
import org.ecmdroid.EEPROM;
import org.ecmdroid.EEPROMAdapter;
import org.ecmdroid.R;
import org.ecmdroid.Utils;
import org.ecmdroid.Variable;
import org.ecmdroid.activities.MainActivity;
import org.ecmdroid.fragments.CellEditorDialogFragment.CellEditorDialogListener;
import org.ecmdroid.task.BurnTask;
import org.ecmdroid.task.FetchTask;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EEPROMFragment extends Fragment implements CellEditorDialogListener {

	public static final String ACTION_BURN = "BURN";

	private static final int SAVE_FILE = 1;
	private static final int LOAD_FILE = 2;

	private static final int COLS = 5;
	@SuppressWarnings("unused")
	private static final String TAG = "EEPROM";

	private ECM ecm = ECM.getInstance(getActivity());
	private TextView offsetHex, offsetDec;
	private TextView byteValHex, byteValDec;
	private TextView hiShortHex, hiShortDec;
	private TextView loShortHex, loShortDec;
	private TextView cellInfo;
	private EEPROMAdapter adapter;

	@Override
	public void onCreate(Bundle args) {
		super.onCreate(args);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.eeprom_menu, menu);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.eeprom_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(getActivity());
		menu.findItem(R.id.fetch).setEnabled(ecm.isConnected());
		menu.findItem(R.id.burn).setEnabled(ecm.isConnected() && pm.getBoolean(Constants.PREFS_ENABLE_BURN, Boolean.FALSE));
		menu.findItem(R.id.save).setEnabled(ecm.isEepromRead());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.fetch:
				new FetchTask(getActivity()) {
					@Override
					protected void onPostExecute(Exception result) {
						super.onPostExecute(result);
						GridView gridview = (GridView) getView().findViewById(R.id.eepromGrid);
						adapter = new EEPROMAdapter(getActivity(), ecm.getEEPROM(), COLS);
						gridview.setAdapter(adapter);
					}
				}.start();
				break;
			case R.id.burn:
				new BurnTask(getActivity()).start();
				break;
			case R.id.save:
				String fn = String.format("%s_%s%s", ecm.getEEPROM().getId(), DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()), Constants.XPR_FILE_SUFFIX);
				this.saveFile(fn);
				break;
			case R.id.load:
				this.loadFile();
				break;
		}
		return true;
	}


	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle args) {
		super.onCreateView(inflater, container, args);
		View view = inflater.inflate(R.layout.eeprom, container, false);
		offsetHex = (TextView) view.findViewById(R.id.offsetHex);
		offsetDec = (TextView) view.findViewById(R.id.offsetDec);
		byteValHex = (TextView) view.findViewById(R.id.byteValHex);
		byteValDec = (TextView) view.findViewById(R.id.byteValDec);
		hiShortHex = (TextView) view.findViewById(R.id.hiShortHex);
		hiShortDec = (TextView) view.findViewById(R.id.hiShortDec);
		loShortHex = (TextView) view.findViewById(R.id.loShortHex);
		loShortDec = (TextView) view.findViewById(R.id.loShortDec);
		cellInfo = (TextView) view.findViewById(R.id.cellInfo);

		GridView gridview = (GridView) view.findViewById(R.id.eepromGrid);
		adapter = new EEPROMAdapter(getActivity(), ecm.getEEPROM(), COLS);
		gridview.setAdapter(adapter);
		// TODO: Chose a nice drawable for currently selected cell
		//gridview.setSelector(android.R.drawable.edit_text);

		gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				if (pos % COLS != 0) {
					int offset = pos - (pos / COLS + 1);
					showCellInfo(offset);
				}
			}
		});

		gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
				if (pos % COLS != 0) {
					int offset = pos - (pos / COLS + 1);
					showCellInfo(offset);
					byte value = ecm.getEEPROM().getBytes()[offset];
					CellEditorDialogFragment editor = CellEditorDialogFragment.newInstance(EEPROMFragment.this, offset, value);
					editor.show(getFragmentManager(), "EEPROMFragment");
				}
				return false;
			}
		});

		if (getArguments() != null && getArguments().getBoolean(ACTION_BURN)) {
			SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
			if (pm.getBoolean(Constants.PREFS_ENABLE_BURN, false)) {
				new BurnTask(getActivity()).start();
			} else {
				Toast.makeText(getActivity(), R.string.eeprom_burning_disabled_by_configuration, Toast.LENGTH_LONG).show();
			}
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		MainActivity activity = (MainActivity) getActivity();
		activity.setTitle(getString(R.string.eeprom));
		activity.updateConnectButton();
	}

	@Override
	public void onCellValueChanged(int offset, byte value) {
		byte oldValue = ecm.getEEPROM().getBytes()[offset];
		if (oldValue != value) {
			ecm.getEEPROM().getBytes()[offset] = value;
			ecm.getEEPROM().touch(offset, 1);
			GridView gridview = (GridView) getView().findViewById(R.id.eepromGrid);
			gridview.invalidateViews();
			showCellInfo(offset);
		}
	}


	private void loadEEPROM(long len, InputStream in) throws IOException {
		final String[] ids;
		try {
			// Determine ECM type
			ids = EEPROM.size2id(getActivity(), (int) len);
		} catch (IOException e) {
			Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
			in.close();
			return;
		}
		if (ids.length > 1) {
			Builder builder = new Builder(getActivity());
			builder.setItems(ids, (dialog, which) -> {
				String selected = ids[which];
				try {
					loadEEPROM(selected, in);
				} catch (IOException e) {
					Log.e(TAG, "Unable to load XPR", e);
				}
			});
			builder.setOnCancelListener(dialogInterface -> {
				try {
					Log.d(TAG, "ECM Type selection cancelled");
					in.close();
				} catch (IOException e) {
					Log.e(TAG, "Unable to close XPR", e);
				}
			});
			builder.setTitle(R.string.select_ecm_type).create().show();
		} else if (ids.length == 1) {
			loadEEPROM(ids[0], in);
		}
	}

	private void loadEEPROM(String id, InputStream in) throws IOException {
		try {
			EEPROM eeprom = EEPROM.load(getActivity(), id, in);
			if (ecm.isConnected() && !eeprom.getId().equals(ecm.getId())) {
				throw new IOException(getString(R.string.incompatible_version_disconnect_first, id));
			}
			ecm.setEEPROM(eeprom);
			Toast.makeText(getActivity(), R.string.eeprom_loaded_sucessfully, Toast.LENGTH_LONG).show();
			GridView gridview = (GridView) getView().findViewById(R.id.eepromGrid);
			adapter = new EEPROMAdapter(getActivity(), ecm.getEEPROM(), COLS);
			gridview.setAdapter(adapter);
		} catch (IOException e) {
			Toast.makeText(getActivity(), getString(R.string.unable_to_load_eeprom) + " " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		} finally {
			in.close();
		}
	}

	@SuppressLint("SetTextI18n")
	private void showCellInfo(int offset) {
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
			int hival = val << 8 | (bytes[offset - 1] & 0xff);
			hiShortHex.setText(Utils.toHex(hival, 4));
			hiShortDec.setText(Integer.toString(hival));
		}

		if (offset + 1 >= bytes.length) {
			loShortHex.setText("");
			loShortDec.setText("");
		} else {
			int loval = (bytes[offset + 1] & 0xff) << 8 | val;
			loShortHex.setText(Utils.toHex(loval, 4));
			loShortDec.setText(Integer.toString(loval));
		}
	}

	private void saveFile(String name) {
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/octet-stream");
		intent.putExtra(Intent.EXTRA_TITLE, name);
		startActivityForResult(intent, SAVE_FILE);
	}

	private void loadFile() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*"); // required for opening XPR files :-/
		startActivityForResult(intent, LOAD_FILE);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		Log.d(TAG, "ActivityResult, requestCode: " + requestCode + ", result: " + resultCode);
		Uri uri;
		if (requestCode == SAVE_FILE && resultCode == Activity.RESULT_OK) {
			if (resultData != null) {
				uri = resultData.getData();
				Log.i(TAG, "Document created, URI:" + uri);
				try {
					ParcelFileDescriptor pfd = getActivity().getContentResolver().
							openFileDescriptor(uri, "w");
					FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
					fileOutputStream.write(ecm.getEEPROM().getBytes());
					// Let the document provider know you're done by closing the stream.
					fileOutputStream.close();
					pfd.close();
					Log.i(TAG, "EEPROM saved");
				} catch (IOException ioe) {
					Log.e(TAG, "EEPROM save failed", ioe);
				}
			}
		} else if (requestCode == LOAD_FILE && resultCode == Activity.RESULT_OK) {
			if (resultData != null) {
				ParcelFileDescriptor pfd = null;
				try {
					uri = resultData.getData();
					pfd = getActivity().getContentResolver().openFileDescriptor(uri, "r");
					long size = pfd.getStatSize();
					InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
					loadEEPROM(size, inputStream);
				} catch (FileNotFoundException e) {
					Log.e(TAG, "EEPROM file Not found", e);
				} catch (IOException e) {
					Log.e(TAG, "IO Exception loading EEPROM", e);
				} finally {
					if (pfd != null) {
						try {
							pfd.close();
						} catch (IOException e) {
							Log.e(TAG, "Unable to close PFD");
						}
					}
				}
			}
		}
	}
}
