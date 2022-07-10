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
package org.ecmdroid.fragments;

import android.app.Activity;
import android.app.Fragment;
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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ToggleButton;

import org.ecmdroid.Constants.Variables;
import org.ecmdroid.DataChannelAdapter;
import org.ecmdroid.ECM;
import org.ecmdroid.EcmDroidService;
import org.ecmdroid.R;
import org.ecmdroid.Utils;
import org.ecmdroid.Variable;
import org.ecmdroid.VariableProvider;
import org.ecmdroid.activities.MainActivity;

public class DataChannelFragment extends Fragment {

	private static final String TAG = "DataChannels";
	private static final String[] DEFAULT_CHANNELS = {
			Variables.Bat_V,
			Variables.RPM,
			Variables.TPD,
			Variables.AFV,
			Variables.CLT
	};
	private static Variable[] channels = new Variable[5];

	private ECM ecm = ECM.getInstance(getActivity());
	private VariableProvider provider = VariableProvider.getInstance(getActivity());
	private ToggleButton toggleButton;
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
			ecmDroidService = ((EcmDroidService.EcmDroidBinder) service).getService();
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

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.datachannels, container, false);
		ListView listView = (ListView) view.findViewById(R.id.dataChannelList);
		dataChannelAdapter = new DataChannelAdapter(getActivity(), ecm, channels);
		listView.setAdapter(dataChannelAdapter);
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_NONE);

		toggleButton = (ToggleButton) view.findViewById(R.id.toggleLiveChannels);
		toggleButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				boolean on = ((ToggleButton) v).isChecked();
				synchronized (DataChannelFragment.class) {
					if (on) {
						ecmDroidService.startReading();
						dataChannelAdapter.setAutoRefresh(true);
						getActivity().registerReceiver(receiver, new IntentFilter(EcmDroidService.REALTIME_DATA));
					} else {
						dataChannelAdapter.setAutoRefresh(false);
						ecmDroidService.stopReading();
						try {
							getActivity().unregisterReceiver(receiver);
						} catch (Exception unknown) {
						}
					}
				}
			}
		});
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		getActivity().bindService(new Intent(getActivity(), EcmDroidService.class), serviceConnection, Context.BIND_AUTO_CREATE);

		if (!Utils.isEmptyString(ecm.getId())) {
			SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
			int i = 0;
			for (String defvar : DEFAULT_CHANNELS) {
				String var = prefs.getString("channel" + (i + 1), defvar);
				if (!Utils.isEmptyString(var)) {
					Log.d(TAG, "Looking up variable " + var);
					channels[i] = provider.getRtVariable(ecm.getId(), var);
				}
				i++;
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		MainActivity activity = (MainActivity) getActivity();
		activity.setTitle(getString(R.string.data_channels));
		activity.updateConnectButton();
		toggleButton.setEnabled(ecmDroidService != null && ecm.isConnected());
		toggleButton.setChecked(ecmDroidService != null && ecmDroidService.isReading());
		if (ecmDroidService != null && ecmDroidService.isReading()) {
			dataChannelAdapter.setAutoRefresh(true);
			getActivity().registerReceiver(receiver, new IntentFilter(EcmDroidService.REALTIME_DATA));
		}
	}

	@Override
	public void onPause() {
		try {
			getActivity().unregisterReceiver(receiver);
		} catch (IllegalArgumentException iae) {
			// receiver not (yet) registered.
		}
		saveSettings();
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unbindService(serviceConnection);
	}

	private void saveSettings() {
		if (Utils.isEmptyString(ecm.getId())) {
			return;
		}
		SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
		Editor editor = prefs.edit();
		for (int i = 0; i < channels.length; i++) {
			String value = channels[i] == null ? "" : channels[i].getName();
			String key = "channel" + (i + 1);
			Log.d(TAG, "Saving " + key + "=" + value);
			editor.putString(key, value);
		}
		editor.apply();
	}
}
