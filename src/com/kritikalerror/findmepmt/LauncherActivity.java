package com.kritikalerror.findmepmt;

import com.kritikalerror.findmepmt.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class LauncherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launch);
		
		// Set the button
		final ImageButton distanceButton = (ImageButton) findViewById(R.id.find_distance);
		final ImageButton popularButton = (ImageButton) findViewById(R.id.find_popular);
		final ImageButton ratingButton = (ImageButton) findViewById(R.id.find_rating);
		final ImageButton aboutButton = (ImageButton) findViewById(R.id.about);

		distanceButton.setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View aView)
				{
					if(!isConnected())
					{
						showConnectionAlertToUser();
					}
					else
					{
						Intent toAnotherActivity = new Intent(aView.getContext(), MainActivity.class);
						toAnotherActivity.putExtra("search_type", "Distance");
						startActivity(toAnotherActivity);
					}
				}
			}
		);  
		
		popularButton.setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View aView)
				{
					if(!isConnected())
					{
						showConnectionAlertToUser();
					}
					else
					{
						Intent toAnotherActivity = new Intent(aView.getContext(), MainActivity.class);
						toAnotherActivity.putExtra("search_type", "Popularity");
						startActivity(toAnotherActivity);
					}
				}
			}
		);
		
		ratingButton.setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View aView)
				{
					if(!isConnected())
					{
						showConnectionAlertToUser();
					}
					else
					{
						Intent toAnotherActivity = new Intent(aView.getContext(), MainActivity.class);
						toAnotherActivity.putExtra("search_type", "Rating");
						startActivity(toAnotherActivity);
					}
				}
			}
		);
		
		aboutButton.setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View aView)
				{
					CustomDialogClass aboutUs = new CustomDialogClass(LauncherActivity.this);
					aboutUs.show();
				}
			}
		);
	}
	
	private boolean isConnected()
	{
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiCheck = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		NetworkInfo dataCheck = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		return wifiCheck.isConnected() || dataCheck.isConnected();
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
}
