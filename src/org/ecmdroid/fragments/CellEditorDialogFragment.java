/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2013 by Michel Marti

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

import org.ecmdroid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Simple Editor dialog for editing a single byte value
 */
public class CellEditorDialogFragment extends DialogFragment implements android.view.View.OnClickListener, OnKeyListener
{
	private static final String OFFSET = "offset";
	private static final String VALUE = "value";

	private CellEditorDialogListener listener;

	/**
	 * Implement this interface to get notified about the editors change
	 */
	public interface CellEditorDialogListener {
		public void onCellValueChanged(int pos, byte value);
	}

	/**
	 * Construct a new Editor Dialog Fragment
	 * @param offset position of the byte to edit
	 * @param value the byte value to edit
	 */
	public static CellEditorDialogFragment newInstance(int offset, byte value) {
		CellEditorDialogFragment dialog = new CellEditorDialogFragment();
		Bundle args = new Bundle();
		args.putByte(VALUE, value);
		args.putInt(OFFSET, offset);
		dialog.setArguments(args);
		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.edit_cell_value);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View editor = inflater.inflate(R.layout.cell_editor, null);
		editor.findViewById(R.id.b_d2).setOnClickListener(this);
		editor.findViewById(R.id.b_t2).setOnClickListener(this);
		editor.findViewById(R.id.b_m16).setOnClickListener(this);
		editor.findViewById(R.id.b_p16).setOnClickListener(this);
		editor.findViewById(R.id.b_zero).setOnClickListener(this);
		editor.findViewById(R.id.b_ff).setOnClickListener(this);
		EditText et = (EditText) editor.findViewById(R.id.editCellValue);
		et.setOnKeyListener(this);
		builder.setView(editor);
		updateEditor(editor, true);
		builder.setPositiveButton(R.string.set, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (listener != null) {
					listener.onCellValueChanged(getArguments().getInt(OFFSET), getArguments().getByte(VALUE));
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof CellEditorDialogListener) {
			listener = (CellEditorDialogListener) activity;
		}
	}

	/**
	 * Handle button clicks
	 */
	public void onClick(View v) {
		int value = getArguments().getByte(VALUE) & 0xff;
		switch(v.getId()) {
		case R.id.b_d2:
			value >>= 1;
			break;
		case R.id.b_t2:
			value <<= 1;
			break;
		case R.id.b_ff:
			value = 0xFF;
			break;
		case R.id.b_zero:
			value = 0;
			break;
		case R.id.b_m16:
			value -= 16;
			break;
		case R.id.b_p16:
			value += 16;
			break;
		}
		value = Math.max(0,Math.min(255, value));
		getArguments().putByte(VALUE, (byte)value);
		updateEditor(v.getRootView(), true);
	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		EditText te = (EditText) v;
		try {
			String input = te.getText().toString();
			int value = Integer.valueOf(input.length() == 0 ? "0" : input).intValue();
			getArguments().putByte(VALUE, (byte)value);
			updateEditor(v.getRootView(), false);
		} catch (NumberFormatException nfe){}
		return false;
	}

	private void updateEditor(View editor, boolean updateText) {
		byte value = getArguments().getByte(VALUE);
		TextView tv = (TextView) editor.findViewById(R.id.textCellValue);
		if (updateText) {
			EditText cv = (EditText) editor.findViewById(R.id.editCellValue);
			cv.setText(Integer.toString(value & 0xFF));
		}
		tv.setText(String.format("[0x%02X]", value & 0xFF));
	}
}
