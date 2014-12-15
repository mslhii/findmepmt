package com.kritikalerror.findmepmt;

import com.kritikalerror.findmepmt.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class LauncherActivity extends Activity {
	
	SharedPreferences mSharedPreferences;
	public static final String Search = "searchKey";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launch);
		
		mSharedPreferences = getSharedPreferences("PMTSettings", Context.MODE_PRIVATE);
		
		// Set the button
		final ImageButton distanceButton = (ImageButton) findViewById(R.id.find_distance);
		final ImageButton popularButton = (ImageButton) findViewById(R.id.find_popular);
		final ImageButton ratingButton = (ImageButton) findViewById(R.id.find_rating);
		final ImageButton aboutButton = (ImageButton) findViewById(R.id.about);
		final ImageButton settingsButton = (ImageButton) findViewById(R.id.bubbles);
		final ImageButton strawButton = (ImageButton) findViewById(R.id.straw);
		
		settingsButton.setOnClickListener(
				new View.OnClickListener()
				{
					public void onClick(View aView)
					{
						final Dialog searchDialog = new Dialog(LauncherActivity.this);
						searchDialog.setContentView(R.layout.settings_fragment);
						
						final EditText addSearch = (EditText) searchDialog.findViewById(R.id.search);
						Button saveButton = (Button) searchDialog.findViewById(R.id.saveBtn);
						
						searchDialog.setTitle("Enter Yelp Search Terms");
						
						if (mSharedPreferences.contains(Search))
						{
							addSearch.setText(mSharedPreferences.getString(Search, ""));
						}
						
						saveButton.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								// TODO Auto-generated method stub
					            String searchParams = addSearch.getText().toString();
					            SharedPreferences.Editor edit = mSharedPreferences.edit();
					            edit.putString(Search, searchParams);
					            edit.commit();
					            searchDialog.dismiss();
							}
							
						});
						searchDialog.show();
					}
				}
			);  

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
		
		strawButton.setOnClickListener(
				new View.OnClickListener()
				{
					public void onClick(View aView)
					{
						AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LauncherActivity.this);			 
						alertDialogBuilder.setTitle("PMT is NOT called Boba!");
						alertDialogBuilder
							.setMessage(R.string.pmt)
							.setCancelable(false)
							.setPositiveButton("OK",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
									dialog.cancel();
								}
							  })
							.setNegativeButton("Copy",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
									copyText();
									dialog.cancel();
								}
							  });
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
					}
				}
			);
	}
	
	public void saveToPreferences(String fileName, String data) {
        SharedPreferences myPrefs = getSharedPreferences("Search", MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString(fileName, data);
        prefsEditor.commit();
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
	
	private void copyText()
	{
		int sdk = android.os.Build.VERSION.SDK_INT;
		if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
		    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		    clipboard.setText(getString(R.string.pmt));
		} else {
		    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
		    android.content.ClipData clip = android.content.ClipData.newPlainText("PMT", getString(R.string.pmt));
		    clipboard.setPrimaryClip(clip);
		}
		
		Toast.makeText(this, "Copied this copypasta to your clipboard!", Toast.LENGTH_SHORT).show();
	}
}
