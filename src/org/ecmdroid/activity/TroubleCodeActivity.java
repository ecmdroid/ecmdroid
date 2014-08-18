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
package org.ecmdroid.activity;
import java.io.IOException;
import java.util.Collection;

import org.ecmdroid.ECM;
import org.ecmdroid.Error;
import org.ecmdroid.Error.ErrorType;
import org.ecmdroid.PDU.Function;
import org.ecmdroid.R;
import org.ecmdroid.task.ProgressDialogTask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class TroubleCodeActivity extends BaseActivity implements OnClickListener {
	private Collection<Error> currentErrors;
	private Collection<Error> storedErrors;
	private ECM ecm = ECM.getInstance(this);
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.troublecodes);
		((Button)findViewById(R.id.readErrors)).setOnClickListener(this);
		((Button)findViewById(R.id.clearErrors)).setOnClickListener(this);
	}
	@Override
	protected void onResume() {
		super.onResume();
		((Button)findViewById(R.id.readErrors)).setEnabled(ecm.isConnected());
		((Button)findViewById(R.id.clearErrors)).setEnabled(ecm.isConnected());
		update();
	}

	public void onClick(View view) {
		if (view.getId() == R.id.readErrors) {
			// Fetch runtime data and update fields
			new ProgressDialogTask(TroubleCodeActivity.this, "") {

				@Override
				protected Exception doInBackground(Void... params) {
					publishProgress(TroubleCodeActivity.this.getString(R.string.fetching_trouble_codes));
					try {
						ecm.readRTData();
						currentErrors = ecm.getErrors(ErrorType.CURRENT);
						storedErrors = ecm.getErrors(ErrorType.STORED);
					} catch (IOException e) {
						return e;
					}
					return null;
				}
				@Override
				protected void onPostExecute(Exception result) {
					super.onPostExecute(result);
					update();
				}
			}.execute();
		}
		else if (view.getId() == R.id.clearErrors) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog alert = builder.setTitle(R.string.clear_errors).setMessage(R.string.clear_all_errors).setCancelable(true).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface alert, int arg) {
					alert.cancel();
				}
			}).setPositiveButton(getText(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface alert, int arg) {
					alert.dismiss();
					new ProgressDialogTask(TroubleCodeActivity.this, ""){
						@Override
						protected Exception doInBackground(Void... args) {
							publishProgress(TroubleCodeActivity.this.getString(R.string.clearing_trouble_codes));
							try {
								ecm.runTest(Function.ClearCodes);
								ecm.readRTData();
							} catch (IOException e) {
								return e;
							}
							return null;
						}

						@Override
						protected void onPostExecute(Exception result) {
							super.onPostExecute(result);
							update();
						};
					}.execute();
				}
			}).create();
			alert.show();
		}
	}

	private void update() {
		currentErrors = storedErrors = null;
		try {
			currentErrors = ecm.getErrors(ErrorType.CURRENT);
			storedErrors = ecm.getErrors(ErrorType.STORED);
		} catch (IOException e) {}
		EditText ce = (EditText) findViewById(R.id.currentErrors);
		ce.setText(errors2str(currentErrors));
		EditText se = (EditText) findViewById(R.id.storedErrors);
		se.setText(errors2str(storedErrors));
	}

	private CharSequence errors2str(Collection<Error> errors) {
		if (errors == null) {
			return "";
		}
		if (errors.size() == 0) {
			return getText(R.string.no_errors);
		}
		String fmt = "%3s | %s\n";
		StringBuffer sb = new StringBuffer();
		for (Error e : errors) {
			sb.append(String.format(fmt, e.getCode(), e.getDescription()));
		}
		return sb;
	};
}
