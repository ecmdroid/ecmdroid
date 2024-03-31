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

import android.support.test.runner.AndroidJUnit4;

import org.ecmdroid.Error.ErrorType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collection;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class TestECM{
	private ECM ecm;

	@Before
	public void setUp() throws Exception {
		byte[] epd = TestUtils.readEEPROM();
		byte[] rtd = TestUtils.readRTData();
		ecm = ECM.getInstance(getContext());
		EEPROM eeprom = EEPROM.get("BUEIB", getContext());
		ecm.setEEPROM(eeprom);
		ecm.setRuntimeData(rtd);
		System.arraycopy(epd, 0, eeprom.getBytes(), 0, epd.length);
	}

	@Test
	public void testErrorParsing() throws IOException {
		Collection<Error> errors;

		errors = ecm.getErrors(ErrorType.CURRENT);
		assertEquals(1, errors.size());
		assertEquals("21", (errors.iterator().next()).getCode());

		errors = ecm.getErrors(ErrorType.STORED);
		assertEquals(1, errors.size());
		assertEquals("21", (errors.iterator().next()).getCode());
	}

	@Test
	public void testSerialNo() {
		assertEquals("204", ecm.getSerialNo());
	}

	@Test
	public void testMfgDate() {
		ecm.getMfgDate();
		assertEquals("6/8/06", ecm.getMfgDate());
	}
}
