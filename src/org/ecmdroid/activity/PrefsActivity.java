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

import org.ecmdroid.R;
import org.ecmdroid.Utils;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Application Preferences
 */
public class PrefsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.app_prefs);

		ListPreference list = (ListPreference) findPreference("connection_type");
		boolean tcp  = getString(R.string.prefs_tcp_connection).equals(list.getValue());
		list.setSummary(list.getEntry());
		list.setOnPreferenceChangeListener(this);

		EditTextPreference txt = (EditTextPreference) this.findPreference("tcp_port");
		txt.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
		txt.setSummary(txt.getText());
		txt.setEnabled(tcp);
		txt.setOnPreferenceChangeListener(this);

		txt = (EditTextPreference) this.findPreference("tcp_host");
		txt.setEnabled(tcp);
		txt.setSummary(txt.getText());
		txt.setOnPreferenceChangeListener(this);

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

	@SuppressWarnings("deprecation")
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference instanceof ListPreference) {
			ListPreference lp = (ListPreference) preference;
			int idx = lp.findIndexOfValue((String) newValue);
			preference.setSummary(lp.getEntries()[idx]);
			boolean tcp = getString(R.string.prefs_tcp_connection).equals(newValue);
			findPreference("tcp_host").setEnabled(tcp);
			findPreference("tcp_port").setEnabled(tcp);
		} else {
			preference.setSummary(newValue == null ? "" : newValue.toString());
		}
		return true;
	}
}
