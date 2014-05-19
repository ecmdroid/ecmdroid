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

import java.io.IOException;

import org.ecmdroid.DBHelper;
import org.ecmdroid.ECM;
import org.ecmdroid.ECM.Protocol;
import org.ecmdroid.EEPROM;
import org.ecmdroid.EcmDroidService;
import org.ecmdroid.R;
import org.ecmdroid.Utils;
import org.ecmdroid.task.FetchTask;
import org.ecmdroid.task.ProgressDialogTask;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "MAIN";

	private ECM ecm = ECM.getInstance(this);
	private Button connectButton;
	private Spinner protocolSpinner;
	private DBHelper dbHelper;
	private SharedPreferences prefs;

	protected EcmDroidService ecmDroidService;

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Disconnected from Service");
			ecmDroidService = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Connected to Log Service");
			ecmDroidService = ((EcmDroidService.EcmDroidBinder)service).getService();
		}
	};

	private String connectionType;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.main);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String version = Utils.getAppVersion(this);
		if (version != null) {
			setTitle(version);
		}
		super.onCreate(savedInstanceState);

		protocolSpinner = (Spinner) findViewById(R.id.protocolSpinner);
		protocolSpinner.setAdapter(new ArrayAdapter<ECM.Protocol>(this, android.R.layout.simple_spinner_item, ECM.Protocol.values()));

		bindService(new Intent(this, EcmDroidService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		connectButton  = (Button) findViewById(R.id.connectButton);
		connectButton.setOnClickListener(this);
		startService(new Intent(this, EcmDroidService.class));
		dbHelper = new DBHelper(this);
		if (!dbHelper.isDbInstalled()) {
			ProgressDialogTask installTask = new ProgressDialogTask(this, "Installing Database") {
				@Override
				protected Exception doInBackground(Void ...args) {
					try {
						dbHelper.installDB(MainActivity.this);
					} catch (IOException e) {
						return e;
					}
					return null;
				}

				@Override
				protected void onPostExecute(Exception result) {
					super.onPostExecute(result);
					if (result != null) {
						Toast.makeText(MainActivity.this, "FATAL: DB Installation failed.", Toast.LENGTH_LONG).show();
						System.exit(0);
					}
				}
			};
			installTask.execute();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		connectionType = prefs.getString(getString(R.string.prefs_conn_type), getString(R.string.prefs_bt_connection));
		if (ecm.isConnected()) {
			connectButton.setText(R.string.disconnect);
			connectButton.setTag(R.string.connected);
		}
		protocolSpinner.setEnabled(!ecm.isConnected());
		protocolSpinner.setSelection(ecm.getCurrentProtocol().ordinal());
		update();
	}

	@Override
	protected void onDestroy() {
		unbindService(serviceConnection);
		super.onDestroy();
	}
	private void update() {
		setText(R.id.ecmIdValue, ecm.getId());
		EEPROM eeprom = ecm.getEEPROM();
		if (eeprom != null) {
			setText(R.id.eepromSizeValue, "" + eeprom.length());
			setText(R.id.eepromPagesValue, "" + eeprom.getPageCount());
			setText(R.id.ecmTypeValue, "" + ecm.getType());
			if (eeprom.isEepromRead()) {
				setText(R.id.ecmSerialValue, ecm.getSerialNo());
				setText(R.id.ecmMfgDateValue, ecm.getMfgDate());
				setText(R.id.ecmLayoutRevisionValue, ecm.getLayoutRevision());
				setText(R.id.ecmCountryIdValue, ecm.getCountryId());
				setText(R.id.ecmCalibrationValue, ecm.getCalibrationId());
			}
		}
	}

	public void onClick(View view) {
		Button b = (Button) view;
		if (b.getTag() == null) {
			if (getString(R.string.prefs_bt_connection).equals(connectionType)) {
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				if (adapter == null || !adapter.isEnabled()) {
					Toast.makeText(this, R.string.bluetooth_is_not_available, Toast.LENGTH_LONG).show();
					return;
				}
				showDevices();
			} else {
				String host = prefs.getString("tcp_host", null);
				int port = 0;
				try {
					port = Integer.parseInt(prefs.getString("tcp_port", "0"));
				} catch (NumberFormatException nfe) {}
				if (host == null || port <= 0 || port > 0xFFFF) {
					Toast.makeText(this, String.format("%s/%d: Illegal host/port combination.", host, port), Toast.LENGTH_LONG).show();
					return;
				}
				MainActivity.this.connect(host, port);
			}
		} else {
			try {
				if (ecmDroidService != null) {
					ecmDroidService.stopReading();
					ecmDroidService.stopRecording();
				}
				ecm.disconnect();
			} catch (IOException ioe) {
				Log.w(TAG, "Disconnect failed. ", ioe);
			}
			b.setText(R.string.connect);
			protocolSpinner.setEnabled(true);
			b.setTag(null);
		}
	}

	private void showDevices() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.paired_devices);
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		final BluetoothDevice[] devices = btAdapter == null ? new BluetoothDevice[0] : btAdapter.getBondedDevices().toArray(new BluetoothDevice[0]);
		CharSequence[] items = new CharSequence[devices.length];

		int i=0;
		for (BluetoothDevice device : devices) {
			items[i++] = device.getName() + " (" + device.getAddress() + ")";
		}

		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				dialog.cancel();
				MainActivity.this.connect(devices[item]);
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void connect(BluetoothDevice bluetoothDevice) {
		Log.i(TAG,  "Device selected: " + bluetoothDevice);
		new ConnectTask(bluetoothDevice).execute();
	}

	private void connect(String host, int port) {
		Log.i(TAG,  "TCP Connection to " + host + ":" + port);
		new ConnectTask(host, port).execute();
	}


	private void setText(int id, String text) {
		View v = findViewById(id);
		if (v instanceof TextView) {
			((TextView)v).setText(text);
		}
	}

	private class ConnectTask extends FetchTask
	{
		private BluetoothDevice btDevice;
		private String host;
		private int port;

		public ConnectTask(BluetoothDevice device) {
			super(MainActivity.this);
			btDevice = device;
		}

		public ConnectTask(String host, int port) {
			super(MainActivity.this);
			this.host = host;
			this.port = port;
		}

		@Override
		protected void onPreExecute() {
			connectButton.setEnabled(false);
			super.onPreExecute();
		}
		@Override
		protected Exception doInBackground(Void... v) {
			String target = null;
			if (btDevice != null) {
				target = btDevice.getName();
			} else {
				target = host + ":" + port;
			}
			publishProgress(String.format("Connecting to %1$s...", target));
			try {
				if (btDevice != null) {
					ecm.connect(btDevice, (Protocol) protocolSpinner.getSelectedItem());
				} else {
					ecm.connect(host, port, (Protocol) protocolSpinner.getSelectedItem());
				}
			} catch (Exception e) {
				return e;
			}
			return super.doInBackground();
		}

		@Override
		protected void onProgressUpdate(String... values) {
			update();
			super.onProgressUpdate(values);
		}
		@Override
		protected void onPostExecute(Exception result) {
			super.onPostExecute(result);
			update();
			connectButton.setEnabled(true);
			if (result == null ) {
				connectButton.setText(R.string.disconnect);
				connectButton.setTag(R.string.connected);
				protocolSpinner.setEnabled(false);
			} else {
				try {
					ecm.disconnect();
				} catch (IOException e) {}
			}
		}
	}
}
