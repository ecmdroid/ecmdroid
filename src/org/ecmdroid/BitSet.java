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

import java.util.Iterator;
import java.util.LinkedList;


public class BitSet implements Iterable<Bit>
{
	@SuppressWarnings("unused")
	private static final String TAG = "BitSet";
	private Bit[] bits = new Bit[8];

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
		for (Bit b : bits) {
			if (b != null && b.refreshValue(data)) {
				active.add(b);
			}
		}
		return active;
	}

	public void add(Bit bit) {
		bits[bit.getBitNr()] = bit;
	}

	public Bit getBit(int bit) {
		return bits[bit];
	}

	/**
	 * Set / Clear all bits in this set
	 */
	public void setAll(boolean value) {
		for (Bit bit : bits) {
			if (bit != null) bit.setValue(value);
		}
	}

	/**
	 * Return a byte with appropriate bits in this set enabled
	 */
	public byte getValue() {
		byte result = 0;
		for (Bit bit : bits) {
			if (bit != null) {
				result |= bit.getValue();
			}
		}
		return result;
	}

	/**
	 * Return the bitmask for all bits in this set.
	 */
	public byte getMask() {
		byte result = 0;
		for (Bit bit : bits) {
			if (bit != null) {
				result |= ((1 << bit.getBitNr() & 0xFF));
			}
		}
		return result;
	}


	/**
	 * Update bits in given byte array according to the bits in this set.
	 * @return true if the underlying byte actually changed
	 */
	public boolean updateValue(byte[] bytes) {
		byte nval = getValue();
		byte mask = getMask();
		int co = offset < 0 ? bytes.length + offset : offset;
		byte oldval = bytes[co];
		byte val = (byte) ((oldval & ~mask) & 0xFF);
		val |= nval;
		// Log.d(TAG, String.format("Setting bit set '%s', offset 0x%04X, to %s", name, co, (Integer.toBinaryString(0x100 | (val & 0xFF)).substring(1,9))));
		bytes[co] = val;
		return val != oldval;
	}

	public Iterator<Bit> iterator() {
		LinkedList<Bit> result = new LinkedList<Bit>();
		for (Bit b : bits) {
			if (b != null) {
				result.add(b);
			}
		}
		return result.iterator();
	}
}
