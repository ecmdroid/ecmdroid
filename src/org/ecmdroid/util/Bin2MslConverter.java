/*
 Bin2Msl Converter, Copyright (C) 2013 by Gunter Baumann

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
package org.ecmdroid.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Observable;
import java.util.regex.Pattern;

import android.util.Log;

/**
 * BIN to MSL (MegalogViewer) Logfile converter.
 */
public class Bin2MslConverter extends Observable
{
	private static final int OBSERVER_UPDATE_DELAY = 500;
	private static final String TAG = "BIN2MSL";

	private boolean cancelled = false;

	/**
	 * Convert a logfile from binary format into msl
	 * @param bin an input stream for the binary log
	 * @param msl an output stream receiving the converted data
	 * @throws IOException if an error occurs during conversion
	 * @since EcmDroid v0.9
	 */
	public void convert(InputStream bin, OutputStream msl) throws IOException
	{

		byte[] buffer5 = new byte[5];
		Integer[] rtBuffer = null;

		int i = 0;
		int engine = 0;
		int numRecord = 0;
		int numDiscard = 0;
		int offsAFV = 0;
		int offsAFV1 = 0;
		int offsEGOCorr = 0;
		int offsEGOCorr1 = 0;
		int offsFlags1 = 0;
		int offsFlags2 = 0;
		int offsAccel = 0;
		int offsWUE = 0;
		int rc = 0;
		int category = 0;
		int rtLen = 0;
		int offset = 0;
		int checkSum = 0;
		float fVal;
		int iVal;
		Locale loc = new Locale("en");

		String l;
		String ecmType;
		String tabFileName = "unknown";
		String[] fields = new String[18];

		BufferedReader offsetsTable = null;
		DataInputStream binFile = null;
		PrintWriter mslFile = null;

		Pattern p = Pattern.compile( "\t" );

		ArrayList<Integer> alCategory = new ArrayList<Integer>();
		ArrayList<String> alSecret = new ArrayList<String>();
		ArrayList<Integer> alSize = new ArrayList<Integer>();
		ArrayList<Integer> alOffset = new ArrayList<Integer>();
		ArrayList<Float> alScale = new ArrayList<Float>();
		ArrayList<Float> alTranslate = new ArrayList<Float>();
		ArrayList<String> alFormat = new ArrayList<String>();
		ArrayList<String> alExport = new ArrayList<String>();

		try {
			binFile = new DataInputStream(new BufferedInputStream(bin));
			mslFile = new PrintWriter(msl, false);

			try {
				rc = binFile.read(buffer5);
				Log.d(TAG, "read "+rc+" bytes from binFile");
			} catch (EOFException e) {
				Log.d(TAG, "unexpected EOF");
				binFile.close();
				throw e;
			}

			ecmType = new String(buffer5);

			if (ecmType.startsWith("KA")  || ecmType.contentEquals("BUEKA")) {
				Log.d(TAG, "Type: BUEKA");
				category = 1;
				rtLen = 99;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("JA")  || ecmType.contentEquals("BUEJA")) {
				Log.d(TAG, "Type: BUEJA");
				category = 1;
				rtLen = 99;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("CB")  || ecmType.contentEquals("BUECB")) {
				Log.d(TAG, "Type: BUECB");
				category = 2;
				rtLen = 103;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("GB") || ecmType.contentEquals("BUEGB")) {
				Log.d(TAG, "Type: BUEGB");
				category = 2;
				rtLen = 107;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("IB") || ecmType.startsWith("IC") || ecmType.contentEquals("BUEIB")|| ecmType.contentEquals("B2RIB") || ecmType.contentEquals("BUEIC")) {
				Log.d(TAG, "Type: BUEIB");
				category = 2;
				rtLen = 107;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("OD") ||ecmType.contentEquals("BUEOD")) {
				Log.d(TAG, "Type: BUEYD");
				category = 3;
				rtLen = 135;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("WD") ||ecmType.contentEquals("BUEWD")) {
				Log.d(TAG, "Type: BUEYD");
				category = 3;
				rtLen = 135;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("YD") ||ecmType.contentEquals("BUEYD")) {
				Log.d(TAG, "Type: BUEYD");
				category = 3;
				rtLen = 135;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("ZD") || ecmType.contentEquals("BUEZD")) {
				Log.d(TAG, "Type: BUEZD");
				category = 3;
				rtLen = 135;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("1D") || ecmType.contentEquals("BUE1D")|| ecmType.contentEquals("B3R1D")) {
				Log.d(TAG, "Type: BUE1D");
				category = 3;
				rtLen = 135;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("2D") || ecmType.contentEquals("BUE2D")) {
				Log.d(TAG, "Type: BUE2D");
				category = 3;
				rtLen = 135;
				rtBuffer = new Integer[rtLen];
			} else if (ecmType.startsWith("3D") || ecmType.contentEquals("BUE3D") || ecmType.contentEquals("B3R3D")) {
				Log.d(TAG, "Type: BUE3D");
				category = 3;
				rtLen = 135;
				rtBuffer = new Integer[rtLen];
			} else {
				Log.w(TAG, "unknown EcmType: "+ ecmType);
				binFile.close();
				mslFile.close();
				throw new IOException("Unsupported ECM Type: " + ecmType);
			}

			if (category == 1) {
				tabFileName="runtime1.tab";
			} else if (category == 2) {
				tabFileName="runtime2.tab";
			} else {
				tabFileName="runtime3.tab";
			}

			Log.d(TAG, "Table: "+ tabFileName);

			try {
				offsetsTable = new BufferedReader(new InputStreamReader(Bin2MslConverter.class.getResourceAsStream(tabFileName)));

				numRecord = 0;

				while ((l = offsetsTable.readLine()) != null) {
					// Log.d(TAG, "read line:>"+l+"<");

					fields = p.split(l);
					offset = Integer.parseInt(fields[7],10);
					//Log.d(TAG, "offset:"+fields[7]);

					if (offset > rtLen-3) {
						//Log.d(TAG, "skip!");
						continue;
					}

					alCategory.add(Integer.parseInt(fields[1],10));
					alSecret.add(fields[3]);
					alSize.add(Integer.parseInt(fields[6],10));
					alOffset.add(offset);
					alScale.add(Float.parseFloat(fields[9]));
					alTranslate.add(Float.parseFloat(fields[10]));
					alFormat.add(fields[12]);
					alExport.add(fields[17]);

					if (fields[17].equals("EGO Corr.")) {
						offsEGOCorr = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("EGO1 Corr.")) {
						offsEGOCorr1 = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("AFV")) {
						offsAFV = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("AFV1")) {
						offsAFV1 = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("sec") || fields[17].equals("Seconds")) {
					} else if (fields[17].equals("status57") || fields[17].equals("Flags1")) {
						offsFlags1 = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("status58") || fields[17].equals("Flags2")) {
						offsFlags2 = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("Accel Corr.")) {
						offsAccel = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("Decel Corr.")) {
					} else if (fields[17].equals("WUE")) {
						offsWUE = Integer.parseInt(fields[7],10);
					} else if (fields[17].equals("TPS deg.") || fields[17].equals("TPD") ) {
					}

					numRecord ++;
				}

			} finally {
				if (offsetsTable != null) {
					offsetsTable.close();
				}
			}

			mslFile.print("\"EcmDroid/Bin2Msl "+ecmType+"\"");
			mslFile.print("\r\n");

			if (category == 3) {
				mslFile.print("Number\tTime\tGego\tGego1\tEngine");
			} else {
				mslFile.print("Number\tTime\tGego\tEngine");
			}

			// Complete the Header
			for (i = 0 ; i < alExport.size(); i++) {
				mslFile.print("\t"+alExport.get(i));
			}
			mslFile.print("\r\n");

			long start = System.currentTimeMillis();
			try {

				numRecord = 1;

				StringBuilder rt = new StringBuilder(1024);
				NumberFormat df = DecimalFormat.getInstance(loc);
				df.setMaximumFractionDigits(3);
				df.setGroupingUsed(false);
				while (!cancelled) {
					rt.setLength(0);

					// timestamp
					iVal = binFile.readInt();
					fVal = iVal;

					// read RT data into buffer

					checkSum = 0;
					for (i = 0; i < rtLen ; i++) {
						rtBuffer[i] = binFile.readUnsignedByte();

						if (i > 0 && i < rtLen-1) {
							checkSum = checkSum ^ rtBuffer[i];
						}
					}

					// checksum comparison

					if (checkSum != rtBuffer[rtLen-1]) {
						numDiscard ++;
						continue;
					}

					rt.append(df.format(numRecord));
					rt.append(String.format(loc, "\t%.5f",fVal/100.0));  // timestamp

					// Gego calculation

					if (rtBuffer[offsFlags2] >= 128) {
						// closed loop
						iVal = rtBuffer[offsEGOCorr];
						iVal += rtBuffer[offsEGOCorr+1]*256;
						fVal = iVal;
					} else {
						// open loop
						iVal = rtBuffer[offsAFV];
						iVal += rtBuffer[offsAFV+1]*256;
						fVal = iVal;
					}

					rt.append('\t').append(df.format(fVal/10.0));  // Gego Corr.

					if (category == 3) {

						if (rtBuffer[offsFlags2] >= 128) {
							// closed loop
							iVal = rtBuffer[offsEGOCorr1];
							iVal += rtBuffer[offsEGOCorr1+1]*256;
							fVal = iVal;
						} else {
							// open loop
							iVal = rtBuffer[offsAFV1];
							iVal += rtBuffer[offsAFV1+1]*256;
							fVal = iVal;
						}

						rt.append('\t').append(df.format(fVal/10.0));  // Gego1 Corr.
					}

					// engine byte
					engine = 0;

					// from: http://www.msextra.com/forums/viewtopic.php?f=98&t=31106
					// running:equ     0       ; 0 = engine not running            1 = running
					// crank:  equ     1       ; 0 = engine not cranking           1 = engine cranking
					// ASE:    equ     2       ; 0 = not in after start enrichment 1 = in after start enrichment
					// warmup: equ     3       ; 0 = not in warmup                 1 = in warmup
					// tpsaen: equ     4       ; 0 = not in TPS acceleration mode  1 = TPS acceleration mode
					// tpsden: equ     5       ; 0 = not in deacceleration mode    1 = in deacceleration mode
					// mapaen: equ     6       ; 0 = not in MAP acceleration mode  1 = MAP deaceeleration mode
					// idleOn: equ     7       ;

					// Flags1: 0=Engine_Run | 1=O2_Active | 2=Accel | 3=Decel  | 4=Run_Stop  | 4=WOT   | 6=Idle        | 7=Ignition_On
					// Engine: 0=Running    | 1=Crank     | 2=ASE   | 3=Warmup | 4=TPS-Accel | 5=Decel | 6=(MAP-Accel) | 7=(Idle)


					// startup enrichment (should be covered by WUE also)
					//if (rtBuffer[offsSeconds] <=20) {
					//  engine = engine | 4;
					//}

					// get warmup enrich and accel enrich from correction values

					// warmup (> 2%)
					if ((rtBuffer[offsWUE] + rtBuffer[offsWUE+1]*256) >= 1020) {
						engine = engine | 8;
					}

					// acceleration enrichment (> 1%)
					// accel enrichment flag seems missing or too short,
					// so it disappears between the samples

					if ((rtBuffer[offsAccel] + rtBuffer[offsAccel+1]*256) >= 10) {
						engine = engine | 16;
					}

					iVal= rtBuffer[offsAccel] + rtBuffer[offsAccel+1]*256;

					// get states from flags1

					// Flags1: 0=Engine_Run | 1=O2_Active | 2=Accel | 3=Decel  | 4=Run_Stop  | 4=WOT   | 6=Idle        | 7=Ignition_On
					// Engine: 0=Running    | 1=Crank     | 2=ASE   | 3=Warmup | 4=TPS-Accel | 5=Decel | 6=(MAP-Accel) | 7=(Idle)

					iVal = rtBuffer[offsFlags1];

					// running - Flag1, bit 0
					if ((iVal & 1) > 0) {
						engine = engine | 1;
					}

					// accel - Flags1,  bit 2
					if ((iVal & 4) > 0) {
						engine = engine | 16;
					}

					// decel - Flags1, bit 3
					if ((iVal & 8) > 0) {
						engine = engine | 32;
					}

					// idle - Flags1, bit 6
					if ((iVal & 64) > 0) {
						engine = engine | 128;
					}

					rt.append('\t').append(engine);               // engine byte

					// runtime data
					for (i = 0 ; i < alOffset.size(); i++) {
						int o = alOffset.get(i);
						//if (alCategory.get(i) > category) {
						//  continue;
						//}
						iVal = rtBuffer[o];

						if (alSize.get(i) == 2) {
							iVal += rtBuffer[o+1] * 256;
						}

						fVal = iVal;
						fVal *= alScale.get(i);
						fVal += alTranslate.get(i);

						rt.append('\t');

						if (alFormat.get(i).equals("0")) {
							rt.append(df.format((int)fVal));
						} else {
							rt.append(df.format(fVal));
						}
					}
					rt.append("\r\n");
					mslFile.append(rt);
					numRecord ++;

					if (countObservers() > 0) {
						long now = System.currentTimeMillis();
						if (start + OBSERVER_UPDATE_DELAY <= now) {
							setChanged();
							notifyObservers(String.format("%d log records converted", numRecord));
							start = now;
						}
					}
				}
			} catch (EOFException e) {
				String s = String.format(Locale.ENGLISH, "Conversion finished. %d of %d records discarded.", numDiscard, numRecord);
				Log.i(TAG, s);
				setChanged();
				notifyObservers(s);
			}
		} finally {
			if (binFile != null) {
				binFile.close();
			}
			if (mslFile != null) {
				mslFile.flush();
				mslFile.close();
			}
		}
	}

	/**
	 * Cancel the conversion.
	 */
	public void cancel() {
		cancelled = true;
	}
}
