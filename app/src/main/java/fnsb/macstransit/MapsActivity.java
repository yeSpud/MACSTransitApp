package fnsb.macstransit;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

	/**
	 * A boolean to check where or not to run the update thread
	 */
	public boolean run = false;

	/**
	 * Create an array to store all the markers for the buses
	 */
	ArrayList<Marker> markers = new ArrayList<>();

	/**
	 * Create our map object
	 */
	private GoogleMap map;

	/**
	 * Create a string array of all the routes
	 */
	private String[] routes = new String[]{"Blue", "Brown", "Gold", "Green", "Purple", "Red", "Yellow"};

	/**
	 * Create an instance of the route match object
	 */
	private RouteMatch routeMatch = new RouteMatch("fnsb", "https://fnsb.routematch.com/feed/vehicle/byRoutes/", this.routes);


	public static double[] getLatLong(JSONObject jsonObject) {
		try {
			//System.out.println(jsonObject.toString());
			Log.d("getLatLong", "Full object: " + jsonObject.toString());
			String lat = jsonObject.getString("latitude");
			Log.d("Latitude", lat);
			String lng = jsonObject.getString("longitude");
			Log.d("Longitude", lng);

			return new double[]{Double.parseDouble(lat), Double.parseDouble(lng)};
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Gets called every time the user presses the menu button.
	 * Use if your menu is dynamic.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();

		for (String string : routes) {
			menu.add(0, Menu.NONE, Menu.NONE, string);

		}
		menu.setGroupCheckable(0, true, false);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		item.setChecked(!item.isChecked());

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		assert mapFragment != null;
		mapFragment.getMapAsync(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Here is where I want to start (or restart as the case may be) the loop to check for position updates
		// This is mostly for data saving measures
		this.run = true;
		this.thread().start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Here is where I want to stop the update cycle that queries the routematch server
		this.run = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// TODO
		// Here is where I want to stop the update cycle that queries the routematch server
		this.run = false;
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;

		// Add a marker in Sydney and move the camera
		LatLng home = new LatLng(64.8391975, -147.7684709);
		//map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 11.0f));
	}


	/**
	 * This is the update thread. It updates the positions of the routes ever 3 seconds
	 *
	 * @return The thread object.
	 */
	private Thread thread() {
		Thread t = new Thread(() -> {

			Log.w("Update thread", "Starting up...");

			while (this.run && !Thread.interrupted()) {

				for (Marker marker : this.markers) {
					runOnUiThread(marker::remove);
				}

				try {
					for (String routes : this.routes) {
						JSONArray array = this.routeMatch.getRoute(routes).getJSONArray("data");

						//Log.i("object", object.toString());
						for (int i = 0; i < array.length(); i++) {
							JSONObject object = array.getJSONObject(i);
							final double[] latlong = getLatLong(object);
							if (latlong != null) {
								runOnUiThread(() -> {
									Marker marker = this.map.addMarker(new MarkerOptions().position(new LatLng(latlong[0], latlong[1])).title(routes));
									//marker.setFlat(true);
									//marker.setIcon();
									marker.setVisible(true);
									this.markers.add(marker);
								});
							}

						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Thread.yield();
				Log.d("Update thread", "Looping...");

			}

			Log.w("Update thread", "Shutting down...");
		});

		return t;

	}
}
