package fnsb.macstransit;

import android.graphics.Color;
import android.view.Menu;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MapsActivity extends androidx.fragment.app.FragmentActivity implements com.google.android.gms.maps.OnMapReadyCallback {

	/**
	 * Create an array of all the routes that are used by the transit system.
	 * <p>
	 * For this case, not only am I passing the required names of the routes, but also their respective color :D
	 */
	public Route[] routes = new Route[]{new Route("Yellow", Color.rgb(254, 255, 2)),
			new Route("Blue", Color.rgb(68, 114, 196)),
			new Route("Red", Color.rgb(245, 21, 19)),
			new Route("Brown", Color.rgb(127, 96, 0)),
			new Route("Gold", Color.rgb(255, 221, 0)),
			new Route("Green", Color.rgb(112, 173, 71)),
			new Route("Purple", Color.rgb(139, 3, 255))};

	/**
	 * Create an instance of the route match object that will be used for this app.
	 * Be sure to pass it the previously created routes as well.
	 */
	public RouteMatch routeMatch = new RouteMatch("fnsb", "https://fnsb.routematch.com/feed/vehicle/byRoutes/", this.routes);

	/**
	 * Create an array list to determine which routes have been selected from the menu to track.
	 */
	public ArrayList<Route> selectedRoutes = new ArrayList<>();

	/**
	 * TODO Documentation
	 */
	public ArrayList<Bus> buses = new ArrayList<>();

	/**
	 * Create the map object.
	 */
	private GoogleMap map;

	/**
	 * TODO Documentation
	 */
	private UpdateThread thread = new UpdateThread(this);

	/**
	 * Gets called every time the user presses the menu button.
	 * Use if your menu is dynamic.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		// FIXME
		//for (int index = 1; index <= this.routes.length; index++) {
		//menu.add(0, index, Menu.NONE, this.routes[index-1].routeName);
		//}
		for (Route route : this.routes) {
			menu.add(1, Menu.NONE, Menu.NONE, route.routeName).setCheckable(true);
		}

		//menu.setGroupCheckable(1, true, false);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		// FIXME
		return super.onOptionsItemSelected(item);
	}


	/**
	 * This is where the activity is initialized. Most importantly,
	 * here is where setContentView(int) is usually called with a layout resource defining the UI,
	 * and using findViewById(int) to retrieve the widgets in that UI that need to interacted with programmatically.
	 * <p>
	 * More importantly, this is where I want to obtain the SupportMapFragment,
	 * and get notified when the map has finished initializing and is ready to be used.
	 *
	 * @param savedInstanceState The previous state of the activity,
	 *                           in the event that there was an issue and the activity had to be destroyed.
	 */
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		((SupportMapFragment) Objects.requireNonNull(this.getSupportFragmentManager()
				.findFragmentById(R.id.map))).getMapAsync(this);

		// Since the menu doesn't work, just track all the routes by adding all of them to the selected routes array list.
		// TODO This should be removed once the menu is fixed
		this.selectedRoutes.addAll(Arrays.asList(this.routes));
	}

	/**
	 * Called when the activity will start interacting with the user.
	 * At this point your activity is at the top of its activity stack, with user input going to it.
	 * <p>
	 * Here is where I want to start (or restart as the case may be) the loop to check for position updates.
	 * This is mostly for data saving measures, but also for performance improvements.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		this.thread.run = true;
		this.thread.thread().start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Here is where I want to stop the update cycle that queries the routematch server
		this.thread.run = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Here is where I want to stop the update cycle that queries the routematch server
		this.thread.run = false;
	}

	/**
	 * Manipulates the map once available. This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia. If Google Play services is not installed on the device,
	 * the user will be prompted to install it inside the SupportMapFragment.
	 * This method will only be triggered once the user has installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		this.map = googleMap;

		// Move the camera to the 'home' position
		LatLng home = new LatLng(64.8391975, -147.7684709);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 11.0f));
	}

	/**
	 * TODO Documentation
	 */
	public void updateBusMarkers() {
		for (Bus bus : this.buses) {
			this.runOnUiThread(() -> {

				// Get the old marker
				Marker marker = bus.getMarker();

				// Get the current LatLng of the bus
				LatLng latLng = new LatLng(bus.latitude, bus.longitude);

				if (marker != null) {
					// Update the markers position to the new LatLong
					marker.setPosition(latLng);
					marker.setTitle(bus.route.routeName);
					if (bus.route.color != 0) {
						marker.setIcon(this.getMarkerIcon(bus.route.color));
					}
					marker.setVisible(true);
					bus.setMarker(marker);
				} else {
					Marker newMarker = this.map.addMarker(new MarkerOptions().position(latLng));
					newMarker.setTitle(bus.route.routeName);
					if (bus.route.color != 0) {
						newMarker.setIcon(this.getMarkerIcon(bus.route.color));
					}
					newMarker.setVisible(true);
					bus.setMarker(newMarker);
				}
			});
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @param color
	 * @return
	 */
	private BitmapDescriptor getMarkerIcon(int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		return BitmapDescriptorFactory.defaultMarker(hsv[0]);
	}
}
