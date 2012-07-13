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

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class EEPROMActivity extends BaseActivity {

	private static final int COLS = 5;
	ECM ecm = ECM.getInstance(this);
	private TextView cellValue;
	private EEPROMAdapter adapter;
	private Button setButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eeprom);
		cellValue = (TextView) findViewById(R.id.cellValue);
		GridView gridview = (GridView) findViewById(R.id.eepromGrid);
		adapter = new EEPROMAdapter(this, ecm.getEEPROM(), COLS);
		gridview.setAdapter(adapter);
		gridview.setDrawSelectorOnTop(true);
		// TODO: Chose a nice drawable for currently selected cell
		//gridview.setSelector(android.R.drawable.edit_text);

		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				if (position % 5 != 0) {
					String sel = (String) parent.getItemAtPosition(position);
					cellValue.setText(Integer.toString(Integer.parseInt(sel, 16)));
					cellValue.setTag(Integer.valueOf(position));
				}
			}
		});

		setButton = (Button) findViewById(R.id.eepromSetButton);
		if (!ecm.isEepromRead()) {
			setButton.setEnabled(false);
			cellValue.setEnabled(false);
		}
		setButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Integer pos = (Integer) cellValue.getTag();
				if (pos != null) {
					int r = pos - (pos / COLS + 1);
					byte[] data = ecm.getEEPROM().getBytes();
					adapter.notifyDataSetChanged();
					try {
						data[r] = (byte) (Integer.decode("" + cellValue.getText()) & 0xFF);
					} catch (Exception e) {
						Toast.makeText(EEPROMActivity.this, cellValue.getText() + ": Illegal Value", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
	}
}
