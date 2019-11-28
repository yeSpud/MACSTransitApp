package fnsb.macstransit.Activities;

import android.util.Log;
import android.view.Menu;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MapStyleOptions;

import fnsb.macstransit.Activities.ActivityListeners.AdjustZoom;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;
import fnsb.macstransit.Threads.UpdateThread;

public class MapsActivity extends androidx.fragment.app.FragmentActivity implements
		com.google.android.gms.maps.OnMapReadyCallback {

	/**
	 * Create an array of all the routes that are used by the transit system. For now leave it uninitialized,
	 * as it will be dynamically generated in the onCreate method.
	 */
	public static Route[] allRoutes;

	/**
	 * Create an instance of the route match object that will be used for this app.
	 */
	public static fnsb.macstransit.RouteMatch.RouteMatch routeMatch;

	/**
	 * Create an array list to determine which routes have been selected from the menu to track.
	 */
	public Route[] selectedRoutes = new Route[0];

	/**
	 * Create an array of all the Shared Stops (stops that share a location).
	 */
	public SharedStop[] sharedStops = new SharedStop[0];

	/**
	 * Create the map object.
	 */
	public GoogleMap map;

	/**
	 * Create an instance of the thread object that will be used to pull data from the routematch server.
	 */
	private UpdateThread thread = new UpdateThread(this, 3000);

	/**
	 * Boolean to check whether or not the menu items for the routes have been (dynamically) created.
	 * This is used to prevent making multiple duplicate menu items in {@code onPrepareOptionsMenu(Menu menu)}.
	 */
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
		// Check if the menu has not yet been created.
		if (!menuCreated) {

			// Iterate through all the routes that can be tracked.
			for (Route route : MapsActivity.allRoutes) {

				// Add the route to the routes menu group, and make sure its checkable.
				menu.add(R.id.routes, Menu.NONE, Menu.NONE, route.routeName).setCheckable(true);
			}

			// Once finished, set the menuCreated variable to true so that this will not be run again.
			menuCreated = true;
		}

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Initialize the contents of the Activity's standard options menu. You should place your menu items in to menu.
	 * <p>
	 * This is only called once, the first time the options menu is displayed. To update the menu every time it is displayed,
	 * see {@code onPrepareOptionsMenu(Menu)}.
	 * <p>
	 * The default implementation populates the menu with standard system menu items.
	 * These are placed in the {@code Menu.CATEGORY_SYSTEM} group so that they will be correctly ordered with application-defined menu items.
	 * Deriving classes should always call through to the base implementation.
	 * <p>
	 * You can safely hold on to menu (and any items created from it),
	 * making modifications to it as desired, until the next time {@code onCreateOptionsMenu()} is called.
	 * <p>
	 * When you add items to the menu, you can implement the Activity's {@code onOptionsItemSelected(MenuItem)} method to handle them there.
	 *
	 * @param menu The options menu in which you place your items.
	 * @return You must return true for the menu to be displayed; if you return false it will not be shown.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Setup the inflater
		this.getMenuInflater().inflate(R.menu.menu, menu);

		// Set the menuCreated variable to false in order to rerun the dynamic menu creation in onPrepareOptionsMenu().
		this.menuCreated = false;

		// Check if night mode should be enabled by default, and set the checkbox to that value
		menu.findItem(R.id.night_mode).setChecked(SettingsPopupWindow.DEFAULT_NIGHT_MODE);

		// Return true, otherwise the menu wont be displayed.
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
		switch (item.getGroupId()) {
			// Check if the item that was selected belongs to the other group
			case R.id.other:
				switch (item.getItemId()) {
					// Check if the item that was selected was the night mode toggle.
					case R.id.night_mode:
						Log.d("onOptionsItemSelected", "Toggling night mode...");

						// Create a boolean to store the resulting value of the menu item
						boolean enabled = !item.isChecked();

						// Toggle night mode
						this.toggleNightMode(enabled);

						// Set the menu item's checked value to that of the enabled value
						item.setChecked(enabled);
						break;
					// Check if the item that was selected was the settings button.
					case R.id.settings:
						Log.d("onOptionsItemSelected", "Showing settings dialog...");
						SettingsPopupWindow settingsPopupWindow = new SettingsPopupWindow(this);
						settingsPopupWindow.showSettingsPopup();
						break;

					default:
						// Since the item's ID was not part of anything accounted for (uh oh), log it as a warning!
						Log.w("onOptionsItemSelected", "Unaccounted menu item in the other group was checked!");
						break;
				}
				break;
			// Check if the item that was selected belongs to the routes group.
			case R.id.routes:
				// Create a boolean to store the resulting value of the menu item
				boolean enabled = !item.isChecked();

				// Then clear the shared stops since they will be recreated
				this.sharedStops = SharedStop.clearSharedStops(this.sharedStops);

				// Then clear the regular stops from the map (as the stops to be displayed will be re-evaluated)
				Stop.removeStops(this.selectedRoutes);

				// Toggle the route based on the menu item's title, and its enabled value
				this.selectedRoutes = enabled ?
						Route.enableRoutes(item.getTitle().toString(), this.selectedRoutes) :
						Route.disableRoute(item.getTitle().toString(), this.selectedRoutes);


				// Draw the buses.
				fnsb.macstransit.RouteMatch.Bus.drawBuses(this.selectedRoutes, this.map);

				// If enabled, draw polylines
				if (SettingsPopupWindow.SHOW_POLYLINES) {
					for (Route route : this.selectedRoutes) {
						if (route.getPolyline() == null) {
							route.createPolyline(this.map);
						}
					}
				}

				// (Re) draw the stops onto the map
				this.drawStops();

				// Set the menu item's checked value to that of the enabled value
				item.setChecked(enabled);
				break;
			default:
				// Since the item's ID and group was not part of anything accounted for (uh oh), log it as a warning!
				Log.w("Menu", "Unaccounted menu item was checked!");
				break;
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
		this.setContentView(R.layout.activity_maps);

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		((com.google.android.gms.maps.SupportMapFragment) java.util.Objects.requireNonNull(this.getSupportFragmentManager()
				.findFragmentById(R.id.map))).getMapAsync(this);

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
		this.map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory
				.newLatLngZoom(new com.google.android.gms.maps.model
						.LatLng(64.8391975, -147.7684709), 11.0f));

		// Add a listener for when the camera has become idle (ie was moving isn't anymore).
		this.map.setOnCameraIdleListener(new AdjustZoom(this));

		// Add a listener for when a stop icon (circle) is clicked.
		this.map.setOnCircleClickListener(new fnsb.macstransit.Activities.ActivityListeners.StopClicked(this));

		// Add a custom info window adapter, to add support for multiline snippets.
		this.map.setInfoWindowAdapter(new InfoWindowAdapter(this));

		// Set it so that if the info window was closed for a Stop marker, make that marker invisible, so its just the dot.
		this.map.setOnInfoWindowCloseListener(new fnsb.macstransit.Activities.ActivityListeners.StopDeselected());

		// Set it so that when an info window is clicked on, it launches a popup window
		this.map.setOnInfoWindowClickListener(new PopupWindow(this));

		// Enable traffic overlay based on settings.
		this.map.setTrafficEnabled(SettingsPopupWindow.ENABLE_TRAFFIC_VIEW);

		// Toggle night mode at this time if enabled.
		this.toggleNightMode(SettingsPopupWindow.DEFAULT_NIGHT_MODE);
	}

	/**
	 * Draws the stops and shared stops onto the map, and adjusts the stop sizes based on the zoom level.
	 */
	public void drawStops() {
		// Check and load all the shared stops.
		this.sharedStops = SharedStop.findSharedStops(this.selectedRoutes, this.sharedStops);

		// Create and show the shared stops on the map if there are any (this.sharedStops will have a size greater than 0)
		if (this.sharedStops.length > 0) {
			SharedStop.addSharedStop(this.map, this.sharedStops);
		}

		// Create and show the regular stops.
		Stop.addStop(this.map, this.selectedRoutes, this.sharedStops);

		// Adjust the circle sizes of the stops on the map given the current zoom.
		AdjustZoom.adjustCircleSize(this.map.getCameraPosition().zoom, this.sharedStops);
	}

	/**
	 * Toggles the map's night mode (dark theme).
	 *
	 * @param enabled Whether to toggle the maps night mode
	 */
	public void toggleNightMode(boolean enabled) {
		this.map.setMapStyle(enabled ?
				MapStyleOptions.loadRawResourceStyle(this, R.raw.nightmode) :
				MapStyleOptions.loadRawResourceStyle(this, R.raw.standard));
	}
}
