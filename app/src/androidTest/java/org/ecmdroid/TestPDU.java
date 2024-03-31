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
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(JUnit4.class)
public class TestPDU {
	@Test
	public void testVersionParsing() throws ParseException, UnsupportedEncodingException {
		byte[] data = new byte[]{0x01, 0x42, 0x00, 0x13, (byte) 0xFF, 0x02, 0x06, 0x42, 0x55, 0x45, 0x49, 0x42, 0x33, 0x31, 0x30, 0x20, 0x31, 0x32, 0x2D, 0x31, 0x31, 0x2D, 0x30, 0x33, 0x03, (byte) 0xE2};
		PDU pdu = new PDU(data, data.length);
		assertFalse(pdu.isRequest());
		assertTrue(pdu.isResponse());
		assertTrue(pdu.isACK());
		assertEquals(pdu.getSender(), PDU.STOCK_ECM_ID);
		assertEquals("BUEIB310 12-11-03", new String(pdu.getEEPromData(), "US-ASCII"));
	}

	@Test
	public void testSetRequest() throws UnsupportedEncodingException {
		byte[] payload = "TestMe".getBytes();
		PDU pdu = PDU.setRequest(0x42, 0x24, payload, 0, payload.length);
		Log.d("TestPDU", pdu.toString());
		assertTrue(pdu.isRequest());
		assertFalse(pdu.isResponse());
		assertFalse(pdu.isACK());
		assertEquals(0x42, pdu.getPageNr());
		assertEquals(0x24, pdu.getPageOffset());
		assertEquals("TestMe", new String(pdu.getEEPromData(), "US-ASCII"));
	}

	@Test
	public void testGetRequest() {
		PDU pdu = PDU.getRequest(1, 0, 0x10);
		assertTrue(pdu.isRequest());
		assertFalse(pdu.isResponse());
		assertFalse(pdu.isACK());
		assertEquals(1, pdu.getPageNr());
		assertEquals(0, pdu.getPageOffset());
		assertEquals("01:00:42:05:ff:02:52:00:01:10:03:fa", Utils.hexdump(pdu.getBytes()).toLowerCase());
	}

	@Test
	public void testGetResponse() throws ParseException {
		byte[] data = new byte[]{0x01, 0x42, 0x00, 0x12, (byte) 0xff, 0x02, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x4d, (byte) 0xe8, 0x03, 0x7b, 0x06, (byte) 0x9e, 0x00, (byte) 0xcc, 0x00, (byte) 0xf4, 0x01, 0x03, (byte) 0xd5};
		PDU pdu = new PDU(data, data.length);
		assertTrue(pdu.isResponse());
		assertFalse(pdu.isRequest());
		assertTrue(pdu.isACK());
		assertEquals("01:42:00:12:ff:02:06:00:00:00:01:00:4d:e8:03:7b:06:9e:00:cc:00:f4:01:03:d5", Utils.hexdump(pdu.getBytes()).toLowerCase());
		assertEquals("00:00:00:01:00:4d:e8:03:7b:06:9e:00:cc:00:f4:01", Utils.hexdump(pdu.getEEPromData()).toLowerCase());
	}

	@Test
	public void testFunctions() {
		PDU pdu = PDU.commandRequest(PDU.Function.Fan);
		Log.d("TestPDU", pdu.toString());
		assertTrue(pdu.isRequest());
		assertFalse(pdu.isResponse());
		assertFalse(pdu.isACK());
		assertEquals("01:00:42:05:FF:02:57:00:20:09:03:C7", Utils.hexdump(pdu.getBytes()).toUpperCase());
	}
}
