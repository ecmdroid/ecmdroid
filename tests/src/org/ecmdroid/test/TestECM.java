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
package org.ecmdroid.test;

import java.io.IOException;
import java.util.Collection;

import org.ecmdroid.ECM;
import org.ecmdroid.EEPROM;
import org.ecmdroid.Error;
import org.ecmdroid.Error.ErrorType;

import android.test.AndroidTestCase;


public class TestECM extends AndroidTestCase {
	private ECM ecm;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		byte[] epd = TestUtils.readEEPROM();
		byte[] rtd = TestUtils.readRTData();
		ecm = ECM.getInstance(getContext());
		EEPROM eeprom = EEPROM.get("BUEIB", getContext());
		ecm.setEEPROM(eeprom);
		ecm.setRealtimeData(rtd);
		System.arraycopy(epd, 0, eeprom.getBytes(), 0, epd.length);
	}

	public void testErrorParsing() throws IOException {
		Collection<Error> errors;

		errors = ecm.getErrors(ErrorType.CURRENT);
		assertEquals(1, errors.size());
		assertEquals("21", (errors.iterator().next()).getCode());

		errors = ecm.getErrors(ErrorType.STORED);
		assertEquals(1, errors.size());
		assertEquals("21", (errors.iterator().next()).getCode());
	}

	public void testSerialNo() {
		assertEquals("204", ecm.getSerialNo());
	}

	public void testMfgDate() {
		ecm.getMfgDate();
		assertEquals("6/8/2006", ecm.getMfgDate());
	}
}
