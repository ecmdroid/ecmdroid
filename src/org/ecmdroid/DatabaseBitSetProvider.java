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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseBitSetProvider extends BitSetProvider {

	private DBHelper dbHelper;

	public DatabaseBitSetProvider(Context ctx) {
		dbHelper = new DBHelper(ctx);
	}
	@Override
	public BitSet getBitSet(String ecm, String name) {
		BitSet ret = null;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			String query = "SELECT * FROM rtoffsets, bits, eeprom" +
					" WHERE rtoffsets.varname = '" + name + "'" +
					" AND bits.varname = rtoffsets.varname" +
					" AND eeprom.name = '" + ecm + "'" +
					" AND rtoffsets.category = eeprom.category" +
					" AND rtoffsets.secret = 0";
			// Log.d(TAG, "Query: " + query);
			Cursor c = db.rawQuery(query, null);
			if (c.moveToFirst()) {
				String setname = c.getString(c.getColumnIndex("varname"));
				String label = c.getString(c.getColumnIndex("name"));
				int offset = c.getInt(c.getColumnIndex("offset"));
				ret = new BitSet(setname, label, offset);
				for (int i = 1; i <= 8; i++) {
					String bitname = c.getString(c.getColumnIndex("bitname" + i));
					String bitdesc = c.getString(c.getColumnIndex("bit" + i));
					if (Utils.isEmpty(bitname) && Utils.isEmpty(bitdesc)) {
						continue;
					}
					if (Utils.isEmpty(bitname)) {
						bitname = setname + "." + i;
					}
					Bit bit = new Bit();
					bit.setName(bitname);
					bit.setBit(i-1);
					bit.setByte_nr(c.getInt(c.getColumnIndex("byte")));
					bit.setOffset(offset);
					bit.setType(ECM.Type.getType(c.getString(c.getColumnIndex("type"))));
					bit.setRemark(bitdesc);
					bit.setCode(c.getString(c.getColumnIndex("dtc" + i)));
					// Log.d(TAG, bit.toString());
					ret.add(bit);
				}
			}
			c.close();
		} finally {
			db.close();
		}
		return ret;
	}

}
