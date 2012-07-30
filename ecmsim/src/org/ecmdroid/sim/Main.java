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

package org.ecmdroid.sim;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.ecmdroid.EEPROM;
import org.ecmdroid.EEPROM.Page;
import org.ecmdroid.PDU;

/**
 * Simple and stupid ECM Simulator. Listens on TCP port 6275 for
 * incoming connections from ecmdroid. Claims to be a BUE2D
 * DDFI3 ECM.
 *
 */
public class Main
{
	static final int PORT = 6275;
	static final PDU ACK = new PDU(PDU.ECM_ID, PDU.DROID_ID, new byte[]{0x06, 0x00});
	static final byte[]  VERSION_RESPONSE = {0x01, 0x42, 0x00, 0x13, (byte) 0xff, 0x02, 0x06, 0x42, 0x55, 0x45, 0x32, 0x44, 0x32, 0x34, 0x32, 0x20, 0x31, 0x31, 0x2d, 0x33, 0x30, 0x2d, 0x30, 0x39, 0x03, (byte) 0x93};
	static byte[] RT_DATA = new byte[] {0x01, 0x42, 0x00, (byte) 0x80, (byte) 0xff, 0x02, 0x06, 0x15, 0x58, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xcc, 0x25, (byte) 0xcc, 0x25, 0x13, 0x3f, 0x13, 0x3f, 0x23, 0x00, 0x15, (byte) 0xb5, 0x04, 0x49, 0x02, 0x7e, 0x02, 0x66, 0x00, 0x58, 0x02, 0x37, 0x06, (byte) 0xf2, 0x03, 0x00, 0x00, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, (byte) 0x86, 0x03, 0x00, (byte) 0x90, 0x00, 0x11, 0x3c, (byte) 0xb1, 0x5c, (byte) 0xff, 0x03, (byte) 0xe4, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x17, 0x00, 0x7d, 0x00, 0x00, 0x00, 0x00, 0x56, 0x00, 0x34, 0x01, (byte) 0xf2, (byte) 0xc2, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x83, 0x67, (byte) 0xff, (byte) 0xff, 0x70, 0x19, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x4d, 0x00, (byte) 0xdc, 0x00, 0x00, (byte) 0xb1, 0x5c, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x32, 0x00, 0x64, (byte) 0xe8, 0x03, (byte) 0xe8, 0x03, 0x00, 0x03, 0x7c};

	public static void main(String[] args) throws IOException
	{
		EEPROM eeprom = prepareEEPROM();
		ServerSocket socket = new ServerSocket(PORT);

		Socket s;
		while (true) {
			System.out.println("Waiting for incomming connection on port " + PORT + "...");
			s = socket.accept();
			System.out.println("Connection established...");
			InputStream in = s.getInputStream();
			OutputStream out = s.getOutputStream();
			byte[] buffer = new byte[256];
			int rtc = 0;
			long deviceTestStart = 0;
			while(!s.isClosed()) {
				try {
					int len = in.read(buffer,0,6);
					if (len == -1) {
						s.close();
						break;
					}
					len = (buffer[3]&0xff) + 1;
					len = in.read(buffer, 6, len);
					// Thread.sleep(50);
					switch (buffer[6])
					{
					case PDU.CMD_VERSION:
						System.out.println("Version request...");
						out.write(VERSION_RESPONSE);
						break;
					case PDU.CMD_SET:
						System.out.println("SET Command");
						// Page number 32 -> Virtual Page
						if (buffer[8] == 0x20) {
							System.out.println("Device test");
							deviceTestStart = System.currentTimeMillis();
						}
						out.write(ACK.getBytes());
						break;
					case PDU.CMD_RTDATA:
						System.out.println("RT Data request");
						rtc++;
						if (rtc % 5 == 0) {
							// broken pdu
							out.write(0x4711);
						}
						out.write(RT_DATA);
						break;

					case PDU.CMD_GET:
						int pgno = buffer[8] & 0xff;
						int offset  = buffer[7] & 0xff;
						int length = buffer[9] & 0xff;
						System.out.println("GET request for " + length  + " bytes from page " + pgno + " at offset " + offset);
						byte[] payload = new byte[length+1];
						payload[0]=0x06;
						if (pgno != 32) {
							Page pg = eeprom.getPage(pgno);
							pg.getBytes(offset, length, payload, 1);
						} else {
							// Virtual page 0x20 - device tests
							byte stat = (byte) ((System.currentTimeMillis() - deviceTestStart > 3000) ? 0 : 1);
							payload[1] = stat;
						}
						PDU response = new PDU(PDU.ECM_ID, PDU.DROID_ID, payload);
						out.write(response.getBytes());
						break;
					default:
						System.out.println("Unsupported command -> NACK");
						PDU pdu = new PDU(PDU.ECM_ID, PDU.DROID_ID, new byte[]{0x42, 0x42});
						out.write(pdu.getBytes());
					}
				} catch (SocketException se) {
					s.close();
					break;
				} catch (Exception ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

	static EEPROM prepareEEPROM() throws IOException {
		EEPROM eeprom = new EEPROM("BUE2D");
		eeprom.setBytes(readResource("BUE2D.xpr"));
		Page page = eeprom.new Page(0, 40);
		page.setStart(eeprom.length() - 256);
		eeprom.addPage(page);

		int start = 0;
		for (int i = 1; i < 15; i++) {
			page = eeprom.new Page(i, 256);
			page.setStart(start);
			start += page.length();
			eeprom.addPage(page);
		}

		page = eeprom.new Page(15, 172);
		page.setStart(start);
		eeprom.addPage(page);
		return eeprom;
	}

	static byte[] readResource(String resource) throws IOException {
		InputStream in = Main.class.getResourceAsStream(resource);
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
