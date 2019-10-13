package fnsb.macstransit;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

	/**
	 * A boolean to check where or not to run the update thread
	 */
	private boolean run = false;

	/**
	 * Create our map object
	 */
	private GoogleMap map;

	/**
	 * Create a string array of all the routes
	 */
	private Route[] routes = new Route[]{new Route("Yellow", Color.rgb(254, 255, 2)),
			new Route("Blue", Color.rgb(68, 114, 196)),
			new Route("Red", Color.rgb(245, 21, 19)),
			new Route("Brown", Color.rgb(127, 96, 0)),
			new Route("Gold", Color.rgb(255, 221, 0)),
			new Route("Green", Color.rgb(112, 173, 71)),
			new Route("Purple", Color.rgb(139, 3, 255))};

	/**
	 * Create an instance of the route match object
	 */
	private RouteMatch routeMatch = new RouteMatch("fnsb", "https://fnsb.routematch.com/feed/vehicle/byRoutes/", this.routes);

	//private ArrayList<Route> selectedRoutes = new ArrayList<>();

	private ArrayList<Bus> buses = new ArrayList<>();

	@Deprecated
	public static double[] getLatLong(JSONObject jsonObject) {
		try {
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
	public boolean onPrepareOptionsMenu(Menu menu) { // FIXME
		menu.clear();

		for (Route route : this.routes) {
			MenuItem item = menu.add(0, Menu.NONE, Menu.NONE, route.routeName);

		}
		menu.setGroupCheckable(0, true, false);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) { // FIXME
		super.onOptionsItemSelected(item);
		item.setChecked(!item.isChecked());
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		((SupportMapFragment) Objects.requireNonNull(this.getSupportFragmentManager()
				.findFragmentById(R.id.map))).getMapAsync(this);
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
		this.map = googleMap;

		// Move the camera to the 'home' position
		LatLng home = new LatLng(64.8391975, -147.7684709);
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

				try {
					for (Route route : this.routes) {
						JSONArray array = this.routeMatch.getRoute(route.routeName).getJSONArray("data");

						Log.i("Full data", array.toString());
						for (int i = 0; i < array.length(); i++) {
							JSONObject object = array.getJSONObject(i);

							// Create a bus object form the data
							Bus bus = new Bus(object.getString("vehicleId"), route);
							try {
								bus.heading = Heading.valueOf(object.getString("headingName"));
							} catch (IllegalArgumentException e) {
								bus.heading = Heading.NORTH;
							}
							bus.latitude = object.getDouble("latitude");
							bus.longitude = object.getDouble("longitude");
							bus.color = route.getColor();


							// Search the current array of buses for that bus ID
							// If it exists, update its lat and long, and heading.
							// If it doesn't exist, add it.
							boolean found = false;
							for (Bus busCheck : this.buses) {
								if (bus.busID.equals(busCheck.busID)) {
									found = true;
									busCheck.heading = bus.heading;
									busCheck.route = bus.route;
									busCheck.latitude = bus.latitude;
									busCheck.longitude = bus.longitude;
									busCheck.color = bus.color;
									break;
								}
							}

							if (!found) {
								this.buses.add(bus);
							}

							// Update the bus markers
							runOnUiThread(this::updateBusMarkers);
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

	private void updateBusMarkers() {
		for (Bus bus : this.buses) {

			// Get the old marker
			Marker marker = bus.getMarker();

			// Get the current LatLng of the bus
			LatLng latLng = new LatLng(bus.latitude, bus.longitude);

			if (marker != null) {
				// Update the markers position to the new LatLong
				marker.setPosition(latLng);
				marker.setTitle(bus.route.routeName);
				if (bus.route.getColor() != 0) {
					marker.setIcon(this.getMarkerIcon(bus.route.getColor()));
				}
				marker.setVisible(true);
				bus.setMarker(marker);
			} else {
				Marker newMarker = this.map.addMarker(new MarkerOptions().position(latLng));
				newMarker.setTitle(bus.route.routeName);
				if (bus.route.getColor() != 0) {
					newMarker.setIcon(this.getMarkerIcon(bus.route.getColor()));
				}
				newMarker.setVisible(true);
				bus.setMarker(newMarker);
			}
		}
	}


	private BitmapDescriptor getMarkerIcon(int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		return BitmapDescriptorFactory.defaultMarker(hsv[0]);
	}
}
