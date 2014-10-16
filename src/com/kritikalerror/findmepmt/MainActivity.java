package com.kritikalerror.findmepmt;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.exceptions.OAuthConnectionException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kritikalerror.findmepmt.R;

/**
 *  This class is used to search places using Places API using keywords like police,hospital etc.
 * 
 * @author Michael Hii
 * @Date   7/30/2014
 *
 */
public class MainActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
	private final String PARAMS = "boba milk bubble tea tapioca";
	private final int SEARCH_BY_BEST_MATCH = 0;
	private final int SEARCH_BY_DISTANCE = 1;
	private final int SEARCH_BY_RATING = 2;
	private final int UPDATE_INTERVAL = 5;
	private final int FASTEST_INTERVAL = 1;
	private final int DIPS_VALUE = 80;
	private final String Search = "searchKey";
	private int yelpSortChoice = SEARCH_BY_DISTANCE;

	private final String TAG = getClass().getSimpleName();
	
	private GoogleMap mMap;
	private Location mCurrentLocation;
	private String provider;
	private LocationClient mLocationClient;
	private LocationRequest mLocationRequest;
	private AdView mAdView;
	private ProgressDialog mLoadingDialog;
	private SharedPreferences mSharedPrefs;
	
	private boolean mHasFirstSearch = false;
	private boolean mHasRefreshed = false;
	private int mAdHeight;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Disable action bar extras
		ActionBar ab = getActionBar(); 
		ab.setDisplayShowTitleEnabled(false); 
		ab.setDisplayShowHomeEnabled(false);

		Intent intent = getIntent();
		provider = intent.getExtras().getString("search_type");
		
		setSearchType(provider);

		initMap();
		loadAds();
		
		// Perform check to see if Google Play Services is available
		final int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (result != ConnectionResult.SUCCESS) 
		{
			Toast.makeText(this, "Google Play service is not available (status=" + result + ")", Toast.LENGTH_LONG).show();
		}
		
		// Client initiates a listener that will provide updates for locations
		mLocationClient = new LocationClient(this, this, this);
		mLocationRequest = new LocationRequest();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

		// Generate user list of providers to search
		ArrayList<String> list = new ArrayList<String>();
		list.add(provider);
		if(provider.equals("Distance"))
		{
			list.add("Popularity");
			list.add("Rating");
		}
		else if(provider.equals("Popularity"))
		{
			list.add("Distance");
			list.add("Rating");
		}
		else
		{
			list.add("Distance");
			list.add("Popularity");
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, list);
		final ArrayList<String> searchTypeList = list;

		// Initial search
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// When user refreshes
		actionBar.setListNavigationCallbacks(dataAdapter,
				new ActionBar.OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition,
					long itemId) 
			{
				if (mCurrentLocation != null) 
				{
					setSearchType(searchTypeList.get(itemPosition));
					beginQuery();
				}
				return true;
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		mLocationClient.connect();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		// Reset the dialog to prevent window leaking
		if (mLoadingDialog != null) 
		{
			mLoadingDialog.dismiss();
			mLoadingDialog = null;
		}

		mLocationClient.disconnect();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		// Reset the dialog to prevent window leaking
		if (mLoadingDialog != null) 
		{
			mLoadingDialog.dismiss();
			mLoadingDialog = null;
		}

		mLocationClient.disconnect();
	}
	
	/**
	 * Load Ads in MainActivity screen
	 * Don't want to load them first in Launcher,
	 * since user will spend the most time staring at a map
	 */
	private void loadAds()
	{
		// Create and setup the AdMob view
		mAdView = new AdView(this);
		FrameLayout layout = (FrameLayout) findViewById(R.id.map);

		mAdView.setAdSize(AdSize.SMART_BANNER);
		mAdView.setAdUnitId("ca-app-pub-6309606968767978/6485120847");
		AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
		
		// Get the height for offset calculations
		AdSize adSize = mAdView.getAdSize();
		//mAdHeight = adSize.getHeight();
		mAdHeight = adSize.getHeightInPixels(getApplicationContext());
		
		// Add the AdMob view
		FrameLayout.LayoutParams adParams = 
				new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, 
						FrameLayout.LayoutParams.WRAP_CONTENT);

		layout.addView(mAdView, adParams);

		mAdView.loadAd(adRequestBuilder.build());
	}
	
	/**
	 * Set search type for Yelp API to process
	 * 0: Best Match, 1: Distance, 2: Highest Rating found
	 * @param choice
	 */
	private void setSearchType(String choice)
	{
		if(choice.equals("Distance"))
		{
			yelpSortChoice = SEARCH_BY_DISTANCE;
		}
		else if(choice.equals("Popularity"))
		{
			yelpSortChoice = SEARCH_BY_BEST_MATCH;
		}
		else
		{
			yelpSortChoice = SEARCH_BY_RATING;
		}
	}
	
	/**
	 * Private method to begin querying Yelp
	 */
	private void beginQuery()
	{
		if(isConnected())
		{
			mMap.clear();
			new GetYelp(MainActivity.this).execute();
		}
		else
		{
			showConnectionAlertToUser('w');
		}
	}

	private class GetYelp extends AsyncTask<Void, Void, ArrayList<Result>>
	{
		private Context context;

		public GetYelp(Context context) 
		{
			this.context = context;
		}

		// Query Yelp for list of places
		protected ArrayList<Result> doInBackground(Void... params) {
			Yelp yelp = Yelp.getYelp(MainActivity.this);
			try {
				// Get search parameter terms
				String searchParams = PARAMS;
				mSharedPrefs = getSharedPreferences("PMTSettings", Context.MODE_PRIVATE);
				
				// Get shared prefs key, otherwise store the default key inside
				if (mSharedPrefs.contains(Search))
				{
					searchParams = mSharedPrefs.getString(Search, "");
				}
				else
				{
					SharedPreferences.Editor edit = mSharedPrefs.edit();
		            edit.putString(Search, searchParams);
		            edit.commit();
				}
				
				String businesses = yelp.search(searchParams, yelpSortChoice, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
				if(businesses == null)
				{
					businesses = "";
				}
				Log.w(TAG, businesses);
				return processJson(businesses);
			} catch (NullPointerException e) {
				Log.e(TAG, "No businesses found!" + e.getMessage());
				return null;
			} catch (JSONException e) {
				Log.e(TAG, "Error in parsing JSON" + e.getMessage());
				return null;
			} catch (OAuthConnectionException e) {
				Log.e(TAG, "OAuthConnection error, cannot connect to the service" + e.getMessage());
				return null;
			}
		}

		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
			if(mLoadingDialog == null) 
			{
				mLoadingDialog = new ProgressDialog(context, R.style.CustomDialog);
				mLoadingDialog.setCancelable(true);
				mLoadingDialog.setCanceledOnTouchOutside(false);
				mLoadingDialog.setTitle("Loading");
				mLoadingDialog.setMessage("Please Wait...");
				mLoadingDialog.isIndeterminate();
				mLoadingDialog.show();
			}
		}

		// Display results on map
		@Override
		protected void onPostExecute(ArrayList<Result> result) {
			super.onPostExecute(result);
			
			// We only want to create one instance of the dialog to prevent the leaking bug
			if(mLoadingDialog != null) 
			{
				mLoadingDialog.dismiss();
				mLoadingDialog = null;
			}

			CameraPosition cameraPosition;

			// Add marker of current position
			Marker currentMarker = mMap.addMarker(new MarkerOptions().position(
					new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.man))
							.snippet("<font color=\"black\"><b>Current Position</b><br>I am here!</font>")
							.title("Current Position"));

			ArrayList<Marker> markerList = new ArrayList<Marker>();

			if ((result == null) || (result.size() == 0))
			{
				Toast.makeText(getApplicationContext(), "Yelp cannot find any bubble tea place near you!",
						Toast.LENGTH_LONG).show();

				cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
				.zoom(13) // Sets the zoom
				.tilt(30) // Sets the tilt of the camera to 30 degrees
				.build(); // Creates a CameraPosition from the builder
				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
			}
			else
			{					
				// Add markers of all found places
				for (int i = 0; i < result.size(); i++) 
				{
					// Adding the if to safeguard against any failed geocodes
					if(result.get(i).getLatitude() != 0.0 && result.get(i).getLongitude() != 0.0)
					{
						String snippetString = "<font color=\"black\"><b>" + result.get(i).getName() + "</b><br>" + 
								result.get(i).getAddress();

						// Set phone number
						if(result.get(i).getPhone() != null)
						{
							snippetString = snippetString + "<br><u>+1" + 
									result.get(i).getPhone() + "</u></font>";
						}
						else
						{
							snippetString = snippetString + "<br>Phone Number unavailable</font>";
						}
						
						// Titleparser string creates a string that contains multiple hidden info for parsing
						String titleParser = "";
						
						if(result.get(i).getRating() != null)
						{
							titleParser = result.get(i).getRating() + ",";
						}
						else
						{
							snippetString = snippetString + "<br>Rating info unavailable";
							titleParser = "None,";
						}
						
						if(result.get(i).getMobileUrl() != null)
						{
							titleParser = titleParser + result.get(i).getMobileUrl() + ",";
						}
						
						// Final tidbit to let user know to click infowindow for yelp site
						snippetString = snippetString + "<br><font color=\"blue\">Touch me to see Yelp page!</font>";

						MarkerOptions markerOptions = new MarkerOptions();
						markerOptions.position(new LatLng(result.get(i).getLatitude(), result.get(i).getLongitude()));
						if(i == 0)
						{
							markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_star));
						}
						else
						{
							markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
						}
						markerOptions.snippet(snippetString);
						markerOptions.title(titleParser);
						markerOptions.draggable(false);

						Marker marker = mMap.addMarker(markerOptions);
						markerList.add(marker);
					}
				}
				
				mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
				    @Override
				    public void onInfoWindowClick(Marker marker) {
				    	String[] titleParseList = marker.getTitle().split(",");
				    	if(titleParseList.length > 1)
				    	{
				    		Log.w(TAG, titleParseList[1]);
				        
				    		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(titleParseList[1]));
				    		startActivity(browserIntent);
				    	}
				    }
				});

				mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

					@Override
					public View getInfoWindow(Marker marker) {
						return null;
					}

					@Override
					public View getInfoContents(Marker marker) {

						View v = getLayoutInflater().inflate(R.layout.marker, null);

						TextView info= (TextView) v.findViewById(R.id.info);
						ImageView image = (ImageView) v.findViewById(R.id.rating);

						info.setText(Html.fromHtml(marker.getSnippet()));
						
						String[] titleParseList = marker.getTitle().split(",");
						
						Log.e(TAG, "titleParseList is: " + titleParseList[0]);
						if(titleParseList[0].equals("0"))
						{
							image.setImageResource(R.drawable.zero);
						}
						else if(titleParseList[0].equals("1.0"))
						{
							image.setImageResource(R.drawable.one);
						}
						else if(titleParseList[0].equals("1.5"))
						{
							image.setImageResource(R.drawable.onehalf);
						}
						else if(titleParseList[0].equals("2.0"))
						{
							image.setImageResource(R.drawable.two);
						}
						else if(titleParseList[0].equals("2.5"))
						{
							image.setImageResource(R.drawable.twohalf);
						}
						else if(titleParseList[0].equals("3.0"))
						{
							image.setImageResource(R.drawable.three);
						}
						else if(titleParseList[0].equals("3.5"))
						{
							image.setImageResource(R.drawable.threehalf);
						}
						else if(titleParseList[0].equals("4.0"))
						{
							image.setImageResource(R.drawable.four);
						}
						else if(titleParseList[0].equals("4.5"))
						{
							image.setImageResource(R.drawable.fourhalf);
						}
						else if(titleParseList[0].equals("5.0"))
						{
							image.setImageResource(R.drawable.five);
						}
						
						return v;
					}
				});

				final Marker firstMarker = markerList.get(0);

				Log.d(TAG, "result size is: " + result.size());
				
				LatLng currentPosition = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
				LatLng firstPosition = new LatLng(result.get(0).getLatitude(), result.get(0).getLongitude());
				
				LatLngBounds.Builder builder = new LatLngBounds.Builder();
				builder.include(currentPosition);
				builder.include(firstPosition);
				LatLngBounds bounds = builder.build();
				
				// Calculate screen padding for display
				float dpiDensity = context.getResources().getDisplayMetrics().density;		
				int padding = (int)(DIPS_VALUE * dpiDensity) + mAdHeight;
				
				CameraUpdate cameraPositions = CameraUpdateFactory.newLatLngBounds(bounds, padding);
				mMap.animateCamera(cameraPositions, new GoogleMap.CancelableCallback() {
					@Override
					public void onFinish() {
						// TODO Auto-generated method stub
						// Display info window of nearest pmt shop
						firstMarker.showInfoWindow();
					}

					@Override
					public void onCancel() {
						// TODO Auto-generated method stub
					}
				}); 
				
				// Refreshing does not redisplay infowindow
				if(mHasRefreshed)
				{
					firstMarker.showInfoWindow();
					mHasRefreshed = false;
				}
			}

			setProgressBarIndeterminateVisibility(false);
		}

		protected ArrayList<Result> processJson(String jsonStuff) throws JSONException 
		{
			ArrayList<Result> businessList;
			if(jsonStuff.equals(""))
			{
				// Return empty array to display no results found
				businessList = new ArrayList<Result>(0);
			}
			else
			{
				//Log.w(TAG, jsonStuff);
				JSONObject json = new JSONObject(jsonStuff);
	
				JSONArray businesses = json.getJSONArray("businesses");
				businessList = new ArrayList<Result>(businesses.length());
				for (int i = 0; i < businesses.length(); i++) 
				{
					businessList.add(Result.jsonToClass(businesses.getJSONObject(i)));
				}
			}
			return businessList;
		}
	}


	private void initMap() 
	{
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main, menu);
		menu.add(0, 0, 0, "Refresh Search");
		menu.add(0, 1, 0, "Change Search Terms");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        	case 0:
        		Log.d(TAG, "Refreshing with choice: " + yelpSortChoice);
        		mHasRefreshed = true;
        		beginQuery();
        		return super.onOptionsItemSelected(item);
        	case 1:
        		final Dialog searchDialog = new Dialog(MainActivity.this);
				searchDialog.setContentView(R.layout.settings_fragment);
				
				final EditText addSearch = (EditText) searchDialog.findViewById(R.id.search);
				Button saveButton = (Button) searchDialog.findViewById(R.id.saveBtn);
				
				searchDialog.setTitle("Enter Yelp Search Terms");
				
				if (mSharedPrefs.contains(Search))
				{
					addSearch.setText(mSharedPrefs.getString(Search, ""));
				}
				
				saveButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
			            String searchParams = addSearch.getText().toString();
			            SharedPreferences.Editor edit = mSharedPrefs.edit();
			            edit.putString(Search, searchParams);
			            edit.commit();
			            searchDialog.dismiss();
					}
					
				});
				searchDialog.show();
        		return super.onOptionsItemSelected(item);
        	default:
        		return super.onOptionsItemSelected(item);
		}
	}

	private void showConnectionAlertToUser(char type){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		
		String buttonMsg;
		String intentChoice;
		boolean isData = false;
		
		if(type == 'g') //GPS is disabled but location network is running
		{
			alertDialogBuilder.setMessage("GPS is currently disabled. You can get better results by enabling GPS. Would you like to enable it?");
			buttonMsg = "Enable GPS";
			intentChoice = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
		}
		else if(type == 'l')
		{
			alertDialogBuilder.setMessage("Google Location Services is currently disabled. This app needs Google Location Services to function properly. Please enable Google Location Services.");
			buttonMsg = "Enable Location Services";
			intentChoice = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
		}
		else
		{
			alertDialogBuilder.setMessage("Internet Connectivity is currently disabled. This app needs the internet to function properly. Please check your connection.");
			buttonMsg = "Enable Data";
			isData = true;
			intentChoice = android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS;
		}
		
		final String finalChoice = intentChoice;
		
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton(buttonMsg,
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				Intent settingsIntent = new Intent(finalChoice);
				startActivity(settingsIntent);
			}
		});
		if(isData)
		{
			alertDialogBuilder.setNeutralButton("Enable WiFi",
					new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int id){
					Intent settingsIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
					startActivity(settingsIntent);
				}
			});
		}
		alertDialogBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				dialog.cancel();
			}
		});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}
	
	private boolean isConnected()
	{
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiCheck = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		NetworkInfo dataCheck = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		return wifiCheck.isConnected() || dataCheck.isConnected();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Connection Failed", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
		mCurrentLocation = mLocationClient.getLastLocation();
		
		if(mCurrentLocation == null)
		{
			final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

		    if(!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) 
		    {
		    	showConnectionAlertToUser('l');
		    }
		    else if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		    {
		    	showConnectionAlertToUser('g');
		    }
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
			Toast.makeText(this, "Cannot find current location, searching...", Toast.LENGTH_LONG).show();
		}
		else
		{
			if(!mHasFirstSearch)
			{
				// Animate to initial position
				CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
				.zoom(13) // Sets the zoom
				.tilt(30) // Sets the tilt of the camera to 30 degrees
				.build(); // Creates a CameraPosition from the builder
				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
				
				mHasFirstSearch = true;
				beginQuery();
			}
		}
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onLocationChanged(Location arg0) {
		// TODO Auto-generated method stub
		mCurrentLocation = mLocationClient.getLastLocation();
		mLocationClient.removeLocationUpdates(this);
		
		// Reset the dialog to prevent window leaking
		if (mLoadingDialog != null) 
		{
			mLoadingDialog.dismiss();
			mLoadingDialog = null;
		}
		
		Toast.makeText(this, "Location found! Searching now.", Toast.LENGTH_LONG).show();
		if(!mHasFirstSearch)
		{
			mHasFirstSearch = true;
			beginQuery();
		}
	}

}
