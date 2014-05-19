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
import java.text.DecimalFormat;

import android.util.Log;


public class Variable implements Cloneable {
	public enum DataClass {
		SCALAR, VALUE, BITS, BITFIELD, ARRAY, AXIS, TABLE, MAP, STRING;
	}

	private static final String TAG = "Variable";
	private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0");

	private int id;
	private ECM.Type type;
	private String name;
	private DataClass cls;
	private int size;
	private int width;
	private int rows;
	private int cols;
	private int offset;
	private String unit = "";
	private String symbol = "";
	private double scale;
	private double translate;
	private String label;
	private String format;
	private DecimalFormat formatter = DEFAULT_FORMAT;
	private double low;
	private double high;
	private int ulow;
	private int uhigh;
	private String remarks;
	private Object[] rawValues;
	private String[] formattedValues;
	private String description;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ECM.Type getType() {
		return type;
	}

	public void setType(ECM.Type type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DataClass getCls() {
		return cls;
	}

	public void setCls(DataClass cls) {
		this.cls = cls;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public int getElementCount() {
		return rawValues == null ? 0 : rawValues.length;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public void init() {
		formattedValues = new String[size / width];
		rawValues = new Object[formattedValues.length];
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public double getTranslate() {
		return translate;
	}

	public void setTranslate(double translate) {
		this.translate = translate;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
		if (format != null) {
			formatter = new DecimalFormat(format);
		}
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public double getHigh() {
		// If uhigh is not max, high must be translated.
		if ((size == 1 && uhigh == 0xFF) || (size == 2 && uhigh == 0xFFFF)) {
			return high * scale + translate;
		}
		return high;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public int getUlow() {
		return ulow;
	}

	public void setUlow(int ulow) {
		this.ulow = ulow;
	}

	public int getUhigh() {
		return uhigh;
	}

	public void setUhigh(int uhigh) {
		this.uhigh = uhigh;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public Object getRawValue() {
		return rawValues[0];
	}

	public Object getRawValueAt(int index) {
		return rawValues[index];
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setFormattedValue(String formattedValue) {
		formattedValues[0] = formattedValue;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("Variable[id: ").append(id).append(", name:");sb.append(name);
		sb.append(", ECM: ").append(type).append(", class: ").append(cls);
		sb.append(", size: ").append(size).append(", offset: ").append(offset);
		sb.append(", unit: ").append(unit).append(", scale:").append(scale);
		sb.append(", trn: ").append(translate).append(", high: ").append(high);
		sb.append("]");
		return sb.toString();
	}

	public Variable refreshValue(byte[] tmp) {
		int co = offset < 0 ? tmp.length + offset : offset;
		if (tmp != null && co >= 0 && co + size <= tmp.length) {
			for (int s = 0; s < (size / width); s++) {
				int value = 0;
				for (int i = width; i >0; i--) {
					value <<= 8;
					value |= (tmp[co + s * width + i - 1] & 0xff);
				}
				if (cls == DataClass.BITS || cls == DataClass.BITFIELD) {
					rawValues[s] = new Short((short) (value & 0xffff));
				} else if (cls != DataClass.STRING) {
					double v = value;
					if (scale != 0) {
						v *= scale;
					}
					if (translate != 0) {
						v += translate;
					}
					rawValues[s] = Double.valueOf(v);
					if ("0".equals(format)) {
						rawValues[s] = Integer.valueOf(((Double)rawValues[s]).intValue());
					}
				} else if (cls == DataClass.STRING) {
					byte[] bytes = new byte[size];
					System.arraycopy(tmp, co, bytes, 0, size);
					rawValues[s] = bytes;
				} else {
					Log.w(TAG, "Unsupported class " + cls);
				}
				formatValueAt(s);
			}
		}
		return this;
	}

	public String getFormattedValue() {
		return formattedValues[0];
	}


	public String getFormattedValueAt(int index) throws ArrayIndexOutOfBoundsException {
		return formattedValues[index];
	}

	public Object getFormattedValueAt(int row, int col) {
		return formattedValues[row * cols + col];
	}

	public String getValueAsString() {
		if (cls == DataClass.BITS || cls == DataClass.BITFIELD) {
			return getFormattedValue();
		}
		return formatter.format(rawValues[0]);
	}

	public int getIntValue() {
		return getIntValueAt(0);
	}

	public int getIntValueAt(int row, int col) {
		return getIntValueAt(row * cols + col);
	}

	public int getIntValueAt(int index) {
		if (cls == DataClass.BITFIELD || cls == DataClass.BITS) {
			return ((Short)rawValues[index]).intValue();
		}

		if (rawValues[index] != null) {
			if (rawValues[index] instanceof Integer) {
				return ((Integer)rawValues[index]).intValue();
			} else {
				return ((Double)rawValues[index]).intValue();
			}
		}
		return 0;
	}

	public void updateValue(byte[] bytes) throws IOException {
		if (rawValues[0] == null) {
			return;
		}
		int co = offset < 0 ? bytes.length + offset : offset;
		byte[] buffer = new byte[size];
		for (int s = 0; s < (size / width); s++) {
			int value = 0;
			if (cls == DataClass.BITFIELD || cls == DataClass.BITS) {
				value = (Short) rawValues[0] & 0xFFFF;
			} else if (cls != DataClass.STRING) {
				double v = 0;
				if (rawValues[s] instanceof Double) {
					v = (Double) rawValues[s];
				} else {
					v = (Integer) rawValues[s];
				}
				if (translate != 0) {
					v -= translate;
				}
				if (scale != 0) {
					v /= scale;
				}
				value = (int) v;

			} else {
				Log.w(TAG, "Unsupported class " + cls);
				return;
			}

			for (int i = 0; i < width; i++) {
				buffer[i + s * width] = (byte) (value & 0xFF);
				value >>= 8;
			}
		}
		Log.d(TAG, String.format("Setting buffer (len: %X) at offset 0x%02X (raw: 0x%02X) to %s (width: %d).", bytes.length, co, offset, Utils.hexdump(buffer), size));
		System.arraycopy(buffer, 0, bytes, co, size);
		Log.d(TAG, String.format("Result: " + Utils.hexdump(bytes, co, co + size)));
	}

	public void parseValue(Object value) throws NumberFormatException {
		parseValueAt(0, value);
	}

	public void parseValueAt(int index, Object value) throws NumberFormatException {
		if (value != null) {
			double v = Double.valueOf(value.toString()).doubleValue();
			rawValues[index] = Double.valueOf(v);

			if ("0".equals(format)) {
				rawValues[index] = Integer.valueOf(((Double)rawValues[index]).intValue());
			}
			formatValueAt(index);
		}
	}

	public void parseValueAt(int row, int col, Object value) {
		parseValueAt(row * cols + col, value);
	}

	private void formatValueAt(int index) {
		if (cls == DataClass.BITS || cls == DataClass.BITFIELD) {
			Short v = (Short) rawValues[index];
			formattedValues[index] = Integer.toBinaryString(0x100 | v).substring(1);
		} else if (cls == DataClass.STRING) {
			byte[] raw = (byte[]) rawValues[index];
			int len = raw.length;
			for (int i = 0; i < raw.length; i++) {
				if (raw[i] == 0) {
					len = i;
					break;
				}
			}
			formattedValues[index] = new String(raw, 0, len);
		} else {
			formattedValues[index] = formatter.format(rawValues[index]);
			if (!Utils.isEmptyString(symbol)) {
				formattedValues[index] += symbol;
			}
		}
	}

}
