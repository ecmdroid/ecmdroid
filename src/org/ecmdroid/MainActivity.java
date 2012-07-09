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

import org.ecmdroid.EEPROM.Page;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "MAIN";

	private ECM ecm = ECM.getInstance(this);
	private Button connectButton;
	private DBHelper dbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.main);
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String version = pInfo.versionName;
			setTitle(getText(R.string.app_name) + " " + version);
		} catch (NameNotFoundException e1) {
			// I don't care...
		}
		super.onCreate(savedInstanceState);

		connectButton  = (Button) findViewById(R.id.connectButton);
		connectButton.setOnClickListener(this);
		startService(new Intent(this, EcmDroidService.class));
		dbHelper = new DBHelper(this);
		if (!dbHelper.isDbInstalled()) {
			AsyncTask<Void, Void, Exception> installTask = new AsyncTask<Void, Void, Exception>() {
				private ProgressDialog pd;
				@Override
				protected void onPreExecute() {
					super.onPreExecute();
					pd = ProgressDialog.show(MainActivity.this, null, "Installing Database...");
				}
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
					pd.cancel();
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
		if (ecm.isConnected()) {
			connectButton.setText(R.string.disconnect);
			connectButton.setTag(R.string.connected);
			update();
		}
	}

	private void update() {
		setText(R.id.ecmIdValue, ecm.getId());
		EEPROM eeprom = ecm.getEEPROM();
		if (eeprom != null) {
			setText(R.id.eepromSizeValue, "" + eeprom.length());
			setText(R.id.eepromPagesValue, "" + eeprom.getPageCount());
			setText(R.id.ecmTypeValue, ecm.getType().toString());
			setText(R.id.ecmSerialValue, ecm.getSerialNo());
			setText(R.id.ecmMfgDateValue, ecm.getMfgDate());
		}
	}

	public void onClick(View view) {
		Button b = (Button) view;
		if (b.getTag() == null) {
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (adapter == null || !adapter.isEnabled()) {
				Toast.makeText(this, R.string.bluetooth_is_not_available, Toast.LENGTH_LONG).show();
				return;
			}
			showDevices();
		} else {
			try {
				ecm.disconnect();
			} catch (IOException ioe) {
				Log.w(TAG, "Disconnect failed. ", ioe);
			}
			b.setText(R.string.connect);
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

	protected void connect(BluetoothDevice bluetoothDevice) {
		Log.i(TAG,  "Device selected: " + bluetoothDevice);
		new ConnectTask(bluetoothDevice).execute();
	}

	private void setText(int id, String text) {
		View v = findViewById(id);
		if (v instanceof TextView) {
			((TextView)v).setText(text);
		}
	}

	private class ConnectTask extends AsyncTask<Void, Void, Exception>
	{

		private BluetoothDevice mDevice;
		private ProgressDialog mProgress;

		public ConnectTask(BluetoothDevice device) {
			mDevice = device;
		}

		@Override
		protected void onPreExecute() {
			connectButton.setEnabled(false);
			mProgress = ProgressDialog.show(MainActivity.this, "", "Connecting to " + mDevice.getName() +". Please wait...", true);
		}
		@Override
		protected Exception doInBackground(Void... v) {
			try {
				ecm.connect(mDevice);
			} catch (IOException e) {
				Log.w(TAG, "Connection failed. " + e.getMessage());
				return e;
			}
			return null;
		}
		@Override
		protected void onPostExecute(Exception result) {
			mProgress.dismiss();
			connectButton.setEnabled(true);
			if (result != null) {
				Toast.makeText(MainActivity.this, "Connection failed. " + result.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			} else {
				Log.v(TAG, "Connection established.");
				connectButton.setText(R.string.disconnect);
				connectButton.setTag(R.string.connected);
				new RefreshTask().execute();
			}
		}
	}
	private class RefreshTask extends AsyncTask<Void, String, IOException>
	{
		private String v, sz, p;
		private ProgressDialog mProgress;

		@Override
		protected void onPreExecute() {
			mProgress = ProgressDialog.show(MainActivity.this, "", "Fetching ECM Information...", true);
		}

		@Override
		protected IOException doInBackground(Void... arg0) {
			Log.d(TAG, "Refreshing ECM Information...");
			try {
				v = ecm.getVersion();
				publishProgress();
				if (ecm.getEEPROM() != null) {
					EEPROM eeprom = ecm.getEEPROM();
					ecm.readRTData();
					for (Page pg : ecm.getEEPROM().getPages()) {
						publishProgress("Reading EEPROM data (" + pg.nr() +"/" + eeprom.getPageCount() + ")...");
						ecm.readEEPromPage(pg);
					}
				}
			} catch (IOException e) {
				return e;
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(String... values) {
			update();
			if (values.length >0) {
				mProgress.setMessage(values[0]);
			}
		}

		@Override
		protected void onPostExecute(IOException exception) {
			mProgress.dismiss();
			if (exception != null) {
				Toast.makeText(MainActivity.this, "I/O error. " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				return;
			}
		}
	}
}
