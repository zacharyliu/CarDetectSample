package com.zacharyliu.cardetectsample;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import au.com.bytecode.opencsv.CSVWriter;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionReceiver;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService;
import com.zacharyliu.carsounddetectionlibrary.CarSoundDetectionService.CarSoundDetectionBinder;
import com.zacharyliu.carsounddetectionlibrary.Constants;
import com.zacharyliu.carsounddetectionlibrary.analyzer.FeatureVector;

public class MainActivity extends Activity implements OnItemSelectedListener, OnClickListener {
	private boolean mBound = false;
	private boolean mStarted = false;
	protected CarSoundDetectionBinder mBinder;
	private TextView resultText;
	private ProgressBar resultBar;
	private XYPlot graph;
	private final int NUM_SERIES = Constants.NEURON_COUNTS[0];
	private SimpleXYSeries graphSeries;
	private Spinner spinner;
	private int currentDisplayPos = 0;
	private ToggleButton toggle;
	private CSVWriter writer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		resultText = (TextView) findViewById(R.id.resultText);
		resultBar = (ProgressBar) findViewById(R.id.resultBar);
		graph = (XYPlot) findViewById(R.id.graph);
		graph.setDomainBoundaries(0, 30, BoundaryMode.FIXED);
//		graph.setRangeBoundaries(0, 1, BoundaryMode.FIXED);
		
		newSeries();
		
		// Bind to service in CarSoundDetectionLibrary
		Intent intent = new Intent(this, CarSoundDetectionService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
		// Initialize feature selector spinner
		spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.features, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);
		
		toggle = (ToggleButton) findViewById(R.id.toggle);
		toggle.setOnClickListener(this);
		
		File sdCard = Environment.getExternalStorageDirectory();
		try {
			String filename = sdCard.getAbsolutePath() + "/CarDetect_" + Long.toString(System.currentTimeMillis()) + ".csv";
			writer = new CSVWriter(new FileWriter(filename));
			Toast.makeText(this, "Logging to: " + filename, Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log("Activity created");
	}
	
	protected void startService() {
		if (mBound && !mStarted) {
			mBinder.start(this, callback);
			toggle.setChecked(true);
			mStarted = true;
		}
	}
	
	protected void stopService() {
		if (mBound && mStarted) {
			mBinder.stop();
			toggle.setChecked(false);
			mStarted = false;
		}
	}
	
	private void newSeries() {
		@SuppressWarnings("deprecation")
		LineAndPointFormatter f1 = new LineAndPointFormatter(Color.rgb(0, 0, 200), null, Color.rgb(0, 0, 80));
        f1.getFillPaint().setAlpha(20);
		
        graph.clear();
		graphSeries = new SimpleXYSeries("Feature");
		graphSeries.useImplicitXVals();
		graph.addSeries(graphSeries, f1);
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
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log("Destroyed activity");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startService();
		log("Resumed processing");
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		stopService();
		log("Stopped processing");
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mBound = true;
			mBinder = (CarSoundDetectionBinder) binder;
			log("Service bound");
			startService();
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
			if (graphSeries.size() > 30) {
				graphSeries.removeFirst();
			}
			graphSeries.addLast(null, vector.get(currentDisplayPos));
			graph.redraw();
			
			writer.writeNext(vector.toStringArray());
		}
		
	};
	
	public static void log(String message) {
		Log.d("Message!", message);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		// Set the graph to display the specified position in the feature vector
		currentDisplayPos = pos;
		newSeries();
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}

	@Override
	public void onClick(View v) {
		if (mStarted) {
			stopService();
		} else {
			startService();
		}
	}

}
