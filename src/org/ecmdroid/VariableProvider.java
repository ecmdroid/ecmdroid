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

import java.util.Collection;

import android.content.Context;
import android.util.Log;

/**
 * Provider for Runtime- and EEPROM Variable Definitions and names.
 */
public abstract class VariableProvider
{
	protected static final String TAG = "VariableProvider";
	private static VariableProvider variableProvider;

	/**
	 * Get all available Runtime Data variable names for an ECM.
	 * @param ecm the ECMs ID (e.g. 'BUEIB')
	 * @return a collection of variable names or an Empty collection, if given ECM ID is unknown
	 */
	public abstract Collection<String> getRtVariableNames(String ecm);

	/**
	 * Get a Runtime Variable definition.
	 * @param ecm the ECMs ID (e.g. 'BUEIB')
	 * @param name the variable name (e.g. 'TPD')
	 * @return the variable or null if variable is unknown
	 */
	public abstract Variable getRtVariable(String ecm, String name);

	/**
	 * Get all available scalar Runtime Variable names for an ECM.
	 * @param ecm the ECMs ID (e.g. 'BUEIB')
	 * @return a collection of scalar variable names or an empty collection
	 */
	public abstract Collection<String> getScalarRtVariableNames(String ecm);

	/**
	 * Get all available bitfield runtime variable names for an ECM.
	 * @param ecm the ECM ID
	 * @return a collection of bitfield names, possibly empty.
	 */
	public abstract Collection<String> getBitfieldRtVariableNames(String ecm);

	/**
	 * Get a EEPROM Variable definition.
	 * @param ecm the ECMs ID (e.g. 'BUEIB')
	 * @param name the variable name (e.g. 'KTemp_Fan_On')
	 * @return the variable or null if variable is unknown
	 */
	public abstract Variable getEEPROMVariable(String ecm, String name);

	/**
	 * Get the descriptive name for a variable name or bit. For a specific bit,
	 * the bit number can be passed in square braces (e.g. 'KCF_Config[5]').
	 * <p>
	 * Example: Requesting 'KTemp_Fan_On' returns the string 'Fan Key-On On Temperature'.
	 * </p>
	 * @param varname the variable name (e.g. 'KTemp_Fan_On')
	 * @return the descriptive name or null if not found
	 */
	public abstract String getName(String varname);

	/**
	 * Get the descriptive name for a specific bit.
	 * @param varname the bitset variable  name (e.g. 'KCF_Config')
	 * @param bitnumber the bit number (0-7)
	 * @return the descriptive bit name or null if not found
	 */
	public abstract String getName(String varname, int bitnumber);

	/**
	 * Get EEPROM Variable definition at given offset or just in front of it (variable offset <= given offset).
	 * @param ecm the ECMs ID (e.g. 'BUEIB')
	 * @param offset variable offset
	 * @return a Variable definition or null
	 */
	public abstract Variable getNearestEEPROMVariable(String ecm, int offset);

	public static synchronized VariableProvider getInstance(Context ctx)
	{
		if (variableProvider == null) {
			try {
				long now = System.currentTimeMillis();
				variableProvider = new DatabaseVariableProvider(ctx);
				Log.d(TAG, "VariableParser took " + (System.currentTimeMillis() - now + "ms."));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return variableProvider;
	}
}