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
package org.ecmdroid.task;

import org.ecmdroid.Utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Base class for async tasks with a progress dialog.
 */
public abstract class ProgressDialogTask extends AsyncTask<Void, String, Exception> implements OnCancelListener
{
	protected Activity context;

	private int ro;
	private ProgressDialog pd;
	private String taskTitle;

	public ProgressDialogTask(Activity context, String taskTitle) {
		this.context = context;
		this.taskTitle = taskTitle;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		ro = Utils.freezeOrientation(context);
		super.onPreExecute();
		pd = ProgressDialog.show(context, taskTitle, "");
		pd.setOnCancelListener(this);
		pd.setCancelable(false);
		pd.setCanceledOnTouchOutside(false);
	}

	public void setCancelable(boolean state) {
		pd.setCancelable(state);
	}

	public void cancel() {
		pd.cancel();
		super.cancel(true);
	}

	@Override
	protected void onPostExecute(Exception result) {
		if (pd.isShowing()) {
			pd.dismiss();
		}
		context.setRequestedOrientation(ro);
		if (result != null) {
			Toast.makeText(context, result.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
		super.onPostExecute(result);
	}

	@Override
	protected void onCancelled() {
		pd.dismiss();
		super.onCancelled();
	}

	public void onCancel(DialogInterface dialog) {
		this.cancel(false);

	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length > 0) {
			pd.setMessage(values[0]);
		}
	}
}
