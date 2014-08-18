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
package org.ecmdroid.test;

import org.ecmdroid.ECM;
import org.ecmdroid.EEPROM;

import android.test.AndroidTestCase;


public class TestEEPROM extends AndroidTestCase {
	public void testVersion()
	{
		EEPROM eeprom = EEPROM.get("BUEIB310 10-11-03", getContext());
		assertNotNull(eeprom);
		assertEquals(7, eeprom.getPageCount());
		assertEquals(1206, eeprom.getPage(0).start());
		assertEquals(0, eeprom.getPage(1).start());
		assertEquals(0x100, eeprom.getPage(2).start());
		assertEquals(0x200, eeprom.getPage(3).start());
		assertEquals(0x29e, eeprom.getPage(4).start());
		assertEquals(0x39e, eeprom.getPage(5).start());
		assertEquals(0x49e, eeprom.getPage(6).start());
		assertEquals(ECM.Type.DDFI2, eeprom.getType());
	}
}
