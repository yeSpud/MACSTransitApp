package fnsb.macstransit;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

public class MapsActivity extends androidx.fragment.app.FragmentActivity implements com.google.android.gms.maps.OnMapReadyCallback {

	/**
	 * Create an array of all the routes that are used by the transit system. For now leave it uninitialized,
	 * as it will be dynamically generated in the onCreate method.
	 */
	public Route[] routes;


	/**
	 * Create an instance of the route match object that will be used for this app. This too is loaded dynamically.
	 */
	public RouteMatch routeMatch;

	/**
	 * Create an array list to determine which routes have been selected from the menu to track.
	 */
	public ArrayList<Route> selectedRoutes = new ArrayList<>();

	/**
	 * Create an array of all the buses that will end up being tracked.
	 */
	public ArrayList<Bus> buses = new ArrayList<>();

	/**
	 * Create the map object.
	 */
	private GoogleMap map;

	/**
	 * Create an instance of the thread object that will be used to pull data from the routematch server.
	 */
	private UpdateThread thread = new UpdateThread(this, 4000);

	private boolean menuCreated;

	/**
	 * Prepare the Screen's standard options menu to be displayed.
	 * This is called right before the menu is shown, every time it is shown.
	 * You can use this method to efficiently enable/disable items or otherwise dynamically modify the contents.
	 * <p>
	 * The default implementation updates the system menu items based on the activity's state.
	 * Deriving classes should always call through to the base class implementation.
	 *
	 * @param menu The options menu as last shown or first initialized by onCreateOptionsMenu().
	 * @return You must return true for the menu to be displayed; if you return false it will not be shown.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Documentation
		if (!menuCreated) {
			for (Route route : this.routes) {
				menu.add(R.id.routes, Menu.NONE, Menu.NONE, route.routeName).setCheckable(true);
			}
			menuCreated = true;
			Log.d("Menu", "onPrepare");
		}
		return super.onPrepareOptionsMenu(menu);
	}


	/**
	 * TODO Documentation
	 *
	 * @param menu
	 * @return
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d("Menu", "onCreate");
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		this.menuCreated = false;
		return true;
	}

	/**
	 * This hook is called whenever an item in your options menu is selected.
	 * The default implementation simply returns false to have the normal processing happen (calling the item's Runnable or sending a message to its Handler as appropriate).
	 * You can use this method for any items for which you would like to do processing without those other facilities.
	 * <p>
	 * Derived classes should call through to the base class for it to perform the default menu handling.
	 *
	 * @param item The menu item that was selected. This value must never be null.
	 * @return Return false to allow normal menu processing to proceed, true to consume it here.
	 */
	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		// TODO Documentation
		Log.d("Menu", "onOptionsItemSelected");
		if (item.getGroupId() == R.id.other) {
			if (item.getItemId() == R.id.nightmode) {
				Log.d("Menu", "Toggle nightmode has been selected!");
				// TODO
				item.setChecked(!item.isChecked());
			} else {
				Log.w("Menu", "Unaccounted for item in the other group was checked!");
			}
		} else if (item.getGroupId() == R.id.routes) {
			Log.d("Menu", "A route has been toggled!");
			// TODO
			item.setChecked(!item.isChecked());
		}
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
		((com.google.android.gms.maps.SupportMapFragment) java.util.Objects.requireNonNull(this.getSupportFragmentManager()
				.findFragmentById(R.id.map))).getMapAsync(this);

		// Load the routes dynamically
		try {
			this.routes = Route.generateRoutes("https://fnsb.routematch.com/feed/");
			this.routeMatch = new RouteMatch("fnsb", "https://fnsb.routematch.com/feed/", this.routes);
		} catch (InterruptedException e) {
			this.finish();
			this.startActivity(getIntent());
		}

		// Since the menu doesn't work, just track all the routes by adding all of them to the selected routes array list.
		// TODO This should be removed once the menu is fixed
		this.selectedRoutes.addAll(java.util.Arrays.asList(this.routes));
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

	/**
	 * Called when the activity loses foreground state,
	 * is no longer focusable or before transition to stopped/hidden or destroyed state.
	 * The activity is still visible to user, so it's recommended to keep it visually active and continue updating the UI.
	 * Implementations of this method must be very quick because the next activity will not be resumed until this method returns.
	 * Followed by either onResume() if the activity returns back to the front, or onStop() if it becomes invisible to the user.
	 * <p>
	 * Here is where I want to stop the update cycle that queries the routematch server.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		this.thread.run = false;
	}

	/**
	 * Perform any final cleanup before an activity is destroyed.
	 * This can happen either because the activity is finishing (someone called finish() on it),
	 * or because the system is temporarily destroying this instance of the activity to save space.
	 * You can distinguish between these two scenarios with the isFinishing() method.
	 * <p>
	 * Note: <i>do not count on this method being called as a place for saving data! For example,
	 * if an activity is editing data in a content provider,
	 * those edits should be committed in either onPause() or onSaveInstanceState(Bundle), not here.
	 * This method is usually implemented to free resources like threads that are associated with an activity,
	 * so that a destroyed activity does not leave such things around while the rest of its application is still running.
	 * There are situations where the system will simply kill the activity's hosting process without calling this method (or any others) in it,
	 * so it should not be used to do things that are intended to remain around after the process goes away.</i>
	 * <p>
	 * Here is where I want to stop the update cycle that queries the routematch server.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		// Setup the map object at this point as it is finally initialized and ready.
		this.map = googleMap;

		// Move the camera to the 'home' position
		LatLng home = new LatLng(64.8391975, -147.7684709);
		map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(home, 11.0f));
	}

	/**
	 * Updates the position (and color) of the markers on the map.
	 */
	public void updateBusMarkers() {

		// Make a copy of the buses that are currently being tracked, to mitigate issue #7 (https://github.com/yeSpud/MACSTransitApp/issues/7)
		Bus[] trackedBuses = this.buses.toArray(new Bus[0]);


		// Start by iterating through all the buses that are currently being tracked.
		for (Bus bus : trackedBuses) {

			// The following should be done on the UI thread
			this.runOnUiThread(() -> {

				// Get the old marker for the bus
				com.google.android.gms.maps.model.Marker marker = bus.getMarker();

				// Get the current LatLng of the bus
				LatLng latLng = new LatLng(bus.latitude, bus.longitude);

				// Check if that bus has a marker to begin with.
				// If the bus doesn't have a marker create a new one,
				// and overwrite the marker variable with the newly created marker
				if (marker == null) {
					marker = this.map.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(latLng));
				} else {
					// Just update the title
					marker.setPosition(latLng);
				}

				// Now update the title

				marker.setTitle(bus.route.routeName);

				// If the route has a color, set its icon to that color
				if (bus.route.color != 0) {
					marker.setIcon(this.getMarkerIcon(bus.route.color));
				}

				// Make sure that the marker is visible
				marker.setVisible(true);

				// Finally, (re)assign the marker to the bus
				bus.setMarker(marker);
			});
		}
	}

	/**
	 * Gets the color of the marker icon based off of the color value given.
	 * The reason why there needs to be a function for this is because there are only 10 colors that a marker icon can be.
	 *
	 * @param color The desired color value as an int.
	 * @return The BitmapDescriptor used for defining the color of a markers's icon.
	 */
	private com.google.android.gms.maps.model.BitmapDescriptor getMarkerIcon(int color) {
		float[] hsv = new float[3];
		android.graphics.Color.colorToHSV(color, hsv);
		return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0]);
	}
}
