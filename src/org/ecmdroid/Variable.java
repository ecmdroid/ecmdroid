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

	private int id;
	private ECM.Type type;
	private String name;
	private DataClass cls;
	private int width;
	private int offset;
	private String unit = "";
	private String symbol = "";
	private double scale;
	private double translate;
	private String label;
	private String format;
	private double low;
	private double high;
	private int ulow;
	private int uhigh;
	private String remarks;
	private String formattedValue;
	private Object rawValue;
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

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
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
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public double getHigh() {
		// If uhigh is not max, high must be translated.
		if ((width == 1 && uhigh == 0xFF) || (width == 2 && uhigh == 0xFFFF)) {
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
		return rawValue;
	}

	public void setRawValue(Object rawValue) {
		this.rawValue = rawValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setFormattedValue(String formattedValue) {
		this.formattedValue = formattedValue;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("Variable[id: ").append(id).append(", name:");sb.append(name);
		sb.append(", ECM: ").append(type).append(", class: ").append(cls);
		sb.append(", width: ").append(width).append(", offset: ").append(offset);
		sb.append(", unit: ").append(unit).append(", scale:").append(scale);
		sb.append(", trn: ").append(translate).append(", high: ").append(high);
		sb.append("]");
		return sb.toString();
	}

	public Variable refreshValue(byte[] tmp) {
		int co = offset < 0 ? tmp.length + offset : offset;
		if (tmp != null && co >= 0 && co + this.width <= tmp.length) {
			int value = 0;
			for (int i = this.width; i >0; i--) {
				value <<= 8;
				value |= (tmp[co + i - 1] & 0xff);
			}
			if (cls == DataClass.BITS || cls == DataClass.BITFIELD) {
				rawValue = new Short((short) (value & 0xffff));
			} else if (cls == DataClass.SCALAR || cls == DataClass.VALUE) {
				double v = value;
				if (scale != 0) {
					v *= scale;
				}
				if (translate != 0) {
					v += translate;
				}
				rawValue = Double.valueOf(v);
				if ("0".equals(format)) {
					rawValue = Integer.valueOf(((Double)rawValue).intValue());
				}
			} else {
				Log.w(TAG, "Unsupported class " + cls);
			}
			formatValue();
		}
		return this;
	}

	private void formatValue() {
		if (cls == DataClass.BITS || cls == DataClass.BITFIELD) {
			Short v = (Short) rawValue();
			formattedValue = Integer.toBinaryString(v);
		} else if (cls == DataClass.SCALAR || cls == DataClass.VALUE) {
			DecimalFormat fmt = new DecimalFormat(format == null ? "0" : format);
			formattedValue = fmt.format(rawValue);
			if (!Utils.isEmptyString(symbol)) {
				formattedValue += symbol;
			}
		}
	}

	public String getFormattedValue() {
		return formattedValue;
	}

	public Object rawValue() {
		return rawValue;
	}

	public int getIntValue() {
		if (cls == DataClass.BITFIELD || cls == DataClass.BITS) {
			return ((Short)rawValue).intValue();
		}

		if ((cls == DataClass.SCALAR || cls == DataClass.VALUE) && rawValue != null) {
			if (rawValue instanceof Integer) {
				return ((Integer)rawValue).intValue();
			} else {
				return ((Double)rawValue).intValue();
			}
		}
		return 0;
	}

	public void updateValue(byte[] bytes) throws IOException {
		if (rawValue == null) {
			return;
		}
		int co = offset < 0 ? bytes.length + offset : offset;
		byte[] buffer = new byte[width];
		int value = 0;
		if (cls == DataClass.BITFIELD || cls == DataClass.BITS) {
			value = (Short) rawValue & 0xFFFF;
		} else if (cls == DataClass.SCALAR || cls == DataClass.VALUE) {
			double v = 0;
			if (rawValue instanceof Double) {
				v = (Double) rawValue;
			} else {
				v = (Integer) rawValue;
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
			buffer[i] = (byte) (value & 0xFF);
			value >>= 8;
		}
		Log.d(TAG, String.format("Setting buffer (len: %X) at offset 0x%02X (raw: 0x%02X) to %s (width: %d).", bytes.length, co, offset, Utils.hexdump(buffer), width));
		System.arraycopy(buffer, 0, bytes, co, width);
		Log.d(TAG, String.format("Result: " + Utils.hexdump(bytes, co, co + width)));
	}

	public void parseValue(Object value) throws NumberFormatException {
		if (value != null) {
			double v = Double.valueOf(value.toString()).doubleValue();
			rawValue = Double.valueOf(v);

			if ("0".equals(format)) {
				if ("0".equals(format)) {
					rawValue = Integer.valueOf(((Double)rawValue).intValue());
				}
			}
			formatValue();
		}
	}
}
