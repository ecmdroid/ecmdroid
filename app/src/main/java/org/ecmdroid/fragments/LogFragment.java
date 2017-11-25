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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.ecmdroid.Constants.Variables;
import org.ecmdroid.ECM;
import org.ecmdroid.EcmDroidService;
import org.ecmdroid.R;
import org.ecmdroid.Utils;
import org.ecmdroid.Variable;
import org.ecmdroid.task.ProgressDialogTask;
import org.ecmdroid.util.Bin2MslConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Observable;
import java.util.Observer;

public class LogFragment extends Fragment implements OnClickListener {
	private static final String PREFS_CONVERTLOG = "convertlog";
	private static final String PREFS_DELAY = "delay";
	private static final String TAG = "LogFragment";
	private Button recordButton;
	private TextView logFile;
	private TextView logStatus;
	private TextView tpsValue, rpmValue, cltValue;
	private ECM ecm = ECM.getInstance(getActivity());
	private EcmDroidService ecmDroidService;

	private static class Interval {
		int delay;
		String name;

		Interval(int d, String n) {
			delay = d;
			name = n;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static Interval[] intervals = new Interval[]{
			new Interval(0, "No Delay"),
			new Interval(250, "250ms"),
			new Interval(500, "500ms"),
			new Interval(1000, "1s"),
			new Interval(2000, "2s"),
			new Interval(5000, "5s")
	};

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Disconnected from Log Service");
			ecmDroidService = null;
			recordButton.setEnabled(false);
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Connected to Log Service");
			ecmDroidService = ((EcmDroidService.EcmDroidBinder) service).getService();
			if (ecm.isConnected()) {
				recordButton.setEnabled(true);
				if (ecm.isRecording()) {
					recordButton.setText(R.string.stop_recording);
				}
			}
			updateUI();
		}
	};

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI();
		}
	};

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.log, container, false);
		logFile = (TextView) view.findViewById(R.id.logFileValue);
		logStatus = (TextView) view.findViewById(R.id.logStatusValue);
		tpsValue = (TextView) view.findViewById(R.id.tpsValue);
		rpmValue = (TextView) view.findViewById(R.id.rpmValue);
		cltValue = (TextView) view.findViewById(R.id.cltValue);
		recordButton = (Button) view.findViewById(R.id.recordButton);
		recordButton.setEnabled(false);
		recordButton.setOnClickListener(this);
		Spinner spinner = (Spinner) view.findViewById(R.id.logInterval);
		ArrayAdapter<Interval> aa = new ArrayAdapter<Interval>(getActivity(), android.R.layout.simple_spinner_item, intervals);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(aa);
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().bindService(new Intent(getActivity(), EcmDroidService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().setTitle(getString(R.string.log_recorder));
		getActivity().registerReceiver(receiver, new IntentFilter(EcmDroidService.RECORDING_STARTED));
		getActivity().registerReceiver(receiver, new IntentFilter(EcmDroidService.RECORDING_STOPPED));
		getActivity().registerReceiver(receiver, new IntentFilter(EcmDroidService.REALTIME_DATA));
		Spinner spinner = (Spinner) getView().findViewById(R.id.logInterval);
		if (ecmDroidService != null && ecm.isRecording()) {
			spinner.setEnabled(false);
		} else {
			spinner.setEnabled(true);
		}
		SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
		spinner.setSelection(prefs.getInt(PREFS_DELAY, 0));
		CheckBox convert = (CheckBox) getView().findViewById(R.id.logConvertCheckbox);
		convert.setChecked(prefs.getBoolean(PREFS_CONVERTLOG, false));
	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
		Spinner spinner = (Spinner) getView().findViewById(R.id.logInterval);
		CheckBox convert = (CheckBox) getView().findViewById(R.id.logConvertCheckbox);
		Editor editor = prefs.edit();
		editor.putInt(PREFS_DELAY, spinner.getSelectedItemPosition());
		editor.putBoolean(PREFS_CONVERTLOG, convert.isChecked());
		editor.apply();
		getActivity().unregisterReceiver(receiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unbindService(serviceConnection);
	}

	public void onClick(View view) {
		Interval intv = (Interval) ((Spinner) getView().findViewById(R.id.logInterval)).getSelectedItem();
		Log.d(TAG, "Interval: " + intv);

		// Start recording...
		if (ecmDroidService != null) {
			if (!ecm.isRecording()) {
				try {
					startRecording();
				} catch (IOException e) {
					Toast.makeText(getActivity(), "I/O error. " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				}
			} else {
				boolean convert = ((CheckBox) getView().findViewById(R.id.logConvertCheckbox)).isChecked();
				recordButton.setEnabled(false);
				new StopTask(getActivity(), convert).execute();
				recordButton.setEnabled(true);
				recordButton.setText(R.string.start_recording);
			}
		}
	}

	private void startRecording() throws IOException {
		if (!Utils.isExternalStorageAvailable()) {
			Toast.makeText(getActivity(), R.string.no_ext_storage, Toast.LENGTH_LONG).show();
			return;
		}
		Spinner spinner = (Spinner) getView().findViewById(R.id.logInterval);
		Interval interval = (Interval) spinner.getSelectedItem();

		CharSequence fn = DateFormat.format("yyyyMMdd_kkmmss", System.currentTimeMillis());
		File dir = getActivity().getApplication().getExternalFilesDir(getString(R.string.log_dir));
		if (!dir.exists() && !dir.mkdirs()) {
			Log.w(TAG, "Unable to create directories " + dir.getAbsolutePath());
		}
		recordButton.setText(R.string.stop_recording);
		ecmDroidService.startRecording(new File(dir, fn + ".bin"), interval == null ? 0 : interval.delay, ecm);
	}

	protected void updateUI() {
		if (ecmDroidService != null) {
			if (ecm.isRecording()) {
				logFile.setText(ecmDroidService.getLogfile());
				logStatus.setText(String.format(getString(R.string.log_status), ecmDroidService.getRecords(), ecmDroidService.getBytes() / 1024));
				Variable tps = ecm.getRuntimeValue(Variables.TPD);
				Variable rpm = ecm.getRuntimeValue(Variables.RPM);
				Variable clt = ecm.getRuntimeValue(Variables.CLT);
				if (tps != null) {
					tpsValue.setText(tps.getFormattedValue());
				}
				if (rpm != null) {
					rpmValue.setText(String.valueOf(rpm.getIntValue()));
				}
				if (clt != null) {
					cltValue.setText(clt.getFormattedValue());
				}
			} else {
				logFile.setText(R.string.dash);
				logStatus.setText(R.string.status_idle);
				tpsValue.setText(R.string.dash);
				rpmValue.setText(R.string.dash);
				cltValue.setText(R.string.dash);
			}
		}
	}

	private class StopTask extends ProgressDialogTask implements Observer {
		private boolean convert;
		private Bin2MslConverter converter;
		private String lastStatus;

		public StopTask(Activity context, boolean convert) {
			super(context, "");
			this.convert = convert;
			converter = new Bin2MslConverter();
		}

		@Override
		protected Exception doInBackground(Void... params) {
			Exception ret = null;
			File log = ecmDroidService.getCurrentFile();
			publishProgress(context.getString(R.string.stopping_logfile_recorder));
			ecmDroidService.stopRecording();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			if (convert) {
				setCancelable(true);
				publishProgress(context.getString(R.string.converting_to_msl));
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				InputStream in = null;
				OutputStream out = null;
				try {
					in = new FileInputStream(log);
					out = new FileOutputStream(new File(log.getAbsolutePath().replaceAll("bin$", "msl")));
					converter.addObserver(this);
					converter.convert(in, out);
				} catch (Exception e) {
					Log.w(TAG, "Conversion failed.", e);
					ret = new Exception(context.getString(R.string.conversion_failed) + " " + e.getMessage());
				} finally {
					try {
						if (in != null)
							in.close();

						if (out != null) {
							out.flush();
							out.close();
						}
					} catch (Exception e) {
						Log.w(TAG, "Unable to close Input/Output stream.", e);
						ret = e;
					}
				}
			}
			return ret;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			Toast.makeText(getActivity(), R.string.conversion_cancelled, Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(Exception result) {
			super.onPostExecute(result);
			if (result == null && lastStatus != null) {
				Toast.makeText(getActivity(), lastStatus, Toast.LENGTH_LONG).show();
			}
		}

		public void update(Observable observable, Object data) {
			lastStatus = (String) data;
			if (isCancelled()) {
				converter.cancel();
			} else {
				publishProgress(lastStatus);
			}
		}
	}
}
