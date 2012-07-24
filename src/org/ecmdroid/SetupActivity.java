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

import java.util.regex.Matcher;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class SetupActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private ECM ecm = ECM.getInstance(this);
	private VariableProvider provider = VariableProvider.getInstance(this);
	private static final String TAG = "SETUP";
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.ecm_setup);
		PreferenceScreen root = this.getPreferenceScreen();
		readPrefs(root);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return Utils.createOptionsMenu(this, menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Utils.prepareOptionsMenu(this, menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!Utils.handleOptionsItemSelected(this, item)) {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void readPrefs(PreferenceGroup group)
	{
		for (int i = 0; i < group.getPreferenceCount(); i++)
		{
			Preference s = group.getPreference(i);
			if (s instanceof PreferenceGroup) {
				readPrefs((PreferenceGroup) s);
				continue;
			}
			s.setPersistent(false);
			String key = s.getKey();
			if (key == null) {
				continue;
			}
			String title = null;
			Matcher m = Constants.BIT_PATTERN.matcher(key);
			if (m.matches()) {
				String name = m.group(1);
				String bits = m.group(2);
				int bits_set = 0, bits_unset = 0;
				boolean bits_missing = false;
				for (String nr : bits.split(",")) {
					int b = Integer.parseInt(nr);
					Bit bit = ecm.getEEPROMBit(name, b);
					if (bit == null) {
						Log.i(TAG, "Bit #" + b + " from bitset " + name + " not present for current ECM version.");
						bits_missing = true;
						break;
					}
					if (title == null) {
						title = bit.getName();
					}
					if (bit.isSet()) {
						bits_set++;
					} else {
						bits_unset++;
					}
				}
				if (bits_missing || (bits_set != 0 && bits_unset != 0) ) {
					if (!bits_missing) {
						Log.i(TAG, key +": Odd bit set detected (on: " + bits_set+", off: " + bits_unset + ").");
					}
					s.setEnabled(false);
				} else {
					if (s instanceof CheckBoxPreference) {
						CheckBoxPreference cb = (CheckBoxPreference) s;
						cb.setChecked(bits_set > 0);
					}
				}
			} else {
				Variable v = ecm.getEEPROMValue(key);
				if (v != null) {
					title = v.getName();
					s.setSummary(v.getFormattedValue());
					s.setDefaultValue(v.getRawValue());
				} else {
					s.setEnabled(false);
					Log.i(TAG, "EEPROM Variable '" + key + "' not present for current ECM version.");
				}
			}
			if (Utils.isEmptyString(s.getTitle())) {
				if (title == null) {
					title = provider.getName(key);
				}
				s.setTitle(title != null ? title : key);
			}
		}
	}

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		Log.d(TAG, "getSharedPreferences("+name+","+mode+")");
		SharedPreferences sp = super.getSharedPreferences(name, mode);
		sp.registerOnSharedPreferenceChangeListener(this);
		return sp;
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String arg) {
		Log.d(TAG, "Changed: " + arg);
	}

}
