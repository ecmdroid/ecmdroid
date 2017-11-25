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

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.ecmdroid.ECM;
import org.ecmdroid.PDU.Function;
import org.ecmdroid.R;
import org.ecmdroid.task.ProgressDialogTask;

import java.io.IOException;

public class ActiveTestsFragment extends ListFragment implements OnClickListener {
	private static final String TAG = "TESTS";
	private static Function[] functions = new Function[]{
			Function.FrontCoil,
			Function.RearCoil,
			Function.Tachometer,
			Function.FuelPump,
			Function.FrontInj,
			Function.Rear_Inj,
			Function.Fan,
			Function.Exh_Valve,
			Function.Active_Intake,
			Function.Shift_Light
	};

	private ECM ecm = ECM.getInstance(getActivity());
	private Function selectedFunction;
	private FunctionTask functionTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activetests, container, false);
		Button startButton = (Button) view.findViewById(R.id.startTestButton);
		startButton.setOnClickListener(this);
		view.findViewById(R.id.tpsResetButton).setOnClickListener(this);
		if (!ecm.isConnected()) {
			startButton.setEnabled(false);
			view.findViewById(R.id.tpsResetButton).setEnabled(false);
		}
		return view;
	}

	@Override
	public void onResume() {
		getActivity().setTitle(getString(R.string.active_tests));
		setListAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice, functions));
		ListView list = getListView();
		list.setItemsCanFocus(false);
		list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		super.onResume();
	}

	@Override
	public void onDestroy() {
		if (functionTask != null) {
			functionTask.cancel();
			functionTask = null;
		}
		super.onDestroy();
	}

	public void onClick(View view) {
		if (view.getId() == R.id.tpsResetButton) {
			resetTPS();
		} else if (selectedFunction != null) {
			Log.d(TAG, "Invoking function '" + selectedFunction + "'");
			functionTask = new FunctionTask(selectedFunction);
			functionTask.execute();
		}
	}

	private void resetTPS() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if (ecm.getEEPROM().getType() == ECM.Type.DDFI3) {
			AlertDialog alert = builder.setTitle(R.string.tps_reset).setMessage(R.string.tps_reset_notice).setCancelable(false).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			}).create();
			alert.show();
		} else {
			AlertDialog alert = builder.setTitle(R.string.tps_reset).setMessage(R.string.tps_reset_instructions).setCancelable(true).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			}).create();
			alert.setButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface alert, int arg) {
					alert.dismiss();
					new FunctionTask(Function.TPS_Reset).execute();
				}
			});
			alert.show();
		}
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		selectedFunction = (Function) list.getItemAtPosition(position);
		Log.d(TAG, "Selected Function: " + selectedFunction);
	}

	private class FunctionTask extends ProgressDialogTask {
		private Function mFunction;

		public FunctionTask(Function function) {
			super(getActivity(), "");
			mFunction = function;
		}

		@Override
		protected IOException doInBackground(Void... arg0) {
			String action = (mFunction == Function.TPS_Reset ? "Requesting " : "Testing ");
			publishProgress(action + mFunction);
			try {
				ecm.runTest(mFunction);
				// Wait until the Test is through...
				do {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Log.d(TAG, "Test interrupted.");
						break;
					}
				}
				while (ecm.isBusy());
				Log.d(TAG, "Active Test finished.");
			} catch (IOException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Exception result) {
			super.onPostExecute(result);
			if (result == null && mFunction == Function.TPS_Reset) {
				Toast.makeText(getActivity(), "TPS Reset OK.", Toast.LENGTH_LONG).show();
			}
		}
	}
}
