package com.kritikalerror.findmepmt;

import com.ks.googleplaceapidemo.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

public class LauncherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launch);
		
		// Set the spinner
		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.search_types, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new SpinnerCheck());

		// Set the button
		final ImageButton activityButton = (ImageButton) findViewById(R.id.find_start);

		activityButton.setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View aView)
				{
					Spinner spinner = (Spinner) findViewById(R.id.spinner1);
					if(!isConnected())
					{
						showConnectionAlertToUser();
					}
					else
					{
						Intent toAnotherActivity = new Intent(aView.getContext(), MainActivity.class);
						toAnotherActivity.putExtra("search_type", String.valueOf(spinner.getSelectedItem()));
						startActivity(toAnotherActivity);
					}
				}
				
				private boolean isConnected()
				{
					ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo wifiCheck = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					NetworkInfo dataCheck = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
					
					return wifiCheck.isConnected() || dataCheck.isConnected();
				}
			}
		);       
	}
	
	private void showConnectionAlertToUser(){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		alertDialogBuilder.setMessage("Internet Connectivity is currently disabled. This app needs the internet to function properly. Please check your connection.");
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton("Enable Data",
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				Intent settingsIntent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
				startActivity(settingsIntent);
			}
		});
		alertDialogBuilder.setNeutralButton("Enable WiFi",
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				Intent settingsIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
				startActivity(settingsIntent);
			}
		});
		alertDialogBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				dialog.cancel();
			}
		});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}
	
	public class SpinnerCheck implements OnItemSelectedListener {
	    public void onItemSelected(AdapterView<?> parent, View view, 
	            int pos, long id) {
	        // An item was selected. You can retrieve the selected item using
	        // parent.getItemAtPosition(pos)
	    	//Toast.makeText(getApplicationContext(), parent.getItemAtPosition(pos).toString(),
			//		Toast.LENGTH_LONG).show();
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	        // Another interface callback
	    }
	}
}
