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

import org.ecmdroid.Constants.Variables;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ToggleButton;

public class DataChannelActivity extends BaseActivity {

	private static final String TAG = "DataChannels";
	private static Variable[] channels = new Variable[4];

	private ECM ecm = ECM.getInstance(this);
	private VariableProvider provider = VariableProvider.getInstance(this);
	private ToggleButton toggleButton;
	private ListView listView;
	private DataChannelAdapter dataChannelAdapter;
	private EcmDroidService ecmDroidService;

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Disconnected from Service");
			ecmDroidService = null;
			toggleButton.setEnabled(false);
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Connected to Service");
			ecmDroidService = ((EcmDroidService.EcmDroidBinder)service).getService();
			toggleButton.setEnabled(ecm.isConnected());
			toggleButton.setChecked(ecmDroidService.isReading());
		}
	};


	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			dataChannelAdapter.notifyDataSetChanged();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,"onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.datachannels);
		bindService(new Intent(this, EcmDroidService.class), serviceConnection, Context.BIND_AUTO_CREATE);

		if (!Utils.isEmptyString(ecm.getId())) {
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			int i = 0;
			for (String defvar : new String[] {Variables.RPM, Variables.TPD,Variables.CLT,Variables.AFV}) {
				String var = prefs.getString("channel" + (i+1), defvar);
				if (!Utils.isEmptyString(var)) {
					Log.d(TAG, "Looking up variable " + var);
					channels[i] = provider.getRtVariable(ecm.getId(), var);
				}
				i++;
			}
		}

		listView = (ListView) findViewById(R.id.dataChannelList);
		dataChannelAdapter = new DataChannelAdapter(this, ecm, channels);
		listView.setAdapter(dataChannelAdapter);
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_NONE);

		toggleButton = (ToggleButton) this.findViewById(R.id.toggleLiveChannels);
		toggleButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				boolean on = ((ToggleButton)v).isChecked();
				synchronized(DataChannelActivity.class) {
					if (on) {
						ecmDroidService.startReading();
						dataChannelAdapter.setAutoRefresh(true);
						registerReceiver(receiver, new IntentFilter(EcmDroidService.REALTIME_DATA));
					} else {
						dataChannelAdapter.setAutoRefresh(false);
						ecmDroidService.stopReading();
						try {
							unregisterReceiver(receiver);
						} catch (Exception unknown){}
					}
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		toggleButton.setEnabled(ecmDroidService != null && ecm.isConnected());
		toggleButton.setChecked(ecmDroidService != null && ecmDroidService.isReading());
		if (ecmDroidService != null && ecmDroidService.isReading()) {
			dataChannelAdapter.setAutoRefresh(true);
			registerReceiver(receiver, new IntentFilter(EcmDroidService.REALTIME_DATA));
		}
	}

	@Override
	protected void onPause() {
		try {
			unregisterReceiver(receiver);
		} catch (Exception unknown){}
		saveSettings();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
	}
	private void saveSettings() {
		if (Utils.isEmptyString(ecm.getId())) {
			return;
		}
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		Editor editor = prefs.edit();
		for (int i=0; i < channels.length; i++) {
			String value = channels[i] == null ? "" : channels[i].getName();
			String key = "channel" + (i+1);
			Log.d(TAG, "Saving " + key + "=" + value);
			editor.putString(key, value);
		}
		editor.commit();
	}
}
