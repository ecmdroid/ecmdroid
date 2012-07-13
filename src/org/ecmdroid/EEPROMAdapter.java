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
	private int cols;

	public EEPROMAdapter(Context ctx, EEPROM eeprom, int cols) {
		this.ctx = ctx;
		if (eeprom != null && eeprom.getBytes() != null) {
			data = eeprom.getBytes();
		}
		this.cols = cols;
	}

	public int getCount() {
		return data.length + data.length / cols + 1;
	}

	public Object getItem(int pos) {
		if (pos % cols == 0) {
			return toHex(pos - (pos / cols), 4);
		}

		return toHex(data[fpos(pos)] & 0xff);
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
		if (pos % cols != 0) {
			tv.setBackgroundColor(ColorMap.getColor(data[fpos(pos)]));
			tv.setTextColor(Color.BLACK);
			tv.setTag(null);
		} else {
			tv.setBackgroundColor(Color.BLACK);
			tv.setTextColor(Color.GRAY);
			tv.setTag("OFFSET");
		}
		return tv;
	}

	public long getItemId(int position) {
		return 0;
	}

	private int fpos(int pos) {
		int r = pos - (pos / cols + 1);
		return r;
	}

	private static String toHex(int i, int... width) {
		String fmt = "%0" + (width.length == 1 ? width[0] : 2) + "X";
		return String.format(fmt, i);
	}
}
