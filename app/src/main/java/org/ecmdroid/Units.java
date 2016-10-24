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

import java.util.HashMap;

/**
 * {@code Units} maps a unit name to its symbol (e.g. "Degree" maps to "°").
 */
public abstract class Units {
	private static final HashMap<String, String> map = new HashMap<String, String>();

	static {
		map.put("Degree", "°");
		map.put("Degree BTDC", "°");
		map.put("Degree C", "°");
		map.put("Degrees", "°");
		map.put("Degrees BDD", "°");
		map.put("Degrees C", "°");
		map.put("Percent", "%");
		map.put("TE degC", "°");
		map.put("Volt", "V");
		map.put("Volts", "V");
	}

	public static String getSymbol(String unit) {
		if (unit != null) {
			return map.get(unit);
		}
		return null;
	}
}
