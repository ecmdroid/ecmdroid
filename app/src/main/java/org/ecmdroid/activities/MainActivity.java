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

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.ecmdroid.Constants;
import org.ecmdroid.DBHelper;
import org.ecmdroid.ECM;
import org.ecmdroid.EcmDroidService;
import org.ecmdroid.R;
import org.ecmdroid.Utils;
import org.ecmdroid.fragments.ActiveTestsFragment;
import org.ecmdroid.fragments.DataChannelFragment;
import org.ecmdroid.fragments.EEPROMFragment;
import org.ecmdroid.fragments.LogFragment;
import org.ecmdroid.fragments.MainFragment;
import org.ecmdroid.fragments.SetupFragment;
import org.ecmdroid.fragments.TorqueValuesFragment;
import org.ecmdroid.fragments.TroubleCodeFragment;
import org.ecmdroid.task.FetchTask;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	public static final String CURRENT_FRAGMENT = "currentFragment";
	private static final String TAG = "MAIN";
	private static final ColorStateList TINT_DISCONNECTED = ColorStateList.valueOf(Color.RED);
	private static final ColorStateList TINT_CONNECTING = ColorStateList.valueOf(Color.GRAY);
	private static final ColorStateList TINT_CONNECTED = ColorStateList.valueOf(Color.rgb(0x00, 0xdd, 0x00));

	private int currentFragment = R.id.nav_info;
	private DBHelper dbHelper;

	private ECM ecm = ECM.getInstance(this);
	protected EcmDroidService ecmDroidService;
	private FloatingActionButton fab;

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Disconnected from Service");
			ecmDroidService = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Connected to Log Service");
			ecmDroidService = ((EcmDroidService.EcmDroidBinder) service).getService();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(" + savedInstanceState + "," + getIntent().getExtras() + ")");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			currentFragment = savedInstanceState.getInt(CURRENT_FRAGMENT, R.id.nav_info);
		} else if (getIntent().getExtras() != null) {
			currentFragment = getIntent().getExtras().getInt(CURRENT_FRAGMENT, R.id.nav_info);
		}

		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!ecm.isConnected()) {
					connect();
				} else {
					disconnect();
				}
			}
		});


		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		((TextView) navigationView.getHeaderView(0).findViewById(R.id.headerTitle)).setText(Utils.getAppVersion(this));

		// Bind to our service and setup the connect button
		bindService(new Intent(this, EcmDroidService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		startService(new Intent(this, EcmDroidService.class));

		switchToFragment(currentFragment);

		// Install the database
		dbHelper = new DBHelper(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateConnectButton();
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(CURRENT_FRAGMENT, currentFragment);
	}

	@Override
	protected void onDestroy() {
		unbindService(serviceConnection);
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		if (id == R.id.nav_torque) {
			Intent intent = new Intent(this, TorqueValuesFragment.class);
			startActivity(intent);
		} else if (id == R.id.nav_settings) {
			Intent intent = new Intent(this, PrefsActivity.class);
			startActivity(intent);
		} else if (id == R.id.nav_about) {
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
		} else {
			currentFragment = id;
			switchToFragment(id);
		}
		return true;
	}

	private void switchToFragment(int id) {
		Fragment fragment = null;
		if (id == R.id.nav_info) {
			fragment = new MainFragment();
		} else if (id == R.id.nav_troublecodes) {
			fragment = new TroubleCodeFragment();
		} else if (id == R.id.nav_tests) {
			fragment = new ActiveTestsFragment();
		} else if (id == R.id.nav_datachannels) {
			fragment = new DataChannelFragment();
		} else if (id == R.id.nav_setup) {
			fragment = new SetupFragment();
		} else if (id == R.id.nav_log) {
			fragment = new LogFragment();
		} else if (id == R.id.nav_eeprom) {
			fragment = new EEPROMFragment();
		}
		FragmentManager mgr = getFragmentManager();
		if (fragment != null) {
			mgr.beginTransaction()
					.replace(R.id.content_frame, fragment)
					.commit();
		}
	}

	private void showDevices() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.paired_devices);
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		final BluetoothDevice[] devices = btAdapter == null ? new BluetoothDevice[0] : btAdapter.getBondedDevices().toArray(new BluetoothDevice[0]);
		CharSequence[] items = new CharSequence[devices.length];

		int i = 0;
		for (BluetoothDevice device : devices) {
			items[i++] = device.getName() + " (" + device.getAddress() + ")";
		}

		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				dialog.cancel();
				connect(devices[item]);
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void disconnect() {
		try {
			if (ecmDroidService != null && ecmDroidService.isRecording()) {
				ecmDroidService.stopRecording();
			}
			ecm.disconnect();
			Toast.makeText(MainActivity.this, R.string.disconnected, Toast.LENGTH_LONG).show();
			updateConnectButton();
			// Reload fragment
			switchToFragment(currentFragment);
		} catch (IOException ioe) {
			Log.w(TAG, "Disconnect failed. ", ioe);
		}
	}

	private void connect() {
		fab.setBackgroundTintList(TINT_CONNECTING);
		fab.setImageResource(R.drawable.ic_connected);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		String connectionType = prefs.getString(getString(R.string.prefs_conn_type), getString(R.string.prefs_bt_connection));
		if (getString(R.string.prefs_bt_connection).equals(connectionType)) {
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (adapter == null || !adapter.isEnabled()) {
				Toast.makeText(MainActivity.this, R.string.bluetooth_is_not_available, Toast.LENGTH_LONG).show();
				return;
			}
			showDevices();
		} else {
			String host = prefs.getString("tcp_host", null);
			int port = 0;
			try {
				port = Integer.parseInt(prefs.getString("tcp_port", "0"));
			} catch (NumberFormatException nfe) {
			}

			if (host == null || port <= 0 || port > 0xFFFF) {
				Toast.makeText(MainActivity.this, String.format(Locale.US, "%s/%d: Illegal host/port combination.", host, port), Toast.LENGTH_LONG).show();
				return;
			}
			connect(host, port);
		}
	}

	private void connect(BluetoothDevice bluetoothDevice) {
		Log.i(TAG, "Device selected: " + bluetoothDevice);

		new ConnectTask(bluetoothDevice, getProtocol()).execute();
	}

	private void connect(String host, int port) {
		Log.i(TAG, "TCP Connection to " + host + ":" + port);
		new ConnectTask(host, port, getProtocol()).execute();
	}

	private ECM.Protocol getProtocol() {
		return ECM.Protocol.values()[PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.PREFS_ECM_PROTOCOL, 0)];
	}

	private class ConnectTask extends FetchTask {
		private ECM.Protocol protocol;
		private BluetoothDevice btDevice;
		private String host;
		private int port;

		public ConnectTask(BluetoothDevice device, ECM.Protocol protocol) {
			super(MainActivity.this);
			btDevice = device;
			this.protocol = protocol;
		}

		public ConnectTask(String host, int port, ECM.Protocol protocol) {
			super(MainActivity.this);
			this.host = host;
			this.port = port;
			this.protocol = protocol;

		}

		@Override
		protected void onPreExecute() {
			// connectButton.setEnabled(false);
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
			publishProgress(String.format(Locale.US, "Connecting to %1$s...", target));
			try {
				if (btDevice != null) {
					ecm.connect(btDevice, protocol);
				} else {
					ecm.connect(host, port, protocol);
				}
			} catch (Exception e) {
				return e;
			}
			return super.doInBackground();
		}

		@Override
		protected void onProgressUpdate(String... values) {
			// update();
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Exception result) {
			super.onPostExecute(result);
			if (result != null) {
				try {
					ecm.disconnect();
				} catch (IOException e) {
				}
			}
			// Reload the fragment
			switchToFragment(currentFragment);
			updateConnectButton();
		}
	}

	private void updateConnectButton() {
		fab.setBackgroundTintList(ecm.isConnected() ? TINT_CONNECTED : TINT_DISCONNECTED);
		fab.setImageResource(ecm.isConnected() ? R.drawable.ic_connected : R.drawable.ic_disconnected);
	}
}
