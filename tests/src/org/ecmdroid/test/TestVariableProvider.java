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
import org.ecmdroid.Variable;
import org.ecmdroid.Variable.DataClass;
import org.ecmdroid.VariableProvider;

import android.test.AndroidTestCase;


public class TestVariableProvider extends AndroidTestCase
{
	private VariableProvider provider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		provider = VariableProvider.getInstance(this.getContext());
	}

	public void testRtVariableNames() {
		Collection<String> vars = provider.getRtVariableNames("BUE2D");
		assertEquals(110, vars.size());
		assertTrue(vars.contains("RPM"));
		assertTrue(vars.contains("TPD"));
		assertTrue(vars.contains("CLT"));
	}

	public void testRtVariable() {
		Variable v = provider.getRtVariable("BUE2D", "pw2");
		assertNotNull(v);
		assertEquals(ECM.Type.DDFI3, v.getType());
		assertEquals("pw2", v.getName());
		assertEquals(Variable.DataClass.SCALAR, v.getCls());
		assertEquals(2, v.getSize());
		assertEquals(23, v.getOffset());
		assertEquals("Milliseconds", v.getUnit());
		assertEquals(0.001330, v.getScale());
		assertEquals(0.0, v.getTranslate());
		assertEquals("Fuel Pulsewidth Rear", v.getLabel());
		assertEquals("0.00", v.getFormat());
		assertEquals(0.0, v.getLow());
		assertEquals(0.1159248615, v.getHigh());
		assertEquals(0, v.getUlow());
		assertEquals(65535, v.getUhigh());
	}

	public void testRTParsing() throws IOException {
		byte[] data = TestUtils.readRTData();
		Variable v = provider.getRtVariable("BUEIB", "AFV");
		assertNotNull(v);
		v.refreshValue(data);
		assertEquals("100.0%", v.getFormattedValue());
	}

	public void testAllRTVariables() throws IOException {
		byte[] data = TestUtils.readRTData();
		Collection<String> vars = provider.getRtVariableNames("BUEIB");
		for (String var : vars) {
			Variable v = provider.getRtVariable("BUEIB", var);
			assertNotNull(v);
			v.refreshValue(data);
			System.out.println("name: " + v.getName() + ", raw: " + v.getRawValue() + ", formatted: " + v.getFormattedValue());
		}
	}

	public void testEEPROMVariable() throws IOException {
		byte[] eeprom = TestUtils.readEEPROM();
		Variable var = provider.getEEPROMVariable("BUEIB", "Country_ID");
		assertNotNull(var);
		var.refreshValue(eeprom);
		assertEquals(77, var.getRawValue());
		var = provider.getEEPROMVariable("BUEIB", "KMFG_Year");
		assertNotNull(var);
		var.refreshValue(eeprom);
		assertEquals(6, var.getRawValue());
		var = provider.getEEPROMVariable("BUEIB", "KMFG_Serial");
		assertNotNull(var);
		var.refreshValue(eeprom);
		assertEquals(204, var.getRawValue());

	}

	public void testNameLookup()
	{
		assertNull(provider.getName("DOES_NOT_EXIST"));
		assertEquals("System Configuration", provider.getName("KConfig"));
		assertEquals("Open loop learn", provider.getName("KConfig[3]"));
		assertNull(provider.getName("KConfig[10]"));
	}

	public void testAxis() throws IOException {
		byte[] eeprom = TestUtils.readEEPROM();
		Variable var = provider.getEEPROMVariable("BUEIB", "Tab_Fuel_Load_Ax");
		assertNotNull(var);
		assertEquals(var.getCls(), DataClass.AXIS);
		var.refreshValue(eeprom);
		assertEquals(12, var.getSize());
		assertEquals(1, var.getWidth());
		int[] expected = { 10, 15, 20, 30, 40, 50, 60, 80, 100, 125, 175, 255 };
		for (int i=0; i< expected.length; i++) {
			assertEquals(String.valueOf(expected[i]), var.getFormattedValueAt(i));
			assertEquals(expected[i], var.getIntValueAt(i));
		}
		try {
			var.getIntValueAt(expected.length);
			fail("ArrayIndexOutOfBoundsException expected.");
		} catch (ArrayIndexOutOfBoundsException e) {}

		var = provider.getEEPROMVariable("BUEIB", "Tab_Fuel_RPM_Ax");
		assertNotNull(var);
		assertEquals(var.getCls(), DataClass.AXIS);
		var.refreshValue(eeprom);
		assertEquals(26, var.getSize());
		assertEquals(2, var.getWidth());
		expected = new int[]{ 8000, 7000, 6000, 5000, 4000, 3400, 2900, 2400, 1900, 1350, 1000, 800, 0 };
		for (int i=0; i< expected.length; i++) {
			assertEquals(String.valueOf(expected[i]), var.getFormattedValueAt(i));
			assertEquals(expected[i], var.getIntValueAt(i));
		}
		try {
			var.getIntValueAt(expected.length);
			fail("ArrayIndexOutOfBoundsException expected.");
		} catch (ArrayIndexOutOfBoundsException e) {}
	}
}

