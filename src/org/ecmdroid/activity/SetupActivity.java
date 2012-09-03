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
package org.ecmdroid.activity;

import java.util.HashMap;
import java.util.regex.Matcher;

import org.ecmdroid.Bit;
import org.ecmdroid.BitSet;
import org.ecmdroid.Constants;
import org.ecmdroid.ECM;
import org.ecmdroid.R;
import org.ecmdroid.Utils;
import org.ecmdroid.Variable;
import org.ecmdroid.VariableProvider;
import org.ecmdroid.task.ProgressDialogTask;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SetupActivity extends PreferenceActivity implements OnPreferenceChangeListener
{
	private static final String TAG = "SETUP";
	private ECM ecm = ECM.getInstance(this);
	private VariableProvider provider = VariableProvider.getInstance(this);
	private HashMap<Preference, Object> prefmap;
	private Button saveButton;
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.ecm_setup);
		setContentView(R.layout.setup);
		prefmap = new HashMap<Preference, Object>();
		saveButton = (Button) findViewById(R.id.applyChanges);
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(SetupActivity.this, EEPROMActivity.class);
				intent.setAction(EEPROMActivity.ACTION_BURN);
				startActivity(intent);
			}
		});
		new RefreshTask().execute();
	}

	@Override
	protected void onResume() {
		saveButton.setVisibility(ecm.isConnected() && ecm.getEEPROM().isTouched() ? View.VISIBLE : View.GONE);
		super.onResume();
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

	private int readPrefs(PreferenceGroup group, boolean hideMissing)
	{
		for (int i = 0; i < group.getPreferenceCount(); i++)
		{
			Preference pref = group.getPreference(i);
			if (pref instanceof PreferenceGroup) {
				int pc = readPrefs((PreferenceGroup) pref, hideMissing);
				if (pc < 1) {
					group.removePreference(pref);
				}
				continue;
			}

			pref.setPersistent(false);
			pref.setOnPreferenceChangeListener(this);
			boolean setup = false;
			for (String key : pref.getKey().split(":")) {
				pref.setKey(key.trim());
				if (setupPreference(pref)) {
					setup = true;
					break;
				}
			}
			if (!setup) {
				if (hideMissing) {
					if (group.removePreference(pref)) {
						i--;
						continue;
					}
				}
				pref.setEnabled(false);
			}
		}
		return group.getPreferenceCount();
	}

	private boolean setupPreference(Preference pref) {
		boolean result = true;
		String title = null;
		String key = pref.getKey();
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
				result = false;
			} else {
				prefmap.put(pref, bitset);
				if (pref instanceof CheckBoxPreference) {
					CheckBoxPreference cb = (CheckBoxPreference) pref;
					cb.setChecked(bits_set > 0);
				}
			}
		} else {
			Variable v = ecm.getEEPROMValue(key);
			if (v != null) {
				title = v.getName();
				pref.setSummary(v.getFormattedValue());
				if (pref instanceof EditTextPreference) {
					((EditTextPreference) pref).setText(v.getValueAsString());
					((EditTextPreference) pref).getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				}
				prefmap.put(pref, v);
			} else {
				Log.i(TAG, "EEPROM Variable '" + key + "' not present in current ECM version.");
				result = false;
			}
		}
		if (Utils.isEmptyString(pref.getTitle())) {
			if (title == null) {
				title = provider.getName(key);
			}
			pref.setTitle(title != null ? title : key);
		}
		return result;
	}

	private class RefreshTask extends ProgressDialogTask {

		public RefreshTask() {
			super(SetupActivity.this, "");
		}

		@SuppressWarnings("deprecation")
		@Override
		protected Exception doInBackground(Void... params) {
			publishProgress(getText(R.string.refreshing_setup_values).toString());
			PreferenceScreen root = SetupActivity.this.getPreferenceScreen();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SetupActivity.this);
			boolean hideMissing = ecm.isEepromRead() ? prefs.getBoolean("hide_nonexistent_vars", false) : false;
			readPrefs(root, hideMissing);
			return null;
		}
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		Log.d(TAG, "Variable '" + preference.getKey() + "' changed. val: " + newValue + " [" + newValue.getClass().getName() + "]");
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
				saveButton.setVisibility(ecm.isConnected() ? View.VISIBLE : View.GONE);
				return true;
			} else {
				Toast.makeText(this, "Error: Unable to set EEPROM value.", Toast.LENGTH_LONG).show();
				return false;
			}
		} else if (var instanceof BitSet) {
			BitSet bitset = (BitSet) var;
			bitset.setAll(Boolean.TRUE.equals(newValue));
			if (ecm.setEEPROMBits(bitset)) {
				saveButton.setVisibility(ecm.isConnected() ? View.VISIBLE : View.GONE);
				return true;
			} else {
				Toast.makeText(this, "Error: Unable to set EEPROM bits.", Toast.LENGTH_LONG).show();
			}
		}
		return false;
	}
}
