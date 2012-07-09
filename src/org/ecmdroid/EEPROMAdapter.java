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

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EEPROMAdapter extends BaseAdapter
{
	private Context ctx;
	private byte[] data = new byte[0];

	public EEPROMAdapter(Context ctx, EEPROM eeprom) {
		this.ctx = ctx;
		if (eeprom != null && eeprom.getBytes() != null) {
			data = eeprom.getBytes();
		}
	}

	public int getCount() {
		return data.length;
	}

	public Object getItem(int pos) {
		String ret = (pos > 0x0F ? "" : "0") + Integer.toHexString(data[pos]&0xff);
		return ret;
	}

	public View getView(int pos, View view, ViewGroup parent) {
		TextView tv;
		if (view == null)
		{
			tv = new TextView(ctx);
			tv.setTextColor(Color.BLACK);
			tv.setGravity(Gravity.CENTER);
			tv.setHeight(60);
		} else {
			tv = (TextView) view;
		}
		tv.setText(getItem(pos).toString());
		tv.setBackgroundColor(ColorMap.getColor(data[pos]));
		return tv;
	}

	public long getItemId(int position) {
		return 0;
	}
}
