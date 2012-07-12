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

import java.text.DecimalFormat;

import android.util.Log;


public class Variable implements Cloneable {
	public enum Class {
		SCALAR, VALUE, BITS, BITFIELD, ARRAY, AXIS, TABLE, MAP;
	}

	private static final String TAG = "Variable";

	private int id;
	private ECM.Type type;
	private String name;
	private Class cls;
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

	public Class getCls() {
		return cls;
	}

	public void setCls(Class cls) {
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
			if (cls == Class.BITS || cls == Class.BITFIELD) {
				String bs = Integer.toBinaryString(value);
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < ((width * 8) - bs.length()); i++) {
					sb.append('0');
				}
				formattedValue = sb.append(bs).toString();
				rawValue = new Short((short) (value & 0xffff));
			} else if (cls == Class.SCALAR || cls == Class.VALUE) {
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
				DecimalFormat fmt = new DecimalFormat(format == null ? "0" : format);
				formattedValue = fmt.format(rawValue);
				if (!Utils.isEmpty(symbol)) {
					formattedValue += symbol;
				}
			} else {
				Log.w(TAG, "Unsupported class " + cls);
			}
		}
		return this;
	}

	public String getFormattedValue() {
		return formattedValue;
	}

	public Object rawValue() {
		return rawValue;
	}

	public int getIntValue() {
		if (cls == Class.BITFIELD || cls == Class.BITS) {
			return ((Short)rawValue).intValue();
		}

		if (cls == Class.SCALAR || cls == Class.VALUE) {
			if (rawValue instanceof Integer) {
				return ((Integer)rawValue).intValue();
			} else {
				return ((Double)rawValue).intValue();
			}
		}
		return 0;
	}
}
