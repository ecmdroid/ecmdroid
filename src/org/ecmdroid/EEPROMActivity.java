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
import android.widget.GridView;
import android.widget.TextView;

public class EEPROMActivity extends BaseActivity {

	private static final int COLS = 5;
	ECM ecm = ECM.getInstance(this);
	private TextView offsetHex, offsetDec;
	private TextView byteValHex, byteValDec;
	private TextView hiShortHex, hiShortDec;
	private TextView loShortHex, loShortDec;

	private EEPROMAdapter adapter;
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

		GridView gridview = (GridView) findViewById(R.id.eepromGrid);
		adapter = new EEPROMAdapter(this, ecm.getEEPROM(), COLS);
		gridview.setAdapter(adapter);
		// TODO: Chose a nice drawable for currently selected cell
		//gridview.setSelector(android.R.drawable.edit_text);

		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				if (pos % COLS != 0) {
					int offset = pos - (pos / COLS + 1);
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
	}
}
