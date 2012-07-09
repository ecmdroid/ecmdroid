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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


public class EcmDroidService extends Service
{
	private static final int RECORDING_ID = 1;
	public static final String REALTIME_DATA = "org.ecmdroid.Service.realtimedataevent";
	public static final String RECORDING_STARTED = "org.ecmdroid.Service.recording_started";
	public static final String RECORDING_STOPPED = "org.ecmdroid.Service.recording_stopped";
	public static final String TAG = "EcmDroidService";

	private boolean recording = false;
	private File currentLog;
	private Thread logThread;
	private long bytesLogged;
	private long records;

	public class EcmDroidBinder extends Binder {
		EcmDroidService getService() {
			return EcmDroidService.this;
		}
	}
	private final IBinder binder = new EcmDroidBinder();
	private NotificationManager nm;

	@Override
	public void onCreate() {
		nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(RECORDING_ID); // Remove possible left-overs from a crash
		Log.d(TAG, "Service created.");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopRecording();
		Log.d(TAG,"Service destroyed.");
	}
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Bound to service.");
		return binder;
	}

	public long getBytes() {
		return bytesLogged;
	}

	public long getRecords() {
		return records;
	}

	public String getLogfile() {
		return currentLog == null ? "" : currentLog.getName();
	}

	public synchronized void startRecording(File log, int interval, ECM ecm) throws IOException {
		if (recording) {
			return;
		}
		sendBroadcast(new Intent(RECORDING_STARTED));
		bytesLogged = records = 0;
		currentLog = log;
		DataOutputStream out = new DataOutputStream(new FileOutputStream(log));
		String id = "UNKWN";
		if (ecm.getEEPROM() != null) {
			id = ecm.getEEPROM().getId();
		}
		out.write(id.getBytes(), 0, 5);

		Logger logger = new Logger(out, ecm, interval);
		logThread = new Thread(logger, "EcmDroidLogger");
		ecm.setRecording(true);
		recording = true;
		logThread.start();
		showNotification(getString(R.string.app_name), getString(R.string.recording_started));
	}

	public synchronized void stopRecording() {
		recording = false;
		if (logThread != null) {
			try {
				logThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	private class Logger implements Runnable {
		private DataOutputStream out;
		private ECM ecm;
		private int interval;

		private Logger(DataOutputStream out, ECM ecm, int interval) {
			this.out = out;
			this.ecm = ecm;
			this.interval = interval;
		}

		public void run() {
			Log.i(TAG, "Recording started.");
			long started = System.currentTimeMillis();
			long now = 0;
			while (recording && ecm.isConnected()) {
				if (interval != 0) now = System.currentTimeMillis();
				try {
					byte[] data = ecm.readRTData();
					sendBroadcast(new Intent(REALTIME_DATA));
					out.writeInt((int) (System.currentTimeMillis() - started) / 10);
					out.write(data);
					bytesLogged += data.length;
					records += 1;
				} catch (IOException e) {
					try {
						if (interval == 0) Thread.sleep(100);
					} catch (InterruptedException ioe) {}
				}
				if (interval != 0) {
					long toSleep = interval - (System.currentTimeMillis() - now);
					if (toSleep > 0) {
						try {
							Thread.sleep(toSleep);
						} catch (InterruptedException e) {}
					}
				}
			}
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Turn off notification
			nm.cancel(RECORDING_ID);
			ecm.setRecording(false);
			sendBroadcast(new Intent(RECORDING_STOPPED));
			Log.i(TAG, "Recording stopped.");
		}
	}

	private void showNotification(String label, String text) {
		Notification notification = new Notification(R.drawable.notify_log, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, LogActivity.class), 0);
		notification.setLatestEventInfo(this, label, text, contentIntent);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		nm.notify(RECORDING_ID, notification);
	}
}
