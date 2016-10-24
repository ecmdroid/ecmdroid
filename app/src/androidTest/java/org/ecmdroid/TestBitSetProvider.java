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

import android.test.AndroidTestCase;

import org.ecmdroid.Constants.DataSource;

import java.io.IOException;


public class TestBitSetProvider extends AndroidTestCase {
	private BitSetProvider p;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		p = BitSetProvider.getInstance(this.getContext());
	}

	public void testBitSetConstructor() {
		BitSet bits = p.getBitSet("BUEIB", "CDiag0", DataSource.RUNTIME_DATA);
		assertNotNull(bits);
		assertEquals("CDiag0", bits.getName());
		assertEquals(67, bits.getOffset());
		int i = 0;
		for (Bit b : bits) {
			assertEquals(i++, b.getBitNr());
			assertEquals(bits.getOffset(), b.getOffset());
		}
	}

	public void testFlags1() throws IOException {
		byte[] data = TestUtils.readRTData();
		BitSet bitset = p.getBitSet("BUEIB", "Flags1", DataSource.RUNTIME_DATA);
		assertNotNull(bitset);
		BitSet active = bitset.getActiveBits(data);
		Bit b = active.iterator().next();
		assertEquals(7, b.getBitNr());
	}
}
