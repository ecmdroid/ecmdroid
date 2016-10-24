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

import android.graphics.Color;

/**
 * Map an octet value to a corresponding ARGB color code.
 */
public class ColorMap {

	private static final int[] colors = new int[256];

	static {
		int r = 0, g = 0, b = 256;
		for (int i = 0; i < 256; i++) {
			colors[i] = Color.argb(255, Math.min(255, r), Math.min(255, g), Math.min(255, b));
			if (i < 64) {
				g += 4;
			} else if (i < 128) {
				b -= 4;
			} else if (i < 192) {
				r += 4;
			} else {
				g -= 4;
			}
		}
	}

	public static int getColor(byte val) {
		return colors[val & 0xff];
	}

}
