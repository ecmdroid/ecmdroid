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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestUtils {
	public static byte[] readRTData() throws IOException {
		return readResource("/org/ecmdroid/test/resources/RT_BUEIB242.bin");
	}
	public static byte[] readEEPROM() throws IOException {
		return readResource("/org/ecmdroid/test/resources/BUEIB.eeprom");
	}

	public static byte[] readBinaryLog() throws IOException {
		return readResource("/org/ecmdroid/test/resources/BUEIB_log.bin");
	}

	private static byte[] readResource(String resource) throws IOException {
		InputStream in = TestUtils.class.getResourceAsStream(resource);
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = in.read(buffer)) > 0){
			bao.write(buffer, 0, length);
		}
		in.close();
		return  bao.toByteArray();
	}
}
