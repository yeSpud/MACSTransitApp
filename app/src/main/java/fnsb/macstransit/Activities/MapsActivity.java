package fnsb.macstransit.Activities;

import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MapStyleOptions;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import fnsb.macstransit.Activities.ActivityListeners.AdjustZoom;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;
import fnsb.macstransit.Settings.CurrentSettings;
import fnsb.macstransit.Threads.UpdateThread;

public class MapsActivity extends androidx.fragment.app.FragmentActivity implements
		com.google.android.gms.maps.OnMapReadyCallback {

	/**
	 * Create an array of all the routes that are used by the transit system.
	 * For now leave it uninitialized, as it will be populated from the Splash Activity,
	 * and initialized in the onCreate method.
	 */
	public static Route[] allRoutes; // TODO Check for concurrent exception && Not Null!

	/**
	 * TODO Documentation
	 */
	public static Bus[] buses = new Bus[0]; // TODO Check for concurrent exception!

	/**
	 * Create an instance of the RouteMatch object that will be used for this app.
	 */
	public static fnsb.macstransit.RouteMatch.RouteMatch routeMatch;

	/**
	 * Create the map object.
	 */
	public static GoogleMap map;

	/**
	 * TODO Documentation
	 */
	private final UpdateThread updateThread = new UpdateThread(this);

	/**
	 * Create an array of all the Shared Stops (stops that share a location).
	 */
	@Deprecated
	public SharedStop[] sharedStops = new SharedStop[0];

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
	protected void onCreate(@Nullable android.os.Bundle savedInstanceState) {
		Log.v("onCreate", "onCreate has been called!");
		super.onCreate(savedInstanceState);

		// Set the activity view to the map activity layout.
		this.setContentView(R.layout.activity_maps);

		// Load in the current settings.
		try {
			CurrentSettings.loadSettings(this);
		} catch (JSONException e) {
			// If there was an exception loading the settings simply log it and return.
			Log.e("onCreate", "Exception when loading settings", e);
			return;
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) this.getSupportFragmentManager()
				.findFragmentById(R.id.map);

		// Set the map fragment callback.
		if (mapFragment != null) {
			mapFragment.getMapAsync(this);
		} else {
			Toast.makeText(this, "Cannot find map!", Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Initialize the contents of the Activity's standard options menu.
	 * You should place your menu items in to menu.
	 * <p>
	 * This is only called once, the first time the options menu is displayed.
	 * To update the menu every time it is displayed, see onPrepareOptionsMenu(Menu).
	 * <p>
	 * The default implementation populates the menu with standard system menu items.
	 * These are placed in the Menu#CATEGORY_SYSTEM group so that they will be correctly ordered with application-defined menu items.
	 * Deriving classes should always call through to the base implementation.
	 * <p>
	 * You can safely hold on to menu (and any items created from it),
	 * making modifications to it as desired, until the next time onCreateOptionsMenu() is called.
	 * <p>
	 * When you add items to the menu,
	 * you can implement the Activity's onOptionsItemSelected(MenuItem) method to handle them there.
	 *
	 * @param menu The options menu in which you place your items.
	 * @return You must return true for the menu to be displayed; if you return false it will not be shown.
	 */
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
				MenuItem menuItem = menu.add(R.id.routes, Menu.NONE, 0, route.routeName);

				// Make sure the item is checkable.
				menuItem.setCheckable(true);

				// Determine whether or not the menu item should be checked before hand.
				menuItem.setChecked(route.enabled);
			}
		}

		// Check if night mode should be enabled by default, and set the checkbox to that value.
		menu.findItem(R.id.night_mode).setChecked(CurrentSettings.settings.getDarktheme());

		// Return true, otherwise the menu wont be displayed.
		return true;
	}

	/**
	 * This hook is called whenever an item in your options menu is selected.
	 * The default implementation simply returns false to have the normal processing happen
	 * (calling the item's Runnable or sending a message to its Handler as appropriate).
	 * You can use this method for any items for which you would like to do processing without those other facilities.
	 * <p>
	 * Derived classes should call through to the base class for it to perform the default menu handling.
	 *
	 * @param item The menu item that was selected. This value cannot be null.
	 * @return Return false to allow normal menu processing to proceed, true to consume it here.
	 */
	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		Log.v("onOptionsItemSelected", "onOptionsItemSelected has been called!");

		// Identify which method to call based on the item ID.
		// Use a switch statement instead of multiple else ifs for this (as it's just slightly faster).
		switch (item.getGroupId()) {

			// Check if the item that was selected belongs to the other group
			case R.id.other:
				this.onOtherOptionsItemSelected(item);
				break;

			// Check if the item that was selected belongs to the routes group.
			case R.id.routes:
				try {
					this.onRouteItemToggled(item);
				} catch (Route.RouteException e) {
					Toast.makeText(this, "An error occurred while toggling that route",
							Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				break;
			default:
				// Since the item's ID and group was not part of anything accounted for (uh oh),
				// log it as a warning!
				Log.w("onOptionsItemSelected", "Unaccounted menu item was checked!");
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * TODO Documentation
	 *
	 * @param item The menu item that was selected. This value cannot be null.
	 */
	private void onOtherOptionsItemSelected(@NotNull MenuItem item) {
		Log.v("onOtherOptionSelected", "onOtherOptionsItemSelected has been called!");

		// Identify what action to execute based on the item ID.
		// Use a switch statement instead of multiple else ifs for this (as it's just slightly faster).
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

				// Launch the settings activity
				this.startActivity(new Intent(this, SettingsActivity.class));
				break;
			// Check if the item that was selected was the fares button.
			case R.id.fares:
				new FarePopupWindow(this).showFarePopupWindow();
				break;

			default:
				// Since the item's ID was not part of anything accounted for (uh oh), log it as a warning!
				Log.w("onOptionsItemSelected", "Unaccounted menu item in the other group was checked!");
				break;
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @param item
	 */
	private void onRouteItemToggled(@NotNull MenuItem item) throws Route.RouteException {
		Log.v("onRouteItemSelected", "onRouteItemSelected bas been called!");

		// Create a boolean to store the resulting value of the menu item.
		boolean enabled = !item.isChecked();

		// Determine which route from all the routes was just selected.
		Route selectedRoute = null;
		for (Route route : MapsActivity.allRoutes) {
			if (route.routeName.equals(item.getTitle().toString())) {
				selectedRoute = route;
				break;
			}
		}

		// Make sure the selected route was found.
		if (selectedRoute == null) {
			throw new Route.RouteException("Unable to determine selected route!");
		}

		// Updated the selected route's boolean.
		selectedRoute.enabled = enabled;

		// (Re) draw the buses onto the map.
		MapsActivity.drawBuses();

		// (Re) draw the stops onto the map.
		this.drawStops();

		// (Re) draw the routes onto the map (if enabled).
		if (CurrentSettings.settings.getPolylines()) {
			MapsActivity.drawRoutes();
		}

		// Set the menu item's checked value to that of the enabled value
		item.setChecked(enabled);
	}


	/**
	 * Called when the activity will start interacting with the user.
	 * At this point your activity is at the top of its activity stack, with user input going to it.
	 */
	@Override
	protected void onResume() {
		Log.v("onResume", "onResume has been called!");
		super.onResume();

		// Update the map's dynamic settings.
		this.updateMapSettings();
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
		this.updateThread.run = false;
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
	 */
	@Override
	protected void onDestroy() {
		Log.v("onDestroy", "onDestroy has been called!");
		super.onDestroy();
		this.updateThread.run = false;
	}

	/**
	 * Manipulates the map once available. This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera.
	 * If Google Play services is not installed on the device,
	 * the user will be prompted to install it inside the SupportMapFragment.
	 * This method will only be triggered once the user has installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		Log.v("onMapReady", "onMapReady has been called!");

		// Setup the map object at this point as it is finally initialized and ready.
		MapsActivity.map = googleMap;

		// Move the camera to the 'home' position
		//noinspection MagicNumber
		MapsActivity.map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory
				.newLatLngZoom(new com.google.android.gms.maps.model
						.LatLng(64.8391975, -147.7684709), 11.0f));

		// Add a listener for when the camera has become idle (ie was moving isn't anymore).
		MapsActivity.map.setOnCameraIdleListener(new AdjustZoom());

		// Add a listener for when a stop icon (circle) is clicked.
		MapsActivity.map.setOnCircleClickListener(new fnsb.macstransit.Activities.ActivityListeners
				.StopClicked(this));

		// Add a custom info window adapter, to add support for multiline snippets.
		MapsActivity.map.setInfoWindowAdapter(new InfoWindowAdapter(this));

		// Set it so that if the info window was closed for a Stop marker,
		// make that marker invisible, so its just the dot.
		MapsActivity.map.setOnInfoWindowCloseListener(new fnsb.macstransit.Activities.ActivityListeners
				.StopDeselected());

		// Set it so that when an info window is clicked on, it launches a popup window
		MapsActivity.map.setOnInfoWindowClickListener(new PopupWindow(this));

		// Update the map's dynamic settings.
		this.updateMapSettings();
	}

	/**
	 * Draws the stops and shared stops onto the map, and adjusts the stop sizes based on the zoom level.
	 */
	public void drawStops() {
		// TODO Draw the stops (and shared stops) based on if their respective routes are enabled or not.
		// Check and load all the shared stops

		// FIXME

		if (MapsActivity.allRoutes != null) {
			for (Route route : MapsActivity.allRoutes) {

				if (route.enabled) {
					if (route.stops[0].circle == null) {
						for (Stop stop : route.stops) {
							stop.showStop(MapsActivity.map);
						}
					} else {
						if (!route.stops[0].circle.isVisible()) {
							for (Stop stop : route.stops) {
								stop.showStop(MapsActivity.map);
							}
						}
					}

					// TODO Show shared stop
				} else {

					if (route.stops != null || route.stops.length != 0) {
						if (route.stops[0].circle != null) {
							if (route.stops[0].circle.isVisible()) {
								for (Stop stop : route.stops) {
									stop.hideStop();
								}
							}
						}
					}

					// TODO Hide shared stop
				}
			}
		}

		// Adjust the circle sizes of the stops on the map given the current zoom.
		AdjustZoom.resizeStops();
	}

	/**
	 * TODO Documentation
	 */
	private static void drawRoutes() {
		// Draw the route polylines based on if they are enabled or not.
		if (MapsActivity.allRoutes != null) {
			for (Route route : MapsActivity.allRoutes) {
				try {
					if (route.getPolyline() == null) {
						route.createPolyline(MapsActivity.map);
					}
					route.getPolyline().setVisible(route.enabled);
				} catch (NullPointerException e) {
					Log.w("drawRoutes", String.format("Route %s doesn't have polyline!", route.routeName));
				}
			}
		}
	}

	/**
	 * TODO Documentation
	 */
	private static void drawBuses() {
		// Force redraw the buses based on if their routes are enabled or not.
		// Otherwise this is taken care of in the update thread.
		for (Bus bus : MapsActivity.buses) {
			try {
				bus.getMarker().setVisible(bus.route.enabled);
			} catch (NullPointerException e) {
				Log.w("drawBuses", "Bus doesn't have a marker!");
			}
		}
	}

	/**
	 * TODO Documentation
	 */
	public void updateMapSettings() {
		if (MapsActivity.map != null) {
			// Enable traffic overlay based on settings.
			MapsActivity.map.setTrafficEnabled(CurrentSettings.settings.getTraffic());

			// Set the the type of map based on settings.
			MapsActivity.map.setMapType(CurrentSettings.settings.getMaptype());

			// Toggle night mode at this time if enabled.
			this.toggleNightMode(CurrentSettings.settings.getDarktheme());

			// Enable street-view options based on settings.
			/* - DEPRECATED -
			if (CurrentSettings.settings.getStreetview()) {
				StreetViewListener streetViewListener = new StreetViewListener(this);
				MapsActivity.map.setOnInfoWindowLongClickListener(streetViewListener);
			}
			 */

			// Enable all the routes that were favorited.
			Route.enableFavoriteRoutes(MapsActivity.allRoutes, CurrentSettings.settings.getRoutes());

			// Redraw the buses.
			MapsActivity.drawBuses();

			// Draw the stops.
			this.drawStops();

			// Draw the routes.
			if (CurrentSettings.settings.getPolylines()) {
				MapsActivity.drawRoutes();
			}

			this.updateThread.run = true;
			if (!this.updateThread.thread().isAlive()) {
				this.updateThread.thread().start();
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
		MapsActivity.map.setMapStyle(enabled ?
				MapStyleOptions.loadRawResourceStyle(this, R.raw.nightmode) :
				MapStyleOptions.loadRawResourceStyle(this, R.raw.standard));
	}
}
