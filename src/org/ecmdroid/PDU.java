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
package org.ecmdroid;

import java.text.ParseException;


public class PDU
{
	public static final byte CMD_RTDATA  = 0x43;
	public static final int CMD_SET      = 0x57;
	public static final int CMD_GET      = 0x52;
	public static final byte CMD_VERSION = 0x56;
	public static final byte ACK         = 0x06;
	public static final byte DROID_ID    = 0x00;
	public static final byte ECM_ID      = 0x42;
	public static final byte SOH         = 0x01;
	public static final byte EOH         = (byte) 0xFF;
	public static final byte SOT         = 0x02;
	public static final byte EOT         = 0x03;


	public static enum Function {
		ClearCodes(1, "Clear Codes"),
		FrontCoil(2, "Front Coil"),
		RearCoil(3, "Rear Coil"),
		Tachometer(4,  "Tachometer"),
		FuelPump(5, "Fuel Pump"),
		FrontInj(6, "Front Injector"),
		Rear_Inj(7, "Rear Injector"),
		TPS_Reset(8, "TPS Reset"),
		Fan(9, "Fan"),
		Exh_Valve(0x0a, "Exhaust Valve");

		byte code;
		String name;

		private Function(int code, String name) {
			this.code = (byte) (code & 0xff);
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}

	private byte[] pdu;

	private static final PDU GET_VERSION = new PDU(DROID_ID, ECM_ID, new byte[] {0x56});
	private static final PDU GET_RT      = new PDU(DROID_ID, ECM_ID, new byte[] {0x43});
	private static final PDU GET_CSTATE  = PDU.getRequest(0x20, 0, 1);

	/** Construct a EEPROM GET Request
	 *
	 * @param pageno EEPROM page number to read from
	 * @param offset offset within selected page
	 * @param len number of bytes to read
	 */
	public static PDU getRequest(int pageno, int offset, int len)
	{
		byte[] payload = new byte[4];
		payload[0] = CMD_GET;
		payload[1] = (byte) (offset & 0xff);
		payload[2] = (byte) (pageno & 0xff);
		payload[3] = (byte) (len & 0xff);
		return new PDU(DROID_ID, ECM_ID, payload);
	}

	/**
	 * Construct a EEPROM SET Request
	 * @param pageno EEPROM page number to read from
	 * @param offset offset within selected page
	 * @param data the data to write to the EEPROM at the specified position
	 * @param pos the offset within the data buffer
	 * @param len the number of bytes to include in the SET request
	 */
	public static PDU setRequest(int pageno, int offset, byte[] data, int pos, int len)
	{
		byte[] payload = new byte[3 + len];
		payload[0] = CMD_SET;
		payload[1] = (byte) (offset & 0xff);
		payload[2] = (byte) (pageno & 0xff);
		System.arraycopy(data, pos, payload, 3, len);
		return new PDU(DROID_ID, ECM_ID, payload);
	}

	/**
	 * Construct a Function Trigger
	 * @param function the function to trigger
	 */
	public static PDU commandRequest(final Function function) {
		return new PDU(DROID_ID, ECM_ID, new byte[]{CMD_SET, 0, 0x20, function.code});
	}

	public static PDU getVersion() {
		return GET_VERSION;
	}

	public static PDU getRuntimeData() {
		return GET_RT;
	}

	public static PDU getCurrentState() {
		return GET_CSTATE;
	}

	public PDU(byte[] packet, int len) throws ParseException
	{
		this.pdu = new byte[len];
		System.arraycopy(packet, 0, this.pdu, 0, len);
		validate();
	}

	private void validate() throws ParseException {
		if  (pdu.length < 9) {
			throw new ParseException("Short packet length.", 0);
		}
		// Check for markers
		if (pdu[0] != SOH) {
			throw new ParseException("Packet does not start with SOH.", 0);
		}
		if (pdu[4] != EOH) {
			throw new ParseException("No EOH detected.", 4);
		}
		if (pdu[5] != SOT) {
			throw new ParseException("No SOT detected.", 5);
		}
		int size = pdu[3] & 0xff;
		if (pdu.length - 7 != size) {
			throw new ParseException("Size/Length mismatch (" + (pdu.length - 7) + "/" + size +")", 3);
		}
		if (pdu[pdu.length-2] != EOT) {
			throw new ParseException("No EOT detected.", 2);
		}
		// Checksum
		byte cs = checksum();
		if ( checksum() != pdu[pdu.length-1]) {
			throw new ParseException("Invalid checksum (" + Integer.toHexString(cs)+ "/" + Integer.toHexString(pdu[pdu.length-1] & 0xff) +")", pdu.length - 1);
		}
	}

	public PDU(byte sender, byte recipient, byte[] payload) {
		int i = 0;
		pdu = new byte[payload.length + 8];
		pdu[i++] = SOH;
		pdu[i++] = sender;
		pdu[i++] = recipient;
		pdu[i++] = (byte) (payload.length + 1);
		pdu[i++] = EOH;
		pdu[i++] = SOT;
		System.arraycopy(payload, 0, pdu, i, payload.length);
		i += payload.length;
		pdu[i++] = EOT;
		pdu[i++] = checksum();
	}

	public byte getSender() {
		return pdu[1];
	}

	public byte getRecipient() {
		return pdu[2];
	}

	public int getDataLength() {
		return pdu[3] & 0xff;
	}

	public byte[] getPayload() {
		byte[] ret = new byte[getDataLength()-1];
		System.arraycopy(pdu, 6, ret, 0, ret.length);
		return ret;
	}

	public byte[] getEEPromData() {
		byte[] ret = new byte[getDataLength() - (isRequest() ? 4 : 2)];
		System.arraycopy(pdu, isRequest() ? 9 : 7, ret, 0, ret.length);
		return ret;
	}

	public int getPageNr() throws IllegalStateException {
		int ret = -1;
		if (isRequest() && (pdu[6] == CMD_GET || pdu[6] == CMD_SET)) {
			ret = pdu[8] & 0xff;
		}
		return ret;
	}

	public int getPageOffset() throws IllegalStateException {
		int ret = -1;
		if (isRequest() && (pdu[6] == CMD_GET || pdu[6] == CMD_SET)) {
			ret = pdu[7] & 0xff;
		}
		return ret;
	}

	public int getCommand() {
		return isRequest() ? getErrorIndicator() : 0;
	}
	public int getErrorIndicator() {
		return isResponse() ? (byte)(pdu[6] & 0xff) : 0;
	}

	public boolean isACK() {
		return isResponse() && (pdu[6] == ACK);
	}

	public boolean isRequest() {
		return getRecipient() == ECM_ID;
	}

	public boolean isResponse() {
		return getSender() == ECM_ID;
	}

	public byte[] getBytes()
	{
		return pdu;
	}

	@Override
	public String toString()
	{
		return Utils.hexdump(pdu, 0, pdu.length);
	}

	private byte checksum()
	{
		byte cs = 0;
		for (int i = 1; i < pdu.length - 1; i++) {
			cs ^= (pdu[i]);
		}
		return cs;
	}
}
