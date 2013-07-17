package com.zacharyliu.cardetectsample;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionReceiver;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService.CarSoundDetectionBinder;

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
		
		Log.d("Message", "Activity created");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mBound = true;
			mBinder = (CarSoundDetectionBinder) binder;
			Log.d("Message", "Service bound");
			//mBinder.start();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBound = false;
		}
		
	};
	
	private CarSoundDetectionReceiver callback = new CarSoundDetectionReceiver() {

		@Override
		public void onResult(List<Integer> result) {
			int total = result.get(0) + result.get(1);
			int progress = (int) (total / 2.0) * resultBar.getMax();
			resultBar.setProgress(progress);
			resultText.setText(total);
		}
		
	};

}
