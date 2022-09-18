package de.kai_morich.simple_bluetooth_le_terminal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.ecmdroid.R;
import org.ecmdroid.fragments.MainFragment;

import java.util.ArrayList;
import java.util.Collections;

/**
 * show list of BLE devices
 */
public class DevicesFragment extends ListFragment {

	private enum ScanState { NONE, LE_SCAN, DISCOVERY, DISCOVERY_FINISHED }
	private ScanState scanState = ScanState.NONE;
	private static final long LE_SCAN_PERIOD = 10000; // similar to bluetoothAdapter.startDiscovery
	private final Handler leScanStopHandler = new Handler();
	private final BluetoothAdapter.LeScanCallback leScanCallback;
	private final Runnable leScanStopCallback;
	private final BroadcastReceiver discoveryBroadcastReceiver;
	private final IntentFilter discoveryIntentFilter;

	private Menu menu;
	private BluetoothAdapter bluetoothAdapter;
	private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
	private ArrayAdapter<BluetoothDevice> listAdapter;

	public DevicesFragment() {
		leScanCallback = (device, rssi, scanRecord) -> {
			if(device != null && getActivity() != null) {
				getActivity().runOnUiThread(() -> { updateScan(device); });
			}
		};
		discoveryBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if(device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC && getActivity() != null) {
						getActivity().runOnUiThread(() -> updateScan(device));
					}
				}
				if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
					scanState = ScanState.DISCOVERY_FINISHED; // don't cancel again
					stopScan();
				}
			}
		};
		discoveryIntentFilter = new IntentFilter();
		discoveryIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		discoveryIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		leScanStopCallback = this::stopScan; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		if(getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems) {
			@NonNull
			@Override
			public View getView(int position, View view, @NonNull ViewGroup parent) {
				BluetoothDevice device = listItems.get(position);
				if (view == null)
					view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
				TextView text1 = view.findViewById(R.id.text1);
				TextView text2 = view.findViewById(R.id.text2);
				if(device.getName() == null || device.getName().isEmpty())
					text1.setText(getString(R.string.ble_unnamed));
				else
					text1.setText(device.getName());
				text2.setText(device.getAddress());
				return view;
			}
		};
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(null);
		View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
		getListView().addHeaderView(header, null, false);
		setEmptyText(getString(R.string.ble_initializing));
		((TextView) getListView().getEmptyView()).setTextSize(18);
		setListAdapter(listAdapter);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_devices, menu);
		this.menu = menu;
		if (bluetoothAdapter == null) {
			menu.findItem(R.id.bt_settings).setEnabled(false);
			menu.findItem(R.id.ble_scan).setEnabled(false);
		} else if(!bluetoothAdapter.isEnabled()) {
			menu.findItem(R.id.ble_scan).setEnabled(false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().setTitle(getString(R.string.ble_devicelist_title));
		getActivity().registerReceiver(discoveryBroadcastReceiver, discoveryIntentFilter);
		if(bluetoothAdapter == null) {
			setEmptyText(getString(R.string.ble_not_supported));
		} else if(!bluetoothAdapter.isEnabled()) {
			setEmptyText(getString(R.string.bluetooth_disabled));
			if (menu != null) {
				listItems.clear();
				listAdapter.notifyDataSetChanged();
				menu.findItem(R.id.ble_scan).setEnabled(false);
			}
		} else {
			setEmptyText(getString(R.string.ble_scan));
			if (menu != null)
				menu.findItem(R.id.ble_scan).setEnabled(true);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		stopScan();
		getActivity().unregisterReceiver(discoveryBroadcastReceiver);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		menu = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.ble_scan) {
			startScan();
			return true;
		} else if (id == R.id.ble_scan_stop) {
			stopScan();
			return true;
		} else if (id == R.id.bt_settings) {
			Intent intent = new Intent();
			intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressLint("StaticFieldLeak") // AsyncTask needs reference to this fragment
	private void startScan() {
		if(scanState != ScanState.NONE)
			return;
		scanState = ScanState.LE_SCAN;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				scanState = ScanState.NONE;
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.location_permission_title);
				builder.setMessage(R.string.location_permission_message);
				builder.setPositiveButton(android.R.string.ok,
						(dialog, which) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0));
				builder.show();
				return;
			}
			LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
			boolean         locationEnabled = false;
			try {
				locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			} catch(Exception ignored) {}
			try {
				locationEnabled |= locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			} catch(Exception ignored) {}
			if(!locationEnabled)
				scanState = ScanState.DISCOVERY;
			// Starting with Android 6.0 a bluetooth scan requires ACCESS_COARSE_LOCATION permission, but that's not all!
			// LESCAN also needs enabled 'location services', whereas DISCOVERY works without.
			// Most users think of GPS as 'location service', but it includes more, as we see here.
			// Instead of asking the user to enable something they consider unrelated,
			// we fall back to the older API that scans for bluetooth classic _and_ LE
			// sometimes the older API returns less results or slower
		}
		listItems.clear();
		listAdapter.notifyDataSetChanged();
		setEmptyText(getString(R.string.ble_scanning));
		menu.findItem(R.id.ble_scan).setVisible(false);
		menu.findItem(R.id.ble_scan_stop).setVisible(true);
		if(scanState == ScanState.LE_SCAN) {
			leScanStopHandler.postDelayed(leScanStopCallback, LE_SCAN_PERIOD);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void[] params) {
					bluetoothAdapter.startLeScan(null, leScanCallback);
					return null;
				}
			}.execute(); // start async to prevent blocking UI, because startLeScan sometimes take some seconds
		} else {
			bluetoothAdapter.startDiscovery();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		// ignore requestCode as there is only one in this fragment
		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			new Handler(Looper.getMainLooper()).postDelayed(this::startScan,1); // run after onResume to avoid wrong empty-text
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getText(R.string.location_denied_title));
			builder.setMessage(getText(R.string.location_denied_message));
			builder.setPositiveButton(android.R.string.ok, null);
			builder.show();
		}
	}

	private void updateScan(BluetoothDevice device) {
		if(scanState == ScanState.NONE)
			return;
		if(!listItems.contains(device)) {
			listItems.add(device);
			Collections.sort(listItems, DevicesFragment::compareTo);
			listAdapter.notifyDataSetChanged();
		}
	}

	private void stopScan() {
		if(scanState == ScanState.NONE)
			return;
		setEmptyText(getString(R.string.no_ble_devices_found));
		if(menu != null) {
			menu.findItem(R.id.ble_scan).setVisible(true);
			menu.findItem(R.id.ble_scan_stop).setVisible(false);
		}
		switch(scanState) {
			case LE_SCAN:
				leScanStopHandler.removeCallbacks(leScanStopCallback);
				bluetoothAdapter.stopLeScan(leScanCallback);
				break;
			case DISCOVERY:
				bluetoothAdapter.cancelDiscovery();
				break;
			default:
				// already canceled
		}
		scanState = ScanState.NONE;

	}

	@Override
	public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
		stopScan();
		BluetoothDevice device = listItems.get(position-1);
		Bundle args = new Bundle();
		args.putString("device", device.getAddress());
		MainFragment fragment = new MainFragment();
		fragment.setArguments(args);
		getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
	}

	/**
	 * sort by name, then address. sort named devices first
	 */
	static int compareTo(BluetoothDevice a, BluetoothDevice b) {
		boolean aValid = a.getName()!=null && !a.getName().isEmpty();
		boolean bValid = b.getName()!=null && !b.getName().isEmpty();
		if(aValid && bValid) {
			int ret = a.getName().compareTo(b.getName());
			if (ret != 0) return ret;
			return a.getAddress().compareTo(b.getAddress());
		}
		if(aValid) return -1;
		if(bValid) return +1;
		return a.getAddress().compareTo(b.getAddress());
	}
}
