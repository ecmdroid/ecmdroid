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

import java.util.Collection;
import java.util.LinkedList;

import org.ecmdroid.Variable.Class;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DatabaseVariableProvider extends VariableProvider {

	private DBHelper dbHelper;

	public DatabaseVariableProvider(Context ctx) {
		dbHelper = new DBHelper(ctx);
	}

	@Override
	public Collection<String> getRtVariableNames(String ecm) {
		return getRtVariableNames(ecm, null);
	}

	@Override
	public Collection<String> getScalarRtVariableNames(String ecm) {
		return getRtVariableNames(ecm, Class.SCALAR);
	}
	private Collection<String> getRtVariableNames(String ecm, Class type) {
		LinkedList<String> ret = new LinkedList<String>();
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			String query = "SELECT names.origname FROM names, rtoffsets, eeprom " +
					" WHERE eeprom.name = '" + ecm + "'" +
					" AND rtoffsets.category = eeprom.category" +
					" AND names.varname = rtoffsets.varname" +
					" AND rtoffsets.secret = 0 " +
					" AND names.secret = 0";
			if (type != null) {
				query += " AND UPPER(rtoffsets.type) = '" + type.toString().toUpperCase() + "'";
			}
			query += " ORDER BY UPPER(names.origname)";
			// Log.d(TAG, "Query: " + query);
			Cursor cursor = db.rawQuery(query, null);
			while (cursor.moveToNext()) {
				ret.add(cursor.getString(0));
			}
			cursor.close();
		} finally {
			db.close();
		}
		return ret;
	}

	@Override
	public Variable getRtVariable(String ecm, String name) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Variable ret = null;
		try {
			String query = "SELECT rtoffsets.*, names.*, eeprom.type as ecm_type FROM rtoffsets, eeprom, names " +
					" WHERE eeprom.name = '" + ecm + "' AND names.origname = '" + name + "'"+
					" AND rtoffsets.category = eeprom.category" +
					" AND names.varname = rtoffsets.varname" +
					" AND names.secret = 0" +
					" AND rtoffsets.secret = 0";
			// Log.d(TAG, "Query: " + query);
			// TODO: Use selection Args?
			Cursor cursor = db.rawQuery(query, null);
			if (cursor.moveToFirst()) {
				ret = new Variable();
				ret.setId(cursor.getInt(cursor.getColumnIndex("uniqueid")));
				ret.setType(ECM.Type.getType(cursor.getString(cursor.getColumnIndex("ecm_type"))));
				ret.setName(cursor.getString(cursor.getColumnIndex("origname")));
				ret.setCls(Class.valueOf(cursor.getString(cursor.getColumnIndex("type")).toUpperCase()));
				ret.setWidth(cursor.getInt(cursor.getColumnIndex("size")));
				ret.setOffset(cursor.getInt(cursor.getColumnIndex("offset")));
				ret.setScale(cursor.getDouble(cursor.getColumnIndex("scale")));
				ret.setTranslate(cursor.getDouble(cursor.getColumnIndex("translate")));
				ret.setLow(cursor.getDouble(cursor.getColumnIndex("low")));
				ret.setHigh(cursor.getDouble(cursor.getColumnIndex("high")));
				ret.setUlow(cursor.getInt(cursor.getColumnIndex("ulow")));
				ret.setUhigh(cursor.getInt(cursor.getColumnIndex("uhigh")));
				ret.setFormat(cursor.getString(cursor.getColumnIndex("format")));
				ret.setLabel(cursor.getString(cursor.getColumnIndex("name")));
				ret.setRemarks(cursor.getString(cursor.getColumnIndex("remark")));
				ret.setDescription(cursor.getString(cursor.getColumnIndex("description")));
				ret.setUnit(cursor.getString(cursor.getColumnIndex("units")));
				ret.setSymbol(Units.getSymbol(ret.getUnit()));
				Log.d(TAG, ret.toString());
			}
			cursor.close();
		} finally {
			db.close();
		}
		return ret;
	}

	@Override
	public Variable getEEPROMVariable(String ecm, String name)
	{
		if (ecm == null || name == null) {
			return null;
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Variable ret = null;
		String query = "SELECT eeoffsets.*, names.*, eeprom.type as ecm_type FROM eeoffsets, eeprom, names " +
				" WHERE eeprom.name = '" + ecm + "' AND names.varname = '" + name + "'"+
				" AND eeoffsets.category = eeprom.category" +
				" AND eeoffsets.varname = names.varname";
		Log.d(TAG, query);
		Cursor cursor = db.rawQuery(query, null);
		if (cursor.moveToFirst()) {
			ret = new Variable();
			ret.setId(cursor.getInt(cursor.getColumnIndex("uniqueid")));
			ret.setType(ECM.Type.getType(cursor.getString(cursor.getColumnIndex("ecm_type"))));
			ret.setName(cursor.getString(cursor.getColumnIndex("origname")));
			String type = cursor.getString(cursor.getColumnIndex("type")).toUpperCase();
			ret.setCls(Class.valueOf(type));
			ret.setWidth(cursor.getInt(cursor.getColumnIndex("size")));
			ret.setOffset(cursor.getInt(cursor.getColumnIndex("offset")));
			ret.setScale(cursor.getDouble(cursor.getColumnIndex("scale")));
			ret.setTranslate(cursor.getDouble(cursor.getColumnIndex("translate")));
			ret.setFormat(cursor.getString(cursor.getColumnIndex("format")));
			ret.setLabel(cursor.getString(cursor.getColumnIndex("name")));
			ret.setRemarks(cursor.getString(cursor.getColumnIndex("remark")));
			ret.setDescription(cursor.getString(cursor.getColumnIndex("description")));
			ret.setUnit(cursor.getString(cursor.getColumnIndex("units")));
			ret.setSymbol(Units.getSymbol(ret.getUnit()));
		}
		cursor.close();
		db.close();
		return ret;
	}
}
