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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.ecmdroid.Constants.DataSource;
import org.ecmdroid.Constants.Variables;
import org.ecmdroid.EEPROM.Page;
import org.ecmdroid.Error.ErrorType;
import org.ecmdroid.PDU.Function;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;


public class ECM
{

	public static enum Type {
		DDFI1, DDFI2, DDFI3;
		public static Type getType(String type) {
			if ("DDFI".equals(type)) return DDFI1;
			if ("DDFI-2".equals(type)) return DDFI2;
			if ("DDFI-3".equals(type)) return DDFI3;
			return null;
		}
	}

	private static final boolean D = false;
	private VariableProvider variableProvider;
	private BitSetProvider bitsetProvider;

	private static final int DEFAULT_TIMEOUT = 1000;
	private static final int TCP_CONNECT_TIMEOUT = 5000;
	private static final String TAG = "ECM";
	private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String UNKNOWN = "N/A";


	private static ECM singleton;

	private byte[] mReceiveBuffer = new byte[256];

	private boolean connected;
	private Object socket;
	private InputStream in;
	private OutputStream out;
	private EEPROM eeprom;
	private byte[] rtData;
	private boolean recording;
	private Context context;

	private ECM(Context ctx)
	{
		variableProvider = VariableProvider.getInstance(ctx);
		bitsetProvider = BitSetProvider.getInstance(ctx);
		context = ctx;
	}

	/**
	 * Connect to given Bluetooth Serial Port
	 * @param bluetoothDevice the bluetooth modem
	 * @throws IOException
	 */
	public void connect(final BluetoothDevice bluetoothDevice) throws IOException
	{
		BluetoothSocket s = null;
		try {
			s = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);

			if (s != null) {
				s.connect();
				in = s.getInputStream();
				out = s.getOutputStream();
				socket = s;
			}
		} catch (IOException e) {
			Log.w(TAG, "Unable to connect. ", e);
			if (socket != null) {
				try {
					s.close();
				} catch (IOException e1) {
				}
				socket = null;
			}
			throw e;
		}
		connected = true;
	}

	/**
	 * Connect via TCP
	 * @param host host name
	 * @param port port
	 */
	public void connect(String host, int port) throws IOException
	{
		Socket s = new Socket();
		s.connect(new InetSocketAddress(host, port), TCP_CONNECT_TIMEOUT);
		in = s.getInputStream();
		out = s.getOutputStream();
		socket = s;
		connected = true;
	}


	/**
	 * Disconnect from the ECM
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		if (connected && socket != null) {
			if (socket instanceof BluetoothSocket) {
				((BluetoothSocket)socket).close();
			} else if (socket instanceof Socket) {
				((Socket)socket).close();
			}
			socket = null;
		}
		connected = false;
		rtData = null;
	}

	synchronized PDU sendPDU(PDU pdu) throws IOException {
		if (D) Log.d(TAG, "Sending: " + pdu);
		if (out == null) {
			throw new IOException("OutputStream to RFCOMM not available.");
		}
		byte[] bytes = pdu.getBytes();
		out.write(bytes);
		// Wait for response
		PDU ret = receivePDU();
		if (!ret.isACK()) {
			throw new IOException("Request not acknowledged by ECM (error code " + ret.getErrorIndicator() +").");
		}
		return ret;
	}

	private PDU receivePDU() throws IOException
	{
		try
		{
			read(mReceiveBuffer, 0, 6, DEFAULT_TIMEOUT);
			if (mReceiveBuffer[0] != PDU.SOH && mReceiveBuffer[4] != PDU.EOH && mReceiveBuffer[5] != PDU.SOT) {
				throw new IOException("Invalid Header received.");
			}
			int len = mReceiveBuffer[3] & 0xff;
			// Log.d(TAG, "Start of PDU: " + Utils.hexdump(mReceiveBuffer, 0, 6));
			read(mReceiveBuffer, 6, len + 1, DEFAULT_TIMEOUT);
			PDU response;
			try {
				response = new PDU(mReceiveBuffer,len + 7);
			} catch (ParseException e) {
				throw new IOException("Unable to parse incoming PDU. " + e.getLocalizedMessage());
			}
			if (D) Log.d(TAG, "Received: " + response);
			return response;
		} catch (IOException ioe) {
			Log.e(TAG, "I/O Exception receiving PDU. " + ioe.getMessage());
			// Drain receive buffer, we might be out-of-sync
			try {
				while(in.available() > 0) {
					in.read();
				}
			} catch (IOException e) {}
			throw ioe;
		}
	}

	/**
	 * Read out the EEPROM and return the version of the currently connected ECM.
	 * @return the full ECM version string (e.g. "BUEIB310 12-11-03")
	 * @throws IOException if communicating with the ECM fails
	 */
	public String setupEEPROM() throws IOException {
		String ret = readVersion();
		Log.i(TAG, "ECM Version: " + ret);
		eeprom = EEPROM.get(ret, context);
		if (eeprom != null) {
			eeprom.setVersion(ret);
		} else {
			Log.w(TAG, "Unknown ECM ID " + ret);
			throw new IOException("Unsupported ECM Version '" + ret + "'!");
		}
		return ret;
	}

	/**
	 * Return the current ECM version string
	 */
	public String getVersion() {
		return eeprom == null ? null : eeprom.getVersion();
	}

	/**
	 * Request the version string from the ECM
	 */
	public String readVersion() throws IOException {
		PDU response = sendPDU(PDU.getVersion());
		return new String(response.getEEPromData(), "US-ASCII");
	}

	/**
	 * Returns the current state of the ECM (Busy/Idle)
	 * @return 0 if the ECM is idle, any other value if it's busy.
	 * @throws IOException if communicating with the ECM fails
	 */
	public byte getCurrentState() throws IOException
	{
		PDU response = sendPDU(PDU.getCurrentState());
		return response.getEEPromData()[0];
	}

	/**
	 * Check if the ECM is busy.
	 * @return true if it is busy, otherwise false.
	 * @throws IOException if communicating with the ECM fails
	 */
	public boolean isBusy() throws IOException {
		return getCurrentState() != 0;
	}

	/**
	 * Run a test function
	 * @param function the function to invoke
	 * @throws IOException if an I/O error occurred
	 */
	public void runTest(Function function) throws IOException {
		PDU response = sendPDU(PDU.commandRequest(function));
		if (!response.isACK()) {
			throw new IOException("Test failed.");
		}
	}

	/**
	 * Read a single page from the EEPROM. The data read will be stored within the byte array
	 * of the EEPROM object holding the page.
	 * @param page the Page to read
	 * @throws IOException if an I/O error occurred
	 */
	public void readEEPromPage(Page page) throws IOException {
		byte[] buffer = page.getParent().getBytes();
		for (int i=0; i < page.length(); ) {
			int dtr = Math.min(page.length() - i, 16);
			int offset = i;
			if (page.nr() == 0) { // Page zero is special
				offset = 0xFF - page.length() + i + 1;
				dtr = 1;
			}
			if (D) Log.d(TAG, "Reading " + dtr + " bytes from page " + page.nr() + " at offset " + offset + " to local buffer at offset " + (page.start() + i));
			PDU response = sendPDU(PDU.getRequest(page.nr(), offset, dtr));
			if (response.getEEPromData().length != dtr) {
				throw new IOException("Requested " + dtr + " bytes from ECM but received " + response.getEEPromData().length);
			}
			System.arraycopy(response.getEEPromData(), 0, buffer, page.start() + i, dtr);
			i += dtr;
		}
	}

	public void writeEEPromPage(Page page) throws IOException {
		byte[] buffer = page.getParent().getBytes();
		for (int i=0; i < page.length(); ) {
			int dtr = Math.min(page.length() - i, 16);
			int offset = i;
			if (page.nr() == 0) { // Page zero is special
				offset = 0xFF - page.length() + i + 1;
				dtr = 1;
			}
			sendPDU(PDU.setRequest(page.nr(), offset, buffer, page.start() + offset, dtr));
			i += dtr;
		}
	}


	/**
	 * Request runtime data from ECM
	 * @return a byte[] holding the Runtime Data (payload only)
	 * @throws IOException if an I/O error occurred.
	 */
	public byte[] readRTData() throws IOException {
		PDU response = sendPDU(PDU.getRuntimeData());
		rtData = response.getBytes();
		return rtData;
	}

	private int read(byte[] buffer, int offset, int len, int timeout) throws IOException {
		// Log.d(TAG, "Trying to read " + len + " bytes to buffer at offset " + offset + "(timeout: " + timeout + ")");
		if (offset + len >= buffer.length) {
			throw new IOException(offset + len + ": Array index out of bounds.");
		}
		int r = 0;
		while (r < len && timeout > 0) {
			if (in.available() > 0) {
				do {
					int toRead = Math.min(len - r, in.available());
					try {
						int i = in.read(buffer, r + offset, toRead);
						if (i == -1) {
							throw new IOException("EOF while reading " + toRead + "/" + len + " bytes at offset " + (r + offset));
						}
						r += i;
					} catch (RuntimeException rte) {
						throw new IOException("Runtime Exception while reading " + toRead + " bytes at offset " + (r + offset));
					}
				} while (r < len && in.available() > 0);
			} else {
				try {
					Thread.sleep(10);
					timeout -= 10;
				} catch (InterruptedException e) {}
			}
		}
		// Log.d(TAG, "Bytes read in total: " + r + ", " + Utils.hexdump(buffer, 0, r));
		if (r != len) {
			throw new IOException("Timeout reading " + r + " from " + len + " bytes.");
		}
		return r;
	}

	public static synchronized ECM getInstance(Context ctx) {
		if (singleton == null) {
			singleton = new ECM(ctx);
		}
		return singleton;
	}

	public boolean isConnected() {
		return connected;
	}

	public EEPROM getEEPROM() {
		return eeprom;
	}

	public Collection<Error> getErrors(ErrorType type) throws IOException {
		String field = (type == ErrorType.CURRENT ? "CDiag%d" : "HDiag%d_LD");
		DataSource ds = DataSource.RUNTIME_DATA;
		if (getRealtimeData() == null && isConnected()) {
			readRTData();
		}

		byte[] data = getRealtimeData();
		if (data == null) {
			if (type == ErrorType.STORED && eeprom != null && eeprom.isEepromRead()) {
				if (D) Log.d(TAG, "No live data, falling back to EEPROM data for stored errors...");
				data = eeprom.getBytes();
				field = "HDiag%d";
				ds = DataSource.EEPROM;
			} else {
				return null;
			}
		}

		List<Error> errors = new LinkedList<Error>();
		if (data != null) {
			for (int i = 0; ; i++) {
				String f = String.format(field,i);
				BitSet bitset = bitsetProvider.getBitSet(this.eeprom.getId(), f, ds);
				if (bitset == null) {
					break;
				}
				if (ds == DataSource.EEPROM && bitset.getOffset() < 0 && !eeprom.hasPageZero()) {
					if (D) Log.d(TAG, f + ": Skipping troublecode variable at offset " + bitset.getOffset());
					continue;
				}
				if (D) Log.d(TAG, "Checking field " + f + "(offset " + bitset.getOffset() + ") for errors");
				for (Bit bit : bitset) {
					if (bit.refreshValue(data)) {
						Error e = new Error();
						e.setCode(bit.getCode());
						e.setType(type);
						e.setDescription(bit.getRemark());
						if (D) Log.d(TAG, "Error read: " + e);
						errors.add(e);
					}
				}
			}
		}
		return errors;
	}


	public void setRealtimeData(byte[] data) {
		rtData = data;
	}

	public byte[] getRealtimeData() {
		return rtData;
	}

	public Variable getRealtimeValue(String name) {
		if (this.eeprom == null) return null;
		Variable v = variableProvider.getRtVariable(getId(), name);
		if (v != null) {
			byte[] tmp = rtData;
			if (tmp != null) {
				v.refreshValue(tmp);
			}
		}
		return v;
	}

	public String getFormattedEEPROMValue(String name, String default_value) {
		Variable var = getEEPROMValue(name);
		if (var == null) {
			return default_value;
		}
		String ret = var.getFormattedValue();
		if (Utils.isEmptyString(ret)) {
			ret = default_value;
		}
		return ret;
	}

	public Variable getEEPROMValue(String name) {
		if (name == null || eeprom == null) {
			return null;
		}

		Variable var = this.variableProvider.getEEPROMVariable(getId(), name);
		if (var != null) {
			if (var.getOffset() < 0 && !eeprom.hasPageZero()) {
				return null;
			}
			var.refreshValue(eeprom.getBytes());
		}
		return var;
	}

	/**
	 * Get EEPROM Variable definition at given offset or just in front of it.
	 * @param offset the offset
	 * @return a Variable or null if not found
	 */
	public Variable getEEPROMValueNearOffset(int offset) {
		return variableProvider.getNearestEEPROMVariable(getId(), offset);
	}


	public Bit getEEPROMBit(String name, int bit) {
		BitSet bitset = bitsetProvider.getBitSet(getId(), name, DataSource.EEPROM);
		if (bitset != null) {
			Bit b = bitset.getBit(bit);
			if (b.getOffset() < 0 && !eeprom.hasPageZero()) {
				return null;
			}
			b.refreshValue(eeprom.getBytes());
			return b;
		}
		return null;
	}

	public Type getType() {
		return eeprom == null ? null : eeprom.getType();
	}

	public void setEEPROM(EEPROM eeprom) {
		this.eeprom = eeprom;
	}

	public String getId() {
		return eeprom == null ? null : eeprom.getId();
	}

	public void setRecording(boolean recording) {
		this.recording = recording;
	}

	public boolean isRecording() {
		return recording;
	}

	public String getSerialNo() {
		return getFormattedEEPROMValue(Variables.KMFG_Serial, UNKNOWN);
	}

	public String getMfgDate() {
		String year  = getFormattedEEPROMValue(Variables.KMFG_Year, null);
		String day   = getFormattedEEPROMValue(Variables.KMFG_Day, null);
		if (year != null && day != null) {
			int y = Integer.parseInt(year) + 2000;
			if (y >= 2090) {
				y -= 100; // 1990..99
			}
			int d = Integer.parseInt(day) + 1;
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, y);
			cal.set(Calendar.DAY_OF_YEAR, d);
			java.text.DateFormat df = DateFormat.getDateFormat(context);
			return df.format(cal.getTime());
		}
		return UNKNOWN;
	}

	public boolean isEepromRead() {
		return eeprom != null && eeprom.isEepromRead();
	}

	public boolean setEEPROMValue(Variable var) {
		try {
			var.updateValue(eeprom.getBytes());
			eeprom.touch();
		} catch (Exception e) {
			Log.w(TAG, "Unable to update value. " + e.getLocalizedMessage());
			return false;
		}
		return true;
	}

	public boolean setEEPROMBits(BitSet bitset) {
		if (bitset.updateValue(eeprom.getBytes())) {
			eeprom.touch();
		}
		return true;
	}

}
