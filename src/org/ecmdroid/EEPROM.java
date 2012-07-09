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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EEPROM {

	private static Map<String, EEPROM> eeproms = new HashMap<String, EEPROM>();

	static {
		eeproms.put ("BUEIA", new EEPROM("BUEIA", ECM.Type.DDFI1, 256, 256, 32, 256, 256));
		eeproms.put ("BUEGC", new EEPROM("BUEGC", ECM.Type.DDFI1, 256, 256, 76, 256, 256));
		eeproms.put ("BUEKA", new EEPROM("BUEKA", ECM.Type.DDFI1, 256, 256, 48, 256, 256));
		eeproms.put ("BUECB", new EEPROM("BUECB", ECM.Type.DDFI2, 256, 256, 90, 256, 256, 24));
		eeproms.put ("BUEGB", new EEPROM("BUEGB", ECM.Type.DDFI2, 256, 256, 150, 256, 256, 24));
		eeproms.put ("BUEIB", new EEPROM("BUEIB", ECM.Type.DDFI2, 256, 256, 158, 256, 256, 24));
		eeproms.put ("B2RIB", new EEPROM("B2RIB", ECM.Type.DDFI2, 256, 256, 158, 256, 256, 24));
		eeproms.put ("BUE1D", new EEPROM("BUE1D", ECM.Type.DDFI3, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 76));
		eeproms.put ("BUE2D", new EEPROM("BUE2D", ECM.Type.DDFI3, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 172));
		eeproms.put ("BUE3D", new EEPROM("BUE3D", ECM.Type.DDFI3, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 44));
		eeproms.put ("BUEOD", new EEPROM("BUEOD", ECM.Type.DDFI3, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 228));
		eeproms.put ("BUEWD", new EEPROM("BUEWD", ECM.Type.DDFI3, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 44));
		eeproms.put ("BUEYD", new EEPROM("BUEYD", ECM.Type.DDFI3, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 160));
		eeproms.put ("BUEZD", new EEPROM("BUEZD", ECM.Type.DDFI3, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 256, 44));
	}

	private ECM.Type type;
	private String id;
	private String version;
	private ArrayList<Page> pages;
	private int length = 0;
	private byte[] data;

	private EEPROM(String id, ECM.Type type, Integer... pagesizes)
	{
		this.id = id;
		this.type = type;
		int i=1;
		pages = new ArrayList<Page>();
		for (Integer ps : pagesizes) {
			Page pg = new Page(i++, ps.intValue());
			pg.setStart(length);
			pg.parent = this;
			pages.add(pg);
			length += ps.intValue();
		}
		data = new byte[length];
	}

	public int length() {
		return length;
	}


	public byte[] getBytes() {
		return data;
	}

	public Collection<Page> getPages() {
		return pages;
	}

	public String getId() {
		return id;
	}

	public ECM.Type getType() {
		return type;
	}
	@Override
	public String toString() {
		return "EEPROM[id: " + id + ", type: " + type + ", version: " + version +", length: " + length + ", number of pages: " + pages.size() + "]";
	}

	public class Page {
		private int nr;
		private int length;
		private int start;
		private EEPROM parent;

		private Page(int nr, int length) {
			this.nr = nr;
			this.length = length;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int nr() {
			return nr;
		}

		public int start() {
			return start;
		}

		public int length() {
			return length;
		}

		public EEPROM getParent() {
			return parent;
		}
	}

	public int getPageCount() {
		return pages.size();
	}

	public static EEPROM get(String ecmtype)
	{
		return eeproms.get(ecmtype == null ? "" : ecmtype.substring(0, Math.min(ecmtype.length(), 5)));
	}

	public Page getPage(int pageno) {
		return pages.get(pageno - 1);
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
