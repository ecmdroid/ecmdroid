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

import java.util.LinkedList;

public class BitSet extends LinkedList<Bit>
{
	@SuppressWarnings("unused")
	private static final String TAG = "BitSet";
	private static final long serialVersionUID = 1L;

	private String name;
	private String label;
	private int offset;

	public BitSet(String name, String label, int offset) {
		super();
		this.name = name;
		this.label = label;
		this.offset = offset;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public int getOffset() {
		return offset;
	}


	public void setOffset(int offset) {
		this.offset = offset;
	}

	public BitSet getActiveBits(byte[] data) {
		BitSet active = new BitSet(name,label,offset);
		for (Bit b : this) {
			if (b.refreshValue(data)) {
				active.add(b);
			}
		}
		return active;
	}

	public Bit getBit(int bit) {
		return this.get(bit);
	}

	/**
	 * Set / Clear all bits in this set
	 */
	public void setAll(boolean value) {
		for (Bit bit : this) {
			bit.setValue(value);
		}
	}

	/**
	 * Return a byte with appropriate bits in this set enabled
	 */
	public byte getValue() {
		byte result = 0;
		for (Bit bit : this) {
			result |= bit.getValue();
		}
		return result;
	}

	/**
	 * Return the bitmask for all bits in this set.
	 */
	public byte getMask() {
		byte result = 0;
		for (Bit bit : this) {
			result |= ((1 << bit.getBitNr() & 0xFF));
		}
		return result;
	}


	/**
	 * Update bits in given byte array according to the bits in this set.
	 */
	public void updateValue(byte[] bytes) {
		byte nval = getValue();
		byte mask = getMask();
		int co = offset < 0 ? bytes.length + offset : offset;
		byte val = bytes[co];
		val = (byte) ((val & ~mask) & 0xFF);
		val |= nval;
		// Log.d(TAG, String.format("Setting bit set '%s', offset 0x%04X, to %s", name, co, (Integer.toBinaryString(0x100 | (val & 0xFF)).substring(1,9))));
		bytes[co] = val;
	}
}
