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
package org.ecmdroid;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

/**
 * Android Adapter and Section Indexer for the EEPROM Editor.
 */
public class EEPROMAdapter extends BaseAdapter implements SectionIndexer
{
	private static final int SECTION_LENGTH = 8; // Number of rows per section
	private Context ctx;
	private byte[] data = new byte[0];
	private int cols;

	private class Section {
		int pos;
		String name;
		public Section(int pos, String name) {
			this.pos = pos;
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	private Section[] sections;
	private int count;



	public EEPROMAdapter(Context ctx, EEPROM eeprom, int cols) {
		this.ctx = ctx;
		if (eeprom != null && eeprom.getBytes() != null) {
			data = eeprom.getBytes();
		}

		this.cols = cols;

		count = data.length + (data.length / (cols -1)) + (data.length % (cols-1) != 0 ? 1 : 0);
		int sd = SECTION_LENGTH * cols;
		sections = new Section[count / sd];
		for (int i=0; i < sections.length; i++) {
			sections[i] = new Section(i * sd, Utils.toHex(fpos(i * sd + 1), 3));
		}
	}

	public int getCount() {
		return count;
	}

	public Object getItem(int pos) {
		if (pos % cols == 0) {
			return Utils.toHex(pos - (pos / cols), 4);
		}

		return Utils.toHex(data[fpos(pos)] & 0xff);
	}

	public View getView(int pos, View view, ViewGroup parent) {
		TextView tv;
		if (view == null)
		{
			tv = new TextView(ctx);
			tv.setTextColor(Color.BLACK);
			tv.setGravity(Gravity.CENTER);
			tv.setHeight(50);
		} else {
			tv = (TextView) view;
		}
		tv.setText(getItem(pos).toString());

		if (pos % cols != 0) {
			tv.setBackgroundColor(ColorMap.getColor(data[fpos(pos)]));
			tv.setTextColor(Color.BLACK);
			tv.setTag(null);
		} else  {
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

	public int getPositionForSection(int section) {
		return sections[section].pos;
	}

	public int getSectionForPosition(int pos) {
		return 0;
	}

	public Object[] getSections() {
		return sections;
	}
}
