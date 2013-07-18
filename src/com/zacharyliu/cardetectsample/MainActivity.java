package com.zacharyliu.cardetectsample;

import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionReceiver;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService.CarSoundDetectionBinder;
import com.zacharyliu.carsounddetectionlibrary.analyzer.Result;

public class MainActivity extends Activity {
	boolean mBound = false;
	protected CarSoundDetectionBinder mBinder;
	private TextView resultText;
	private ProgressBar resultBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		resultText = (TextView) findViewById(R.id.resultText);
		resultBar = (ProgressBar) findViewById(R.id.resultBar);
		
		// Bind to service in CarSoundDetectionLibrary
		Intent intent = new Intent(this, CarSoundDetectionService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
		log("Activity created");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		log("Destroyed activity");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mBound) {
			mBinder.start(this, callback);
		}
		log("Resumed processing");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (mBound) {
			mBinder.stop();
		}
		log("Stopped processing");
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mBound = true;
			mBinder = (CarSoundDetectionBinder) binder;
			log("Service bound");
			mBinder.start(MainActivity.this, callback);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBound = false;
		}
		
	};
	
	private CarSoundDetectionReceiver callback = new CarSoundDetectionReceiver() {

		@Override
		public void onResult(Result result) {
			log("Got result");
			Double percent = result.getResult();
			resultBar.setProgress((int) (percent * resultBar.getMax()));
			String text = String.format(Locale.getDefault(), "%1$,.2f", percent);
			resultText.setText(text);
		}
		
	};
	
	public static void log(String message) {
		Log.d("Message!", message);
	}

}
