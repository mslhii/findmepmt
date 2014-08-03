package com.kritikalerror.findmepmt;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ks.googleplaceapidemo.R;

/**
 *  This class is used to search places using Places API using keywords like police,hospital etc.
 * 
 * @author Michael Hii
 * @Date   6/4/2014
 *
 */
public class MainActivity extends Activity {
	private final String PARAMS = "boba milk bubble tea tapioca";
	private int yelpSortChoice = 1;

	private final String TAG = getClass().getSimpleName();
	private GoogleMap mMap;
	private LocationManager locationManager;
	private Location loc;
	private String provider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Intent intent = getIntent();
		provider = intent.getExtras().getString("provider");

		initMap();

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
					if(providerList.get(itemPosition).equals("Google"))
					{
						new GetPlaces(MainActivity.this).execute();
					}
					else if(providerList.get(itemPosition).equals("Yelp"))
					{
						new GetYelp().execute();
					}
					else
					{
						Toast.makeText(getApplicationContext(), "Invalid provider selected!",
								Toast.LENGTH_LONG).show();
					}
				}
				return true;
			}
		});
	}

	private class GetYelp extends AsyncTask<Void, Void, ArrayList<Result>>
	{
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

		// Display results on map
		@Override
		protected void onPostExecute(ArrayList<Result> result) {
			//super.onPostExecute(result);

			CameraPosition cameraPosition;
			
			// Add marker of current position
			mMap.addMarker(new MarkerOptions()
			.title("Current Position")
			.position(
					new LatLng(loc.getLatitude(), loc.getLongitude()))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.man))
							.snippet("I am here!"));
			
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
						String snippetString = result.get(i).getRating() + " stars, " + 
								result.get(i).getAddress();
						
						if(result.get(i).getPhone() != null)
						{
							snippetString = snippetString + ", " + result.get(i).getPhone();
						}
						
						mMap.addMarker(new MarkerOptions()
						.title(result.get(i).getName())
						.position(
								new LatLng(result.get(i).getLatitude(), result.get(i).getLongitude()))
										.icon(BitmapDescriptorFactory
												.fromResource(R.drawable.pin))
												.snippet(snippetString));
					}
				}
				
				cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(result.get(0).getLatitude(), result
						.get(0).getLongitude())) // Sets camera to first result
						.zoom(14) // Sets the zoom
						.tilt(30) // Sets the tilt of the camera to 30 degrees
						.build(); // Creates a CameraPosition from the builder
				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
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

			// Add marker of current position
			mMap.addMarker(new MarkerOptions()
			.title("Current Position")
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
					mMap.addMarker(new MarkerOptions()
					.title(result.get(i).getName())
					.position(
							new LatLng(result.get(i).getLatitude(), result
									.get(i).getLongitude()))
									.icon(BitmapDescriptorFactory
											.fromResource(R.drawable.pin))
											.snippet(result.get(i).getVicinity()));
				}
				cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(result.get(0).getLatitude(), result
						.get(0).getLongitude())) // Sets the center of the map to
						// Mountain View
						.zoom(14) // Sets the zoom
						.tilt(30) // Sets the tilt of the camera to 30 degrees
						.build(); // Creates a CameraPosition from the builder
				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
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
		return true;
	}

	private void currentLocation() 
	{
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		String provider = locationManager
				.getBestProvider(new Criteria(), false);

		Location location = locationManager.getLastKnownLocation(provider);

		if (location == null) 
		{
			locationManager.requestLocationUpdates(provider, 0, 0, listener);
		} 
		else 
		{
			loc = location;
			if(provider.equals("Google"))
			{
				new GetPlaces(MainActivity.this).execute();
			}
			else if(provider.equals("Yelp"))
			{
				new GetYelp().execute();
			}
			Log.e(TAG, "location : " + location);
		}

	}

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
		}
	};

}
