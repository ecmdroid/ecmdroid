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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.ecmdroid.util.Bin2MslConverter;

import android.test.AndroidTestCase;

public class TestBin2Msl extends AndroidTestCase {

	public void testConversion() throws IOException
	{
		ByteArrayInputStream bin = new ByteArrayInputStream(TestUtils.readBinaryLog());
		ByteArrayOutputStream msl = new ByteArrayOutputStream(1024*1024*10);
		new Bin2MslConverter().convert(bin, msl);
		System.out.println("Number of bytes in msl file: " + msl.size());
	}
}
