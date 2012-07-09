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
	private int byte_nr;
	private int bit;
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

	public int getByte_nr() {
		return byte_nr;
	}

	public void setByte_nr(int byte_nr) {
		this.byte_nr = byte_nr;
	}

	public int getBit() {
		return bit;
	}

	public void setBit(int bit) {
		this.bit = bit;
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

	public void setValue(byte value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "BitSet[name: " + name + ", remark: " + remark + ", ECM: " + type + ", offset: " + offset + ", byte: " + byte_nr + ", bit: " + bit + ", code: " + code + "]";
	}

	public boolean isSet(byte[] data) {
		if (offset >= data.length) {
			return false;
		}
		byte mask = (byte) (1 << bit);
		return (data[offset] & mask) != 0;
	}
}
