/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2012 by Michel Marti

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.ecmdroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;

import org.ecmdroid.R;
import org.ecmdroid.Utils;

/**
 * Application Preferences
 */
public class PrefsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	private static final int STORAGE_LOCATION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.app_prefs);

		ListPreference list = (ListPreference) findPreference("connection_type");
		boolean tcp = getString(R.string.prefs_tcp_connection).equals(list.getValue());
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

		Preference storage = findPreference("storage_location");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String storageLocation = prefs.getString("storage.location", null);
		storage.setSummary(storageLocation == null ? getString(R.string.setup_storage_hint) : Uri.parse(storageLocation).getLastPathSegment());
		storage.setOnPreferenceClickListener(preference -> {
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
			startActivityForResult(intent, STORAGE_LOCATION);
			return true;
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == STORAGE_LOCATION && resultCode == Activity.RESULT_OK) {
			Uri uri;
			if (data != null) {
				uri = data.getData();
				if (uri != null) {
					getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
							| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString("storage.location", uri.toString());
					editor.apply();
					Preference storage = findPreference("storage_location");
					storage.setSummary(uri.getLastPathSegment());
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return Utils.createOptionsMenu(this, menu);
	}

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
