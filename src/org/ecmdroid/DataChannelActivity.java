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

import java.io.IOException;

import org.ecmdroid.Constants.Variables;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ToggleButton;

public class DataChannelActivity extends BaseActivity {

	private static final String TAG = "DataChannels";
	private static final boolean D = false;
	private static Variable[] channels = new Variable[4];
	private ECM ecm = ECM.getInstance(this);
	private VariableProvider provider = VariableProvider.getInstance(this);
	private ToggleButton toggleButton;
	private ListView listView;
	private DataChannelAdapter dataChannelAdapter;
	private static RefreshTask refreshTask = null;

	@SuppressWarnings("unused")
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
		if(D) {
			ecm.setEEPROM(EEPROM.get("BUEIB"));
			byte[] rt = new byte[200];
			rt[53] = 0x04; rt[52] = (byte) 0x80;
			rt[31] = 0x0a; rt[30] = (byte) 0x98;
			ecm.setRealtimeData(rt);
		}


		if (!Utils.isEmpty(ecm.getId())) {
			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			int i = 0;
			for (String defvar : new String[] {Variables.RPM, Variables.TPD,Variables.CLT,Variables.AFV}) {
				String var = prefs.getString("channel" + (i+1), defvar);
				if (!Utils.isEmpty(var)) {
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
						Log.d(TAG, "Starting Refresh Task");
						if (refreshTask == null) {
							refreshTask = new RefreshTask();
						}
						refreshTask.execute();
					} else {
						if (refreshTask != null) {
							Log.d(TAG, "Stopping RefreshTask");
							refreshTask.cancel(true);
							refreshTask = null;
						}
					}
				}
			}
		});
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		toggleButton.setEnabled(ecm.isConnected() && !ecm.isRecording());
		// TODO: Use the toggle button for enabling/disabling reception
		// registerReceiver(receiver, new IntentFilter(EcmDroidService.REALTIME_DATA));
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		// unregisterReceiver(receiver);
		synchronized(DataChannelActivity.class) {
			if (refreshTask != null) {
				Log.d(TAG, "Stopping RefreshTask");
				refreshTask.cancel(true);
				refreshTask = null;
			}
		}
		saveSettings();
		super.onPause();
	}

	private void saveSettings() {
		if (Utils.isEmpty(ecm.getId())) {
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

	private class RefreshTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected void onPreExecute() {
			dataChannelAdapter.setAutoRefresh(true);
		}
		@Override
		protected Void doInBackground(Void... params) {
			while(!this.isCancelled() && (D || ecm.isConnected() && !ecm.isRecording())) {
				try {
					ecm.readRTData();
				} catch (IOException e) {
					if (D) {
						byte[] data = ecm.getRealtimeData();
						data[52] = (byte) (Math.random() * 50);
						data[30] = (byte) (Math.random() * 50);
						data[12] = (byte) (Math.random() * 50);
						data[25] = (byte) (Math.random() * 255);
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}

				publishProgress();
			}
			Log.d(TAG, "Refresh Task done");
			return null;
		}
		@Override
		protected void onProgressUpdate(Void... values) {
			// Refresh all values
			dataChannelAdapter.notifyDataSetChanged();
		}
		@Override
		protected void onPostExecute(Void result) {
			dataChannelAdapter.setAutoRefresh(false);
			dataChannelAdapter.notifyDataSetChanged();
		}
	}
}
