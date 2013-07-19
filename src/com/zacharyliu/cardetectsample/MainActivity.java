package com.zacharyliu.cardetectsample;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionReceiver;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService.CarSoundDetectionBinder;
import com.zacharyliu.carsounddetectionlibrary.analyzer.FeatureVector;

public class MainActivity extends Activity {
	boolean mBound = false;
	protected CarSoundDetectionBinder mBinder;
	private TextView resultText;
	private ProgressBar resultBar;
	private XYPlot graph;
	private final int NUM_SERIES = 14;
	private List<SimpleXYSeries> graphSeries = new ArrayList<SimpleXYSeries>(NUM_SERIES);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		resultText = (TextView) findViewById(R.id.resultText);
		resultBar = (ProgressBar) findViewById(R.id.resultBar);
		graph = (XYPlot) findViewById(R.id.graph);
		graph.setDomainBoundaries(0, 30, BoundaryMode.FIXED);
//		graph.setRangeBoundaries(0, 1, BoundaryMode.FIXED);
		
		@SuppressWarnings("deprecation")
		LineAndPointFormatter f1 = new LineAndPointFormatter(Color.rgb(0, 0, 200), null, Color.rgb(0, 0, 80));
        f1.getFillPaint().setAlpha(20);
		
		for (int i=0; i<NUM_SERIES; i++) {
			graphSeries.add(new SimpleXYSeries(Integer.toString(i)));
			graphSeries.get(i).useImplicitXVals();
			graph.addSeries(graphSeries.get(i), f1);
		}
		
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
		public void onResult(FeatureVector vector) {
			log("Got result");
			Double percent = vector.getResult();
			resultBar.setProgress((int) (percent * resultBar.getMax()));
			String text = String.format(Locale.getDefault(), "[%1$,.5f, %2$,.5f]", vector.result[0], vector.result[1]);
			resultText.setText(text);
			for (int i=0; i<NUM_SERIES; i++) {
				if (graphSeries.get(i).size() > 30) {
					graphSeries.get(i).removeFirst();
				}
				graphSeries.get(i).addLast(null, vector.get(i));
			}
			graph.redraw();
		}
		
	};
	
	public static void log(String message) {
		Log.d("Message!", message);
	}

}
