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
import android.location.Criteria;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
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
public class MainActivity extends Activity {
	private final String PARAMS = "boba milk bubble tea tapioca";
	private final int SEARCH_BY_BEST_MATCH = 0;
	private final int SEARCH_BY_DISTANCE = 1;
	private final int SEARCH_BY_RATING = 2;
	private int yelpSortChoice = SEARCH_BY_DISTANCE;

	private final String TAG = getClass().getSimpleName();
	private static final int DISTANCE_OPTION = Menu.FIRST;
	private static final int RATING_OPTION = Menu.FIRST + 1;
	private static final int BEST_MATCH_OPTION = Menu.FIRST + 2;
	
	private GoogleMap mMap;
	private LocationManager locationManager;
	private Location loc;
	private String provider;
	
	private ProgressDialog locateDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
		provider = intent.getExtras().getString("provider");

		initMap();

		// Check for any location/GPS access
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
				!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			showGPSDisabledAlertToUser(false);
		}
		else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			showGPSDisabledAlertToUser(true);
		}

		// Generate user list of providers to search
		ArrayList<String> list = new ArrayList<String>();
		list.add(provider);
		if(provider.equals("Yelp"))
		{
			list.add("Google");
		}
		else
		{
			list.add("Yelp");
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, list);
		final ArrayList<String> providerList = list;

		// Initial search
		currentLocation();
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// When user refreshes
		actionBar.setListNavigationCallbacks(dataAdapter,
				new ActionBar.OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition,
					long itemId) 
			{
				if (loc != null) 
				{
					mMap.clear();
					beginQuery(providerList.get(itemPosition));
				}
				return true;
			}
		});
	}
	
	private void beginQuery(String providerChoice)
	{
		if(providerChoice.equals("Google"))
		{
			new GetPlaces(MainActivity.this).execute();
			provider = "Google";
		}
		else if(providerChoice.equals("Yelp"))
		{
			new GetYelp(MainActivity.this).execute();
			provider = "Yelp";
		}
		else
		{
			Toast.makeText(getApplicationContext(), "Invalid provider selected!",
					Toast.LENGTH_LONG).show();
		}
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
			String businesses = yelp.search(PARAMS, yelpSortChoice, loc.getLatitude(), loc.getLongitude());
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
			mMap.addMarker(new MarkerOptions()
			.position(
					new LatLng(loc.getLatitude(), loc.getLongitude()))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.man))
							.snippet("I am here!"));

			ArrayList<Marker> markerList = new ArrayList<Marker>();

			if (result.size() == 0)
			{
				Toast.makeText(getApplicationContext(), "Yelp cannot find any PMT place near you!",
						Toast.LENGTH_LONG).show();

				cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(loc.getLatitude(), loc.getLongitude()))
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
						String snippetString = result.get(i).getName() + "\n" + 
								result.get(i).getRating() + " stars\n" + 
								result.get(i).getAddress();

						if(result.get(i).getPhone() != null)
						{
							snippetString = snippetString + "\n" + result.get(i).getPhone();
						}

						MarkerOptions markerOptions = new MarkerOptions();
						markerOptions.position(new LatLng(result.get(i).getLatitude(), result.get(i).getLongitude()));
						markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
						markerOptions.snippet(snippetString);

						Marker marker = mMap.addMarker(markerOptions);
						markerList.add(marker);
					}
				}

				mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

					@Override
					public View getInfoWindow(Marker arg0) {
						return null;
					}

					@Override
					public View getInfoContents(Marker marker) {

						View v = getLayoutInflater().inflate(R.layout.marker, null);

						TextView info= (TextView) v.findViewById(R.id.info);

						info.setText(marker.getSnippet());

						return v;
					}
				});

				final Marker firstMarker = markerList.get(0);

				cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(result.get(0).getLatitude(), result
						.get(0).getLongitude())) // Sets camera to first result
						.zoom(14) // Sets the zoom
						.tilt(30) // Sets the tilt of the camera to 30 degrees
						.build(); // Creates a CameraPosition from the builder
				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
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

		ArrayList<Result> processJson(String jsonStuff) throws JSONException 
		{
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

	private class GetPlaces extends AsyncTask<Void, Void, ArrayList<Place>> 
	{
		private ProgressDialog dialog;
		private Context context;

		public GetPlaces(Context context) 
		{
			this.context = context;
		}

		@Override
		protected void onPostExecute(ArrayList<Place> result) 
		{
			super.onPostExecute(result);

			CameraPosition cameraPosition;

			if (dialog.isShowing()) 
			{
				dialog.dismiss();
			}

			ArrayList<Marker> markerList = new ArrayList<Marker>();

			// Add marker of current position
			mMap.addMarker(new MarkerOptions()
			.position(
					new LatLng(loc.getLatitude(), loc.getLongitude()))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.man))
							.snippet("I am here!"));

			if (result.size() == 0)
			{
				Toast.makeText(getApplicationContext(), "Google cannot find any PMT place near you!",
						Toast.LENGTH_LONG).show();

				cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(loc.getLatitude(), loc.getLongitude()))
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
					String snippetString = result.get(i).getName() + "\n" +
							result.get(i).getVicinity();

					Marker marker = mMap.addMarker(new MarkerOptions()
					.title(result.get(i).getName())
					.position(
							new LatLng(result.get(i).getLatitude(), result
									.get(i).getLongitude()))
									.icon(BitmapDescriptorFactory
											.fromResource(R.drawable.pin))
											.snippet(snippetString));

					markerList.add(marker);
				}
				final Marker firstMarker = markerList.get(0);

				cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(result.get(0).getLatitude(), result
						.get(0).getLongitude())) // Sets the center of the map to
						// Mountain View
						.zoom(14) // Sets the zoom
						.tilt(30) // Sets the tilt of the camera to 30 degrees
						.build(); // Creates a CameraPosition from the builder

				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
					@Override
					public void onFinish() {
						//DO some stuff here!
						Log.d("animation", "onFinishCalled");
						firstMarker.showInfoWindow();
					}

					@Override
					public void onCancel() {
						Log.d("animation", "onCancel");
					}
				}); 
				
				mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

					@Override
					public View getInfoWindow(Marker arg0) {
						return null;
					}

					@Override
					public View getInfoContents(Marker marker) {

						View v = getLayoutInflater().inflate(R.layout.marker, null);

						TextView info= (TextView) v.findViewById(R.id.info);

						info.setText(marker.getSnippet());

						return v;
					}
				});
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

		@Override
		protected ArrayList<Place> doInBackground(Void... arg0) 
		{
			PlacesService service = new PlacesService("AIzaSyD-RjNYm-VCo1rtTwHqjIi8XQz29UAra4M");
			ArrayList<Place> findPlaces; 
			findPlaces = service.findPlaces(loc.getLatitude(), loc.getLongitude());

			return findPlaces;
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
		menu.add(0, DISTANCE_OPTION, 0, "Search by Distance");
		menu.add(0, RATING_OPTION, 0, "Search by Rating (Yelp Only)");
		menu.add(0, BEST_MATCH_OPTION, 0, "Search by Best Match (Yelp Only)");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case BEST_MATCH_OPTION:
			Log.e(TAG, "Best Matched Pressed!");
			yelpSortChoice = SEARCH_BY_BEST_MATCH;
			break;
		case DISTANCE_OPTION:
			Log.e(TAG, "Distance Pressed!");
			yelpSortChoice = SEARCH_BY_DISTANCE;
			break;
		case RATING_OPTION:
			Log.e(TAG, "Rating Pressed!");
			yelpSortChoice = SEARCH_BY_RATING;
			break;
		}
		beginQuery(this.provider);
		return super.onOptionsItemSelected(item);
	}

	private void currentLocation() 
	{
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		String locProvider = locationManager.getBestProvider(new Criteria(), false);

		Location location = locationManager.getLastKnownLocation(locProvider);

		if (location == null) 
		{
			//TODO: app hangs here
			locationManager.requestLocationUpdates(locProvider, 0, 0, listener);

			locateDialog = new ProgressDialog(this);
			locateDialog.setCancelable(true);
			locateDialog.setMessage("Locating User Position...");
			locateDialog.isIndeterminate();
			/*
			locateDialog.setOnCancelListener(new OnCancelListener() {

	            @Override
	            public void onCancel(DialogInterface arg0) {
	                AlertDialog.Builder builder = new AlertDialog.Builder(locateDialog.getContext());
	                builder.setMessage( "Are you sure you want to cancel?")
	                       .setCancelable(false)
	                       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                           public void onClick(DialogInterface diag, int id) {
	                               diag.dismiss();
	                               locateDialog.dismiss();

	                           }
	                       })
	                       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	                           public void onClick(DialogInterface diag, int id) {
	                                diag.cancel();
	                           }
	                       });
	                AlertDialog alert = builder.create();
	                alert.show();               
	            }

	        });
	        */
			locateDialog.show();
		} 
		else 
		{
			loc = location;
			beginQuery(this.provider);
			Log.e(TAG, "location : " + location);
		}

	}

	/**
	 * Location listener to receive updates from LocationManager
	 */
	private LocationListener listener = new LocationListener() 
	{
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) 
		{

		}

		@Override
		public void onProviderEnabled(String provider) 
		{

		}

		@Override
		public void onProviderDisabled(String provider) 
		{

		}

		@Override
		public void onLocationChanged(Location location) 
		{
			Log.e(TAG, "location update : " + location);
			loc = location;
			locationManager.removeUpdates(listener);
			locateDialog.cancel();
			
			Context context = getApplicationContext();
			
			Toast.makeText(context, "Location found! Please refresh the search.", Toast.LENGTH_LONG).show();
			
			//context.beginQuery();
		}
	};

	private void showGPSDisabledAlertToUser(boolean type){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		
		String buttonMsg;
		
		if(type) //GPS is disabled but location network is running
		{
			alertDialogBuilder.setMessage("GPS is currently disabled. You can get better results by enabling GPS. Would you like to enable it?");
			buttonMsg = "Enable GPS";
		}
		else
		{
			alertDialogBuilder.setMessage("Location Services is currently disabled. This app needs Location Services to function properly. Please enable Location Services.");
			buttonMsg = "Enable Location Services";
		}
		
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton(buttonMsg,
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				Intent callGPSSettingIntent = new Intent(
						android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(callGPSSettingIntent);
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
