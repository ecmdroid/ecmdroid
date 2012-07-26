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

import org.ecmdroid.ECM.Type;

public class Bit
{
	private int id;
	private Type type;
	private int offset;
	private int byteNr;
	private int bitNr;
	private String code;
	private String name;
	private String remark;
	private byte value;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getByteNr() {
		return byteNr;
	}

	public void setByteNr(int byte_nr) {
		this.byteNr = byte_nr;
	}

	public int getBitNr() {
		return bitNr;
	}

	public void setBitNr(int bit) {
		this.bitNr = bit;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public byte getValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = (byte) (value == false ? 0 : (1 << bitNr) & 0xff);
	}

	public boolean refreshValue(byte[] data) {
		if (offset >= data.length) {
			value = 0;
			return false;
		}
		byte mask = (byte) (1 << bitNr);
		value = (byte) ((data[offset] & 0xff) & mask);
		return value != 0;
	}

	public boolean isSet() {
		return value != 0;
	}

	@Override
	public String toString() {
		return "BitSet[name: " + name + ", remark: " + remark + ", ECM: " + type + ", offset: " + offset + ", byte: " + byteNr + ", bit: " + bitNr + ", code: " + code + "]";
	}

}
