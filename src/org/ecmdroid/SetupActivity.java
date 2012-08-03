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

import java.util.HashMap;
import java.util.regex.Matcher;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SetupActivity extends PreferenceActivity implements OnPreferenceChangeListener
{
	private static final String TAG = "SETUP";
	private ECM ecm = ECM.getInstance(this);
	private VariableProvider provider = VariableProvider.getInstance(this);
	private HashMap<Preference, Object> prefmap;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.ecm_setup);
		prefmap = new HashMap<Preference, Object>();
		new RefreshTask().execute();
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
			s.setOnPreferenceChangeListener(this);
			String title = null;
			Matcher m = Constants.BIT_PATTERN.matcher(key);
			if (m.matches()) {
				String name = m.group(1);
				String bits = m.group(2);
				int bits_set = 0, bits_unset = 0;
				boolean bits_missing = false;
				BitSet bitset = new BitSet(name, null, 0);
				for (String nr : bits.split(",")) {
					int b = Integer.parseInt(nr);
					Bit bit = ecm.getEEPROMBit(name, b);
					if (bit == null) {
						Log.i(TAG, "Bitset '" + name + "' not present in current ECM version.");
						bits_missing = true;
						break;
					}
					bitset.add(bit);
					bitset.setOffset(bit.getOffset());
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
					prefmap.put(s, bitset);
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
					if (s instanceof EditTextPreference) {
						((EditTextPreference) s).setText(v.getValueAsString());
						((EditTextPreference) s).getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
					}
					prefmap.put(s, v);
				} else {
					s.setEnabled(false);
					Log.i(TAG, "EEPROM Variable '" + key + "' not present in current ECM version.");
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

	private class RefreshTask extends AsyncTask<Void, String, Exception> {
		private int ro;
		private ProgressDialog mProgress;

		@Override
		protected void onPreExecute() {
			// Prevent screen rotation during progress dialog display
			ro = getRequestedOrientation();
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			mProgress = ProgressDialog.show(SetupActivity.this, "", getText(R.string.refreshing_setup_values), true);
		}

		@Override
		protected Exception doInBackground(Void... params) {
			@SuppressWarnings("deprecation")
			PreferenceScreen root = SetupActivity.this.getPreferenceScreen();
			readPrefs(root);
			return null;
		}

		@Override
		protected void onPostExecute(Exception result) {
			mProgress.dismiss();
			setRequestedOrientation(ro);
		}
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// Log.d(TAG, "Pref changed: " + preference + ", val: " + newValue + " [" + newValue.getClass().getName() + "]");
		Object var = prefmap.get(preference);
		if (var == null) return false;

		if (var instanceof Variable) {
			Variable v = (Variable) var;
			try {
				v.parseValue(newValue);
			} catch (NumberFormatException nfe) {
				Toast.makeText(this, newValue + ": Unable to parse number.", Toast.LENGTH_LONG).show();
				return false;
			}
			if (ecm.setEEPROMValue(v)) {
				preference.setSummary(v.getFormattedValue());
				return true;
			} else {
				Toast.makeText(this, "Error: Unable to set EEPROM value.", Toast.LENGTH_LONG).show();
				return false;
			}
		} else if (var instanceof BitSet) {
			BitSet bitset = (BitSet) var;
			bitset.setAll(Boolean.TRUE.equals(newValue));
			if (ecm.setEEPROMBits(bitset)) {
				return true;
			} else {
				Toast.makeText(this, "Error: Unable to set EEPROM bits.", Toast.LENGTH_LONG).show();
			}
		}
		return false;
	}
}
