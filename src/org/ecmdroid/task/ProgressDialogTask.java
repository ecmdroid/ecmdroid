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
package org.ecmdroid.task;

import org.ecmdroid.Utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Base class for async tasks with a progress dialog.
 */
public abstract class ProgressDialogTask extends AsyncTask<Void, String, Exception>
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
		pd.setCancelable(false);
	}

	@Override
	protected void onPostExecute(Exception result) {
		pd.dismiss();
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

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length > 0) {
			pd.setMessage(values[0]);
		}
	}
}
