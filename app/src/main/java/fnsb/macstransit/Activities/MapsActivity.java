package fnsb.macstransit.Activities;

import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.ConcurrentModificationException;

import fnsb.macstransit.Activities.activitylisteners.AdjustZoom;
import fnsb.macstransit.R;
import fnsb.macstransit.routematch.Bus;
import fnsb.macstransit.routematch.Route;
import fnsb.macstransit.routematch.SharedStop;
import fnsb.macstransit.routematch.Stop;
import fnsb.macstransit.Threads.UpdateThread;
import fnsb.macstransit.settings.CurrentSettings;
import fnsb.macstransit.settings.V2;

public class MapsActivity extends androidx.fragment.app.FragmentActivity implements
		com.google.android.gms.maps.OnMapReadyCallback {

	/**
	 * Create an array to store all the routes that we will track.
	 * This is not to say that all routes in this array are enabled - they can also be disabled (hidden).
	 * <p>
	 * This array is initialized in SplashActivity.
	 * As such it has the possibility of being null (caused by Sunday bypass).
	 */
	@Nullable
	public static Route[] allRoutes; // TODO Check for concurrent exceptions

	/**
	 * Create an array of all the buses that will be used
	 * (either shown or hidden) on the map at any given time.
	 * For now just initialize this array to 0.
	 */
	public static Bus[] buses = new Bus[0];

	/**
	 * Create an instance of the RouteMatch object that will be used for this app.
	 */
	public static fnsb.macstransit.routematch.RouteMatch routeMatch;

	/**
	 * Create the map object. This will be null until the map is ready to be used.
	 * Deprecated because this leaks memory in the static form. Use as dependency injection.
	 */
	@Nullable
	private GoogleMap map;

	/**
	 * Bool used to check if we have selected favorited routes from all routes.
	 * If this is set to true then we do not need to select favorite routes again as it should only be selected once.
	 */
	public static boolean selectedFavorites = false;

	/**
	 * Update thread used for fetching bus locations.
	 * This may be null until initialized in onCreate.
	 */
	@Nullable
	private static UpdateThread updateThread;

	/**
	 * Handler used to update bus markers on the UI Thread from the Update Thread.
	 */
	private final Handler mainThreadHandler = new Handler(android.os.Looper.myLooper());

	/**
	 * Create a variable to store our fare popup window instance.
	 * This should be initialized in the onCreate method for this activity.
	 */
	private FarePopupWindow farePopupWindow;

	/**
	 * Documentation
	 */
	private final CurrentSettings currentSettings = CurrentSettings.INSTANCE;

	/**
	 * Checks the selected item and enables or disables the specific route depending in its checked status.
	 *
	 * @param item The menu item that should belong to a route. This value cannot be null.
	 * @throws Route.RouteException Thrown if the route to be toggled is not found within allRoutes.
	 */
	private void onRouteItemToggled(@NonNull MenuItem item) throws Route.RouteException {
		Log.v("onRouteItemSelected", "onRouteItemSelected bas been called!");

		// Make sure there are routes to iterate though by checking to see if allRoutes isn't null.
		if (MapsActivity.allRoutes == null) {

			// Since allRoutes is null return early.
			return;
		}

		// Create a boolean to store the resulting value of the menu item.
		boolean enabled = !item.isChecked();

		// Determine which route from all the routes was just selected.
		Route selectedRoute = null;
		for (Route route : MapsActivity.allRoutes) {
			if (route.getRouteName().equals(item.getTitle().toString())) {
				selectedRoute = route;
				break;
			}
		}

		// Make sure the selected route was found.
		if (selectedRoute == null) {
			throw new Route.RouteException("Unable to determine selected route!");
		}

		// Updated the selected route's boolean.
		selectedRoute.setEnabled(enabled);

		// Try to (re)draw the buses onto the map.
		// Because we are iterating a static variable that is modified on a different thread
		// there is a possibility of a concurrent modification.
		try {
			this.drawBuses();
		} catch (ConcurrentModificationException e) {
			Log.e("onRouteItemToggled",
					"Unable to redraw all buses due to concurrent modification", e);
		}

		// (Re) draw the stops onto the map.
		this.drawStops();

		// (Re) draw the routes onto the map (if enabled).
		if (((V2) this.currentSettings.getSettingsImplementation()).getPolylines()) {
			this.drawRoutes();
		}

		// Set the menu item's checked value to that of the enabled value
		item.setChecked(enabled);
	}

	/**
	 * Draws the stops and shared stops onto the map, and adjusts the stop sizes based on the zoom level.
	 */
	private void drawStops() {

		// First, make sure that allRoutes isn't null. If it is, return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("drawStops", "allRoutes is null!");
			return;
		}

		// Iterate though all the routes as we know at this point that they are not null.
		for (Route route : MapsActivity.allRoutes) {

			// Iterate though the stops in the route before getting to the shared stops.
			for (Stop stop : route.getStops()) {

				// Check that the stop isn't null
				// as calling any functions of a null object would result in a crash.
				if (stop != null) {

					if (route.getEnabled()) {

						// Show the stop on the map.
						stop.showStop(this.map);
					} else {

						// Hide the stop from the map.
						// Be sure its only hidden and not actually destroying the object.
						stop.hideStop();
					}
				} else {

					// Log that one of the stops was null. If this is called often enough then it
					// may be worthwhile to check the validateStops function in the SplashActivity.
					Log.w("drawStops", "Stop is null!");
				}
			}

			// Check that there are shared stops to hide in the route.
			SharedStop[] sharedStops = route.getSharedStops();

			// Iterate though the shared stops in the route.
			for (SharedStop sharedStop : sharedStops) {

				if (route.getEnabled()) {

					// Show the shared stops.
					sharedStop.showSharedStop(this.map);
				} else {

					// Hide the shared stops on the map.
					// Note that the stops should be hidden - not destroyed.
					sharedStop.hideStop();
				}
			}
		}

		// Adjust the circle sizes of the stops on the map given the current zoom.
		AdjustZoom.resizeStops(this.map);
	}

	/**
	 * Draws the route's polylines to the map.
	 * While all polylines are drawn they will only be shown if the route that they belong to is enabled.
	 */
	private void drawRoutes() {

		// Make sure there are routes to iterate through (check if the routes are not null).
		if (MapsActivity.allRoutes != null) {

			// Start by iterating through all the routes.
			for (Route route : MapsActivity.allRoutes) {

				try {

					// Check if the route has a polyline to set visible.
					if (route.getPolyline() == null) {

						// Create a new polyline for the route since it didn't have one before.
						route.createPolyline(this.map);
					}

					// Set the polyline's visibility to whether the route is enabled or not.
					route.getPolyline().setVisible(route.getEnabled());
				} catch (NullPointerException e) {

					// If the polyline was still null after being created, log it as a warning.
					Log.w("drawRoutes", String.format("Polyline for route %s was not created successfully!",
							route.getRouteName()));
				}
			}
		}
	}

	/**
	 * (Re)draws the buses on the map.
	 * While all buses are drawn they will only be shown if the route that they belong to is enabled.
	 *
	 * @throws ConcurrentModificationException Concurrent exception may be thrown
	 *                                         as it iterates through the bus list,
	 *                                         which may be modified at the time of iteration.
	 */
	private void drawBuses() throws ConcurrentModificationException {

		// Start by iterating though all the buses on the map.
		for (Bus bus : MapsActivity.buses) {

			// Comments
			Route route = bus.getRoute();

			if (bus.getMarker() != null) {

				// Set the bus marker visibility based on if the bus's route is enabled or not.
				bus.getMarker().setVisible(route.getEnabled());
			} else {

				if (route.getEnabled()) {

					if (this.map != null) {

						// Try creating a new marker for the bus (if its enabled).
						bus.addMarker(this.map, new LatLng(bus.getLatitude(), bus.getLongitude()),
								bus.getColor());

						bus.getMarker().setVisible(true);

					} else {

						// Since the map is null simply log it as a  warning.
						Log.w("drawBuses", "Map is not ready!");
					}

				} else {

					// If the marker was null simply log it as a warning.
					Log.w("drawBuses", String.format("Bus doesn't have a marker for route %s!",
							route.getRouteName()));
				}
			}
		}
	}

	/**
	 * Manages and syncs the state of the Update Thread.
	 */
	private static void manageUpdateThread() {

		// Return early if the update thread is null.
		if (MapsActivity.updateThread == null) {
			return;
		}

		// If the thread isn't locked, then there is no need to continue.
		if (!MapsActivity.updateThread.getIsLockedForever()) {
			return;
		}

		// Be sure to synchronize with the thread lock.
		Log.v("manageUpdateThread", "Synchronizing with lock");
		synchronized (MapsActivity.updateThread.LOCK) {

			// Depending on the state of the update thread, either resume, or start the runner.
			java.lang.Thread.State state = MapsActivity.updateThread.runner.getState();
			Log.d("manageUpdateThread", "State of thread: " + state.name());
			switch (state) {
				case NEW:
					Log.d("manageUpdateThread", "Starting update thread");

					// Start the thread.
					MapsActivity.updateThread.runner.start();
					break;
				case BLOCKED:
				case TIMED_WAITING:
				case WAITING:

					// Notify the thread to resume
					MapsActivity.updateThread.LOCK.notifyAll();
					break;
				case TERMINATED:

					Log.w("manageUpdateThread", "Update thread has been terminated.");
					break;
				default:
					Log.w("manageUpdateThread", "Thread state unaccounted for");
					break;
			}
		}
	}

	@Override
	protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
		Log.v("onCreate", "onCreate has been called!");
		super.onCreate(savedInstanceState);

		// Set the activity view to the map activity layout.
		this.setContentView(R.layout.activity_maps);

		// Load in the current settings.
		try {
			this.currentSettings.loadSettings(this);
		} catch (org.json.JSONException e) {
			// If there was an exception loading the settings simply log it and return.
			Log.e("onCreate", "Exception when loading settings", e);
			return;
		}

		// Set the map to null for now. It will be set when the callback is ready.
		this.map = null;

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) this.getSupportFragmentManager()
				.findFragmentById(R.id.map);


		// Set the map fragment callback if it's not null.
		if (mapFragment != null) {
			mapFragment.getMapAsync(this);
		} else {
			Toast.makeText(this, "Cannot find map!", Toast.LENGTH_LONG).show();
		}

		// Create a new fares popup window if one does not yet already exist.
		if (this.farePopupWindow == null) {
			this.farePopupWindow = new FarePopupWindow(this);
		}
	}

	@Override
	protected void onDestroy() {
		Log.v("onDestroy", "onDestroy called!");
		super.onDestroy();

		// Make sure there are routes to iterate though
		if (MapsActivity.allRoutes != null) {

			// Iterate though each route to get access to its shared stops and regular stops.
			for (Route route : MapsActivity.allRoutes) {

				// Iterate though each stop.
				Log.d("onDestroy", "Removing stop circles");
				for (Stop stop : route.getStops()) {

					// Remove the stop's circle.
					stop.removeStopCircle();

					// Remove stop's marker.
					stop.removeMarker();
				}

				// Get the shared stops for the route.
				Log.d("onDestroy", "Removing shared stop circles");
				SharedStop[] sharedStops = route.getSharedStops();

				// Iterate though each shared stop.
				for (SharedStop sharedStop : sharedStops) {

					// Remove each shared stop circles.
					sharedStop.removeSharedStopCircles();

					// Remove the shared stop's marker.
					sharedStop.removeMarker();
				}

				// Remove route polylines.
				Log.d("onDestroy", "Removing route polyline");
				route.removePolyline();
			}
		}

		if (MapsActivity.buses != null) {

			// Iterate though all the buses, and remove its marker.
			Log.d("onDestroy", "Removing bus markers");
			for (Bus bus : MapsActivity.buses) {
				bus.removeMarker();
			}
		}

		// Stop the update thread.
		if (MapsActivity.updateThread != null) {
			MapsActivity.updateThread.stop();
			MapsActivity.updateThread = null;
		}

		// Be sure to clear the map.
		if (this.map != null) {
			this.map.clear();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.v("onCreateOptionsMenu", "onCreateOptionsMenu has been called!");

		// Setup the inflater
		this.getMenuInflater().inflate(R.menu.menu, menu);

		// Iterate through all the routes that can be tracked (if allRoutes isn't null).
		if (MapsActivity.allRoutes != null) {
			for (int i = 0; i < MapsActivity.allRoutes.length; i++) {

				// Get the route object that we will be using from all the routes.
				Route route = MapsActivity.allRoutes[i];

				// Create the menu item that corresponds to the route object.
				MenuItem menuItem = menu.add(R.id.routes, Menu.NONE, 0, route.getRouteName());

				// Make sure the item is checkable.
				menuItem.setCheckable(true);

				// Determine whether or not the menu item should be checked before hand.
				menuItem.setChecked(route.getEnabled());
			}
		}

		// Check if night mode should be enabled by default, and set the checkbox to that value.
		menu.findItem(R.id.night_mode).setChecked(((V2) this.currentSettings.getSettingsImplementation())
				.getDarktheme());

		// Return true, otherwise the menu wont be displayed.
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		Log.v("onOptionsItemSelected", "onOptionsItemSelected has been called!");

		// Identify which method to call based on the item ID.
		// Because resource IDs will be non-final in Android Gradle Plugin version 5.0,
		// we cant use a switch case statement :(   Back to if else if else stuff.
		// Check if the item that was selected belongs to the other group
		if (item.getGroupId() == R.id.other) {
			this.onOtherOptionsItemSelected(item);
		} else if (item.getGroupId() == R.id.routes) { // Check if the item that was selected belongs to the routes group.
			try {
				this.onRouteItemToggled(item);
			} catch (Route.RouteException e) {
				Toast.makeText(this, "An error occurred while toggling that route",
						Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		} else {
			// Since the item's ID and group was not part of anything accounted for (uh oh),
			// log it as a warning!
			Log.w("onOptionsItemSelected", "Unaccounted menu item was checked!");
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Checks the selected item and executed different methods depending on its item ID.
	 * If the item ID is determined to not correspond with any method, a warning will be logged.
	 *
	 * @param item The menu item that was selected. This value cannot be null.
	 */
	private void onOtherOptionsItemSelected(@NonNull MenuItem item) {
		Log.v("onOtherOptionSelected", "onOtherOptionsItemSelected has been called!");

		// Identify what action to execute based on the item ID.
		// Because resource IDs will be non-final in Android Gradle Plugin version 5.0,
		// we cant use a switch case statement :(   Back to if else if else stuff.
		// noinspection IfStatementWithTooManyBranches - Disabes too many if else branches warning as we have no other choice.
		if (item.getItemId() == R.id.night_mode) { // Check if the item that was selected was the night mode toggle.
			Log.d("onOptionsItemSelected", "Toggling night mode...");

			// Create a boolean to store the resulting value of the menu item
			boolean enabled = !item.isChecked();

			// Toggle night mode
			this.toggleNightMode(enabled);

			// Set the menu item's checked value to that of the enabled value
			item.setChecked(enabled);
		} else if (item.getItemId() == R.id.settings) { // Check if the item that was selected was the settings button.

			// Launch the settings activity
			this.startActivity(new android.content.Intent(this, SettingsActivity.class));
		} else if (item.getItemId() == R.id.fares) { // Check if the item that was selected was the fares button.
			this.farePopupWindow.showFarePopupWindow();
		} else {

			// Since the item's ID was not part of anything accounted for (uh oh), log it as a warning!
			Log.w("onOptionsItemSelected", "Unaccounted menu item in the other group was checked!");
		}
	}

	@Override
	protected void onResume() {
		Log.v("onResume", "onResume has been called!");
		super.onResume();

		// Update the map's dynamic settings.
		this.updateMapSettings();

		if (MapsActivity.updateThread != null) {
			MapsActivity.updateThread.state = UpdateThread.STATE.RUN;
		}

		// (Re) start the update thread.
		MapsActivity.manageUpdateThread();
	}

	/**
	 * Called when the activity loses foreground state,
	 * is no longer focusable or before transition to stopped / hidden or destroyed state.
	 * The activity is still visible to user, so it's recommended to keep it visually active and continue updating the UI.
	 * Implementations of this method must be very quick because the next activity will not be resumed until this method returns.
	 * Followed by either onResume() if the activity returns back to the front, or onStop() if it becomes invisible to the user.
	 */
	@Override
	protected void onPause() {
		Log.v("onPause", "onPause has been called!");
		super.onPause();

		if (MapsActivity.updateThread != null) {
			MapsActivity.updateThread.state = UpdateThread.STATE.PAUSE;
		}
	}

	/**
	 * Manipulates the map once available. This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera.
	 * If Google Play services is not installed on the device,
	 * the user will be prompted to install it inside the SupportMapFragment.
	 * This method will only be triggered once the user has installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(@NonNull GoogleMap googleMap) {
		Log.v("onMapReady", "onMapReady has been called!");

		// Setup the map object at this point as it is finally initialized and ready.
		this.map = googleMap;

		// Move the camera to the 'home' position
		//noinspection MagicNumber
		this.map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.
				newLatLngZoom(new LatLng(64.8391975, -147.7684709), 11.0f));

		// Add a listener for when the camera has become idle (ie was moving isn't anymore).
		this.map.setOnCameraIdleListener(new AdjustZoom(this.map));

		// Add a listener for when a stop icon (circle) is clicked.
		this.map.setOnCircleClickListener(new fnsb.macstransit.Activities.activitylisteners
				.StopClicked(this, this.map));

		// Add a custom info window adapter, to add support for multiline snippets.
		this.map.setInfoWindowAdapter(new InfoWindowAdapter(this));

		// Set it so that if the info window was closed for a Stop marker,
		// make that marker invisible, so its just the dot.
		this.map.setOnInfoWindowCloseListener(new fnsb.macstransit.Activities.activitylisteners
				.StopDeselected());

		// Set it so that when an info window is clicked on, it launches a popup window
		this.map.setOnInfoWindowClickListener(new PopupWindow(this));

		// Update the map's dynamic settings.
		this.updateMapSettings();

		// Setup the actual thread object for the update thread if it didn't exist previously.
		if (MapsActivity.updateThread == null) {
			MapsActivity.updateThread = new UpdateThread(this.mainThreadHandler, this.map);
		}

		// Comments
		MapsActivity.updateThread.state = UpdateThread.STATE.RUN;
	}

	/**
	 * Updates the various settings on the map object determined by the settings file.
	 * It also redraws the buses and stops that are active on the map, and draws the polylines if they are enabled.
	 * This should be called when the map has been setup and is ready to be refreshed.
	 */
	private void updateMapSettings() {

		// Make sure to only execute the following if the maps object is not null (map has been setup).
		if (this.map != null) {

			// Comments
			V2 settings = (V2) this.currentSettings.getSettingsImplementation();

			// Enable traffic overlay based on settings.
			this.map.setTrafficEnabled(settings.getTraffic());

			// Set the the type of map based on settings.
			this.map.setMapType(settings.getMaptype());

			// Toggle night mode at this time if enabled.
			this.toggleNightMode(settings.getDarktheme());

			// Get the favorited routes from the settings object.
			Route[] favoritedRoutes = settings.getRoutes();

			if (!MapsActivity.selectedFavorites) {

				// If the favorited routes is not null, enable them.
				Route.enableFavoriteRoutes(favoritedRoutes);
			}

			// Try redrawing the buses.
			// Because we are iterating a static variable that is modified on a different thread
			// there is a possibility of a concurrent modification.
			try {
				this.drawBuses();
			} catch (ConcurrentModificationException e) {
				Log.e("updateMapSettings", "Unable to draw all buses due to concurrent modification", e);
			}

			// Draw the stops.
			this.drawStops();

			// Draw the routes.
			if (settings.getPolylines()) {
				this.drawRoutes();
			}
		} else {
			Log.w("updateMapSettings", "Map is not yet ready!");
		}
	}

	/**
	 * Toggles the map's night mode (dark theme).
	 *
	 * @param enabled Whether to toggle the maps night mode
	 */
	public void toggleNightMode(boolean enabled) {
		if (this.map != null) {
			this.map.setMapStyle(enabled ?
					MapStyleOptions.loadRawResourceStyle(this, R.raw.nightmode) :
					MapStyleOptions.loadRawResourceStyle(this, R.raw.standard));
		} else {
			Log.w("toggleNightMode", "Map is not yet ready");
		}
	}
}
