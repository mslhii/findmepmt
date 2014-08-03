package com.kritikalerror.findmepmt;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
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

	private final String TAG = getClass().getSimpleName();
	private GoogleMap mMap;
	private String[] places;
	private LocationManager locationManager;
	private Location loc;
	
	private String yelpResults;
	
	private Result yelpObject;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initMap();

		// Generate user list of radii to search
		//TODO: deprecated?
		places = getResources().getStringArray(R.array.radius);
		currentLocation();
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(ArrayAdapter.createFromResource(
				this, R.array.radius, android.R.layout.simple_list_item_1),
				new ActionBar.OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition,
					long itemId) {
				Log.e(TAG,
						places[itemPosition].toLowerCase().replace("-",
								"_"));
				if (loc != null) {
					mMap.clear();
					new GetPlaces(MainActivity.this,
							places[itemPosition].toLowerCase().replace(
									"-", "_").replace(" ", "_")).execute();
					new GetYelp().execute();
				}
				return true;
			}
		});
	}

	private class GetYelp extends AsyncTask<Void, Void, String>
	{
		protected String doInBackground(Void... params) {
			Yelp yelp = Yelp.getYelp(MainActivity.this);
			String businesses = yelp.search("boba milk bubble tea tapioca", loc.getLatitude(), loc.getLongitude());
			try {
				return processJson(businesses);
			} catch (JSONException e) {
				Log.e("errorage", e.getMessage());
				return businesses;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			yelpResults = result;
			Log.w("tag", yelpResults);
			setProgressBarIndeterminateVisibility(false);
		}

		String processJson(String jsonStuff) throws JSONException {
			JSONObject json = new JSONObject(jsonStuff);
			
			// Debug only!!
			Log.w("doge", json.toString());
			
			JSONArray businesses = json.getJSONArray("businesses");
			ArrayList<String> businessNames = new ArrayList<String>(businesses.length());
			ArrayList<Result> businessList = new ArrayList<Result>(businesses.length());
			for (int i = 0; i < businesses.length(); i++) 
			{
				businessList.add(Result.jsonToClass(businesses.getJSONObject(i)));
				
				//TODO: old please delete!!
				/*
				JSONObject business = businesses.getJSONObject(i);
				
				JSONObject locations = business.getJSONObject("location");
				//businessNames.add(business.getString("name"));
				String sum = business.getString("name") + "/" 
						+ business.getString("rating") + "/"
						+ locations.getJSONArray("address").getString(0) + "/"
						+ locations.getString("city") + "/"
						+ locations.getString("state_code") + "/";
				
				if(locations.has("coordinate"))
				{
					String latitude = locations.getJSONObject("coordinate").getString("latitude");
					String longitude = locations.getJSONObject("coordinate").getString("longitude");
					sum = sum + "/" + latitude + "/" + longitude;
				}
				businessNames.add(sum);
				*/
			}
			//http://maps.google.com/maps/api/geocode/json?address=2086+University+Ave,+Berkeley,+CA
			return TextUtils.join("\n", businessNames);
		}
	}

	private class GetPlaces extends AsyncTask<Void, Void, ArrayList<Place>> 
	{
		private ProgressDialog dialog;
		private Context context;
		private String places;

		public GetPlaces(Context context, String places) 
		{
			this.context = context;
			this.places = places;
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
				Toast.makeText(getApplicationContext(), "Cannot find any PMT place near you!",
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
			PlacesService service = new PlacesService(
					"AIzaSyD-RjNYm-VCo1rtTwHqjIi8XQz29UAra4M");
			ArrayList<Place> findPlaces; 
			findPlaces = service.findPlaces(loc.getLatitude(), // 28.632808
					loc.getLongitude(), places); // 77.218276

			for (int i = 0; i < findPlaces.size(); i++) 
			{		
				Place placeDetail = findPlaces.get(i);
				Log.e(TAG, "places : " + placeDetail.getName() + ", vicinity: " + placeDetail.getVicinity());
			}
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
			new GetPlaces(MainActivity.this, places[0].toLowerCase().replace(
					"-", "_")).execute();
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
