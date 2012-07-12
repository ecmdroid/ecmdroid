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

import java.util.ArrayList;
import java.util.Collection;

import org.ecmdroid.ECM.Type;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class EEPROM {

	private static final String TAG = "EEPROM";
	private ECM.Type type;
	private String id;
	private String version;
	private ArrayList<Page> pages;
	private int length = 0;
	private byte[] data;
	private boolean eepromRead;

	private EEPROM(String id, ECM.Type type, Integer... pagesizes)
	{
		this.id = id;
		this.type = type;
		int i=1;
		pages = new ArrayList<Page>();
		for (Integer ps : pagesizes) {
			Page pg = new Page(i++, ps.intValue());
			pg.setStart(length);
			pg.parent = this;
			pages.add(pg);
			length += ps.intValue();
		}
		data = new byte[length];
	}

	private EEPROM() {
	}

	public int length() {
		return length;
	}


	public byte[] getBytes() {
		return data;
	}

	public Collection<Page> getPages() {
		return pages;
	}

	public String getId() {
		return id;
	}

	public ECM.Type getType() {
		return type;
	}
	@Override
	public String toString() {
		return "EEPROM[id: " + id + ", type: " + type + ", version: " + version +", length: " + length + ", number of pages: " + pages.size() + "]";
	}

	public static EEPROM get(String name, Context ctx) {
		if (name == null) {
			return null;
		}
		if (name.length() > 5) {
			name = name.substring(0, 5);
		}
		DBHelper helper = new DBHelper(ctx);
		EEPROM eeprom = null;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = null;
		try {
			String query = "SELECT xsize, type, page, pages.size as pgsize" +
					" FROM eeprom, pages" +
					" WHERE pages.category = eeprom.category" +
					" AND name = '" + name + "'" +
					" ORDER BY page";
			// Log.d(TAG, query);
			c = db.rawQuery(query, null);
			if (c.getCount() == 0) {
				return null;
			}
			eeprom = new EEPROM();
			eeprom.id = name;
			eeprom.pages = new ArrayList<EEPROM.Page>();
			int pc = 0;
			while(c.moveToNext()) {
				if (eeprom.length == 0) {
					eeprom.length = c.getInt(c.getColumnIndex("xsize"));
					eeprom.type = Type.getType(c.getString(c.getColumnIndex("type")));
					eeprom.data = new byte[eeprom.length];
				}
				int pnr = c.getInt(c.getColumnIndex("page"));
				int sz  = c.getInt(c.getColumnIndex("pgsize"));
				Page pg = eeprom.new Page(pnr, sz);
				if (pnr == 0) {
					pg.start = eeprom.length - pg.length;
				} else {
					pg.start = pc;
					pc += pg.length;
				}
				pg.parent = eeprom;
				eeprom.pages.add(pg);
			}
		} finally {
			if (c != null) {
				c.close();
			}
			db.close();
		}
		return eeprom;
	}

	public class Page {
		private int nr;
		private int length;
		private int start;
		private EEPROM parent;

		private Page(int nr, int length) {
			this.nr = nr;
			this.length = length;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int nr() {
			return nr;
		}

		public int start() {
			return start;
		}

		public int length() {
			return length;
		}

		public EEPROM getParent() {
			return parent;
		}
	}

	public int getPageCount() {
		return pages.size();
	}

	public Page getPage(int pageno) {
		return pages.get(pageno);
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isEepromRead() {
		return eepromRead;
	}

	public void setEepromRead(boolean eepromRead) {
		this.eepromRead = eepromRead;
	}
}
