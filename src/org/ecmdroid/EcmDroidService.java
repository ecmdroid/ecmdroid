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

import org.ecmdroid.activity.LogActivity;

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
	public static final String REALTIME_DATA = "org.ecmdroid.Service.realtimedataevent";
	public static final String RECORDING_STARTED = "org.ecmdroid.Service.recording_started";
	public static final String RECORDING_STOPPED = "org.ecmdroid.Service.recording_stopped";
	public static final String TAG = "EcmDroidService";

	private static final int RECORDING_ID = 1;
	private static final Intent INTENT = new Intent(REALTIME_DATA);

	private final IBinder binder = new EcmDroidBinder();
	private NotificationManager nm;

	private boolean recording = false;
	private int recordingInterval;
	private long recordingStarted;
	private boolean reading;
	private ReaderThread readerThread;
	private File currentLog;
	private DataOutputStream logstream;
	private long bytesLogged;
	private long recordsLogged;
	private long readFailures;
	private ECM ecm;

	public class EcmDroidBinder extends Binder {
		public EcmDroidService getService() {
			return EcmDroidService.this;
		}
	}
	@Override
	public void onCreate() {
		ecm = ECM.getInstance(this);
		nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(RECORDING_ID); // Remove possible left-overs from a crash
		readerThread = new ReaderThread();
		readerThread.start();
		Log.d(TAG, "Service created.");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopRecording();
		stopReading();
		readerThread.shutdown();
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
		return recordsLogged;
	}

	public long getReadFailures() {
		return readFailures;
	}

	public float getLogsPerSecond() {
		return (float) (recordsLogged / (System.currentTimeMillis() - recordingStarted) * 1000.0);
	}

	public String getLogfile() {
		return currentLog == null ? "" : currentLog.getName();
	}

	/**
	 * Get the currently used Log file or null, if logging is not active.
	 */
	public File getCurrentFile() {
		return currentLog;
	}

	public synchronized void startRecording(File log, int interval, ECM ecm) throws IOException {

		if (recording) {
			return;
		}
		this.recordingInterval = interval;
		sendBroadcast(new Intent(RECORDING_STARTED));
		bytesLogged = recordsLogged = readFailures = 0;
		currentLog = log;
		logstream = new DataOutputStream(new FileOutputStream(log));
		String id = "UNKWN";
		if (ecm.getEEPROM() != null) {
			id = ecm.getEEPROM().getId();
		}
		logstream.write(id.getBytes(), 0, 5);
		synchronized(readerThread) {
			recording = true;
			readerThread.notify();
		}
		Log.i(TAG, "Recording started.");
		ecm.setRecording(true);
		recordingStarted = System.currentTimeMillis();
		showNotification(getString(R.string.app_name), getString(R.string.recording_started));
	}

	public synchronized void stopRecording() {
		recording = false;
		if (logstream != null) {
			try {
				logstream.flush();
				logstream.close();
			} catch (Exception ioe) {}
			logstream = null;
		}
		// Turn off notification
		nm.cancel(RECORDING_ID);
		Log.i(TAG, "Recording stopped.");
		ecm.setRecording(false);
		recordingInterval = 0;
		sendBroadcast(new Intent(RECORDING_STOPPED));
	}

	public boolean isRecording() {
		return recording;
	}

	public int getRecordingInterval() {
		return recordingInterval;
	}

	public synchronized void startReading() {
		synchronized(readerThread) {
			reading = true;
			readerThread.notify();
		}
		Log.i(TAG, "RT Data read started.");
	}

	public synchronized void stopReading() {
		synchronized(readerThread) {
			reading = false;
			readerThread.notify();
		}
		Log.i(TAG, "RT Data read stopped.");
	}

	private class ReaderThread extends Thread
	{
		private static final int DEFAULT_INTERVAL = 250;
		private boolean running = true;

		private ReaderThread() {
			super("ECM-Reader-Thread");
		}

		@Override
		public void run() {
			long now = 0;
			ECM ecm = ECM.getInstance(EcmDroidService.this);
			while (running) {
				if ( !(ecm.isConnected() && (recording || reading)) ) {
					synchronized(this) {
						try {
							this.wait();
						} catch (InterruptedException e) {
							continue;
						}
					}
				}
				int i = recordingInterval;
				now = System.currentTimeMillis();
				try {
					byte[] data = ecm.readRTData();
					sendBroadcast(INTENT);
					if (recording) {
						logPacket(data);
					}
				} catch (Exception e) {
					readFailures++;
					if (i == 0) {
						i = DEFAULT_INTERVAL;
					}
				}
				if (running && i != 0) {
					long toSleep = i - (System.currentTimeMillis() - now);
					if (toSleep > 0) {
						try {
							Thread.sleep(toSleep);
						} catch (InterruptedException e) {}
					}
				}
			}
			Log.d(TAG, "ReaderThread terminated.");
		}

		public void shutdown() {
			synchronized(this) {
				running = false;
				this.notify();
			}
			try {
				this.join();
			} catch (InterruptedException e) {}
		}
	}

	private void showNotification(String label, String text) {
		Notification notification = new Notification(R.drawable.notify_log, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, LogActivity.class), 0);
		notification.setLatestEventInfo(this, label, text, contentIntent);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		nm.notify(RECORDING_ID, notification);
	}

	private synchronized void logPacket(byte[] data) throws IOException {
		if (logstream != null) {
			logstream.writeInt((int) (System.currentTimeMillis() - recordingStarted) / 10);
			logstream.write(data);
			bytesLogged += (data.length + 4);
			recordsLogged++;
		}
	}

	public boolean isReading() {
		return reading;
	}
}
