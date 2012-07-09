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

public abstract class VariableProvider
{
	protected static final String TAG = "VariableProvider";
	private static VariableProvider variableProvider;

	public abstract Collection<String> getRtVariableNames(String ecm);
	public abstract Variable getRtVariable(String ecm, String name);
	public abstract Collection<String> getScalarRtVariableNames(String ecm);
	public abstract Variable getEEPROMVariable(String ecm, String name);

	public static synchronized VariableProvider getInstance(Context ctx)  {
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