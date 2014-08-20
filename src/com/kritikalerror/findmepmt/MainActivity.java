package com.kritikalerror.findmepmt;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
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
import com.ks.googleplaceapidemo.R;

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
	private int yelpSortChoice = SEARCH_BY_DISTANCE;

	private final String TAG = getClass().getSimpleName();
	
	private GoogleMap mMap;
	private Location mCurrentLocation;
	private String provider;
	private LocationClient mLocationClient;
	private LocationRequest mLocationRequest;
	private AdView mAdView;
	
	private boolean mHasFirstSearch = false;

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
			showConnectionAlertToUser('w');
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
					mMap.clear();
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
		mAdView.setAdUnitId("ca-app-pub-6309606968767978/2177105243");
		AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
		
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
		new GetYelp(MainActivity.this).execute();
	}

	private class GetYelp extends AsyncTask<Void, Void, ArrayList<Result>>
	{
		private ProgressDialog dialog;
		private Context context;

		public GetYelp(Context context) 
		{
			this.context = context;
		}

		// Query Yelp for list of places
		protected ArrayList<Result> doInBackground(Void... params) {
			Yelp yelp = Yelp.getYelp(MainActivity.this);
			String businesses = yelp.search(PARAMS, yelpSortChoice, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
			try {
				return processJson(businesses);
			} catch (JSONException e) {
				Log.e("errorage", e.getMessage());
				return null;
			}
		}

		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
			dialog = new ProgressDialog(context);
			dialog.setCancelable(false);
			dialog.setMessage("Loading..");
			dialog.isIndeterminate();
			dialog.show();
		}

		// Display results on map
		@Override
		protected void onPostExecute(ArrayList<Result> result) {
			super.onPostExecute(result);
			if (dialog.isShowing()) 
			{
				dialog.dismiss();
			}

			CameraPosition cameraPosition;

			// Add marker of current position
			Marker currentMarker = mMap.addMarker(new MarkerOptions().position(
					new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.man))
							.snippet("<b>Current Position</b><br>I am here!")
							.title("Current Position"));

			ArrayList<Marker> markerList = new ArrayList<Marker>();

			if (result.size() == 0)
			{
				Toast.makeText(getApplicationContext(), "Yelp cannot find any PMT place near you!",
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
					if(result.get(i).getLatitude() != null && result.get(i).getLongitude() != null)
					{
						String snippetString = "<b>" + result.get(i).getName() + "</b><br>" + 
								result.get(i).getAddress();

						// Set phone number
						if(result.get(i).getPhone() != null)
						{
							snippetString = snippetString + "<br><u>+1" + 
									result.get(i).getPhone() + "</u>";
						}
						else
						{
							snippetString = snippetString + "<br>Phone Number unavailable";
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
						markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
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
				        Log.w(TAG, titleParseList[1]);
				        
				        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(titleParseList[1]));
				        startActivity(browserIntent);
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
				int padding = (int)(DIPS_VALUE * dpiDensity);
				
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
			}

			setProgressBarIndeterminateVisibility(false);
		}

		protected ArrayList<Result> processJson(String jsonStuff) throws JSONException 
		{
			//Log.w(TAG, jsonStuff);
			JSONObject json = new JSONObject(jsonStuff);

			JSONArray businesses = json.getJSONArray("businesses");
			ArrayList<Result> businessList = new ArrayList<Result>(businesses.length());
			for (int i = 0; i < businesses.length(); i++) 
			{
				businessList.add(Result.jsonToClass(businesses.getJSONObject(i)));
			}
			return businessList;
		}
	}


	private void initMap() 
	{
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main, menu);
		menu.add(0, 0, 0, "Refresh Search");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "Refreshing with choice: " + yelpSortChoice);
		beginQuery();
		return super.onOptionsItemSelected(item);
	}

	private void showConnectionAlertToUser(char type){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		
		String buttonMsg;
		String intentChoice;
		
		if(type == 'g') //GPS is disabled but location network is running
		{
			alertDialogBuilder.setMessage("GPS is currently disabled. You can get better results by enabling GPS. Would you like to enable it?");
			buttonMsg = "Enable GPS";
			intentChoice = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
		}
		else if(type == 'l')
		{
			alertDialogBuilder.setMessage("Location Services is currently disabled. This app needs Location Services to function properly. Please enable Location Services.");
			buttonMsg = "Enable Location Services";
			intentChoice = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
		}
		else
		{
			alertDialogBuilder.setMessage("WiFi is currently disabled. This app needs WiFi to function properly. Please enable WiFi.");
			buttonMsg = "Enable WiFi";
			intentChoice = android.provider.Settings.ACTION_WIFI_SETTINGS;
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
		alertDialogBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				dialog.cancel();
			}
		});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
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
		mLocationClient.removeLocationUpdates(this);
		
		if(!mHasFirstSearch)
		{
			mHasFirstSearch = true;
			beginQuery();
		}
	}

}
