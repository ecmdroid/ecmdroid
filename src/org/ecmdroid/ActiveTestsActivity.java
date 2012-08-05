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

import org.ecmdroid.PDU.Function;
import org.ecmdroid.tasks.ProgressDialogTask;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class ActiveTestsActivity extends ListActivity implements OnClickListener
{
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
	private ECM ecm = ECM.getInstance(this);
	private Button startButton;
	private Function selectedFunction;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activetests);

		setListAdapter(new ArrayAdapter<Function>(this, android.R.layout.simple_list_item_single_choice, functions));
		ListView list = getListView();
		list.setItemsCanFocus(false);
		list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		startButton  = (Button) findViewById(R.id.startTestButton);
		startButton.setOnClickListener(this);
		((Button) findViewById(R.id.tpsResetButton)).setOnClickListener(this);
		if(!ecm.isConnected()) {
			startButton.setEnabled(false);
			((Button) findViewById(R.id.tpsResetButton)).setEnabled(false);
		}
	}

	public void onClick(View view) {
		if (view.getId() == R.id.tpsResetButton) {
			resetTPS();
		} else  if (selectedFunction != null) {
			Log.d(TAG, "Invoking function '" + selectedFunction + "'");
			new FunctionTask(selectedFunction).execute();
		}
	}

	private void resetTPS() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (ecm.getEEPROM().getType() == ECM.Type.DDFI3) {
			AlertDialog alert = builder.setTitle(R.string.tps_reset).setMessage(R.string.tps_reset_notice).setCancelable(false).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			}).create();
			alert.show();
			return;
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
	protected void onListItemClick(ListView list, View view, int position, long id) {
		selectedFunction = (Function) list.getItemAtPosition(position);
		Log.d(TAG, "Selected Function: " + selectedFunction);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return Utils.createOptionsMenu(this, menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Utils.prepareOptionsMenu(this, menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!Utils.handleOptionsItemSelected(this, item)) {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private class FunctionTask extends ProgressDialogTask
	{
		private Function mFunction;
		public FunctionTask(Function function) {
			super(ActiveTestsActivity.this, "");
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
						Log.d(TAG, "Interrupted...", e);
					}
				}
				while(ecm.isBusy());
			} catch (IOException e) {
				return e;
			}
			return null;
		}
		@Override
		protected void onPostExecute(Exception result) {
			super.onPostExecute(result);
			if (result == null && mFunction == Function.TPS_Reset) {
				Toast.makeText(ActiveTestsActivity.this, "TPS Reset OK.", Toast.LENGTH_LONG).show();
			}
		}
	}
}
