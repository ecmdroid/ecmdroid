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
		assertEquals(111, vars.size());
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
		assertEquals(2, var.getWidth());
		assertEquals(1, var.getElementCount());
		assertEquals(2, var.getSize());
		assertEquals(0, var.getRows());
		assertEquals(0, var.getCols());
		assertEquals(DataClass.VALUE, var.getCls());
		var.refreshValue(eeprom);
		assertEquals(204, var.getRawValue());

		var.parseValue(0xCDDC);
		assertEquals(0xCDDC, var.getIntValue());
		var.updateValue(eeprom);
		assertEquals((byte)0xCD, eeprom[var.getOffset() + 1]);
		assertEquals((byte)0xDC, eeprom[var.getOffset()]);
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
		assertEquals(1, var.getCols());
		assertEquals(12, var.getRows());
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
		assertEquals(1, var.getCols());
		assertEquals(13, var.getRows());
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

	public void testTable() throws IOException {
		byte[] eeprom = TestUtils.readEEPROM();
		Variable var = provider.getEEPROMVariable("BUEIB", "Tab_ABP_Conv");
		assertNotNull(var);
		assertEquals(var.getCls(), DataClass.TABLE);
		assertEquals(5, var.getRows());
		assertEquals(2, var.getCols());
		var.refreshValue(eeprom);
		int[][] expected = {{125, 100}, {127, 60}, {170, 80}, {213, 100}, {250, 120}};
		for (int r = 0; r < expected.length; r++) {
			for (int c = 0; c < expected[0].length; c++) {
				assertEquals(String.valueOf(expected[r][c]), var.getFormattedValueAt(r, c));
				assertEquals(expected[r][c], var.getIntValueAt(r, c));
			}
		}
		try {
			var.getFormattedValueAt(expected.length + 1, expected[0].length + 1);
			fail("ArrayIndexOutOfBoundsException expected.");
		} catch (ArrayIndexOutOfBoundsException e) {}

		var.parseValueAt(0, 0, Integer.valueOf(42));
		var.parseValueAt(0, 1, Integer.valueOf(99));
		var.parseValueAt(1, 0, Integer.valueOf(88));
		var.parseValueAt(1, 1, Integer.valueOf(77));
		assertEquals(42, var.getIntValueAt(0, 0));
		assertEquals(99, var.getIntValueAt(0, 1));
		assertEquals(88, var.getIntValueAt(1, 0));
		assertEquals(77, var.getIntValueAt(1, 1));

		var.updateValue(eeprom);
		int o = var.getOffset();
		assertEquals(42, eeprom[o++]);
		assertEquals(99, eeprom[o++]);
		assertEquals(88, eeprom[o++]);
		assertEquals(77, eeprom[o++]);
	}
}

