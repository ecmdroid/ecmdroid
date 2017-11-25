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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
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
import org.ecmdroid.fragments.CellEditorDialogFragment.CellEditorDialogListener;
import org.ecmdroid.task.BurnTask;
import org.ecmdroid.task.FetchTask;
import org.ecmdroid.task.SaveTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

public class EEPROMFragment extends Fragment implements CellEditorDialogListener {

	public static final String ACTION_BURN = "BURN";

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
				new SaveTask(getActivity(), ecm.getEEPROM()).start();
				break;
			case R.id.load:
				final StringBuilder result = new StringBuilder();
				Dialog dlg = createLoadDialog(result);
				dlg.setOnDismissListener(new OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						if (result.length() > 0) {
							if (ecm.isEepromRead() && ecm.getEEPROM().isTouched()) {
								AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				if (pos % COLS != 0) {
					int offset = pos - (pos / COLS + 1);
					showCellInfo(offset);
				}
			}
		});

		gridview.setOnItemLongClickListener(new OnItemLongClickListener() {
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
		getActivity().setTitle(getString(R.string.eeprom));
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

	private Dialog createLoadDialog(final StringBuilder result) {
		Builder builder = new Builder(getActivity());
		if (Utils.isExternalStorageAvailable()) {
			final File dir = getActivity().getApplicationContext().getExternalFilesDir(getString(R.string.eeprom_dir));
			final String[] files = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return filename.endsWith(Constants.XPR_FILE_SUFFIX) || filename.endsWith(Constants.EPR_FILE_SUFFIX);
				}
			});
			if (files != null && files.length > 0) {
				Arrays.sort(files);
				builder.setItems(files, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String selected = files[which];
						if (result != null) {
							result.setLength(0);
							result.append(new File(dir, selected).getPath());
						}
					}
				});
			} else {
				builder.setMessage(R.string.no_eeprom_dumps_present);
			}
		} else {
			builder.setMessage(R.string.no_ext_storage);
		}
		// builder.setIcon(R.drawable.ic_menu_open);
		builder.setTitle(R.string.load_eeprom);
		return builder.create();
	}


	private void loadEEPROM(final File file) {
		int len = (int) file.length();
		final String[] ids;
		try {
			// Determine ECM type
			ids = EEPROM.size2id(getActivity(), len);
		} catch (IOException e) {
			Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
			return;
		}
		if (ids.length > 1) {
			Builder builder = new Builder(getActivity());
			builder.setItems(ids, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String selected = ids[which];
					loadEEPROM(selected, file);
				}
			});
			builder.setTitle(R.string.select_ecm_type).create().show();
		} else if (ids.length == 1) {
			loadEEPROM(ids[0], file);
		}
	}

	private void loadEEPROM(String id, File file) {
		try {
			FileInputStream in = new FileInputStream(file);
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
}
