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
}
