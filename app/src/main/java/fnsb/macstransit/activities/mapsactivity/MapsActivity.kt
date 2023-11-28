package fnsb.macstransit.activities.mapsactivity

import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Parcelable
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.settings.V2
import fnsb.macstransit.R
import android.util.Log
import android.view.Menu
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.SupportMapFragment
import fnsb.macstransit.activities.SettingsActivity
import fnsb.macstransit.activities.mapsactivity.mappopups.FarePopupWindow
import fnsb.macstransit.databinding.ActivityMapsBinding
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.SharedStop
import fnsb.macstransit.routematch.Stop
import fnsb.macstransit.settings.CurrentSettings
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException

class MapsActivity: FragmentActivity() {

	/**
	 * The view model for the maps activity.
	 * This is usually where all the large functions and additional properties are.
	 */
	private lateinit var viewModel: MapsViewModel

	/**
	 * Create a variable to store our fare popup window instance.
	 */
	private lateinit var farePopupWindow: FarePopupWindow

	override fun onCreate(savedInstanceState: Bundle?) {
		Log.v("onCreate", "onCreate has been called!")
		super.onCreate(savedInstanceState)

		// Setup view model.
		viewModel = ViewModelProvider(this)[MapsViewModel::class.java]

		// Setup binding.
		val binding: ActivityMapsBinding = ActivityMapsBinding.inflate(layoutInflater)
		binding.viewmodel = viewModel
		binding.lifecycleOwner = this

		// Set the activity view to the map activity layout.
		setContentView(binding.root)

		// Load in the current settings.
		try {
			CurrentSettings.loadSettings(this)
		} catch (e: JSONException) {

			// If there was an exception loading the settings simply log it and return.
			Log.e("onCreate", "Exception when loading settings", e)
			return
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val supportFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

		// Launch the maps coroutine (which sets up the Google Map object).
		lifecycleScope.launch {
			val setupJob = launch(Dispatchers.Main, CoroutineStart.LAZY) { viewModel.setupMap(supportFragment, this@MapsActivity) }
			Log.v("onCreate", "Setting up map")
			setupJob.start()
			setupJob.join()
			Log.v("onCreate", "Map finished")
			repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.runUpdater() }
		}

		// Setup the fares popup window.
		Log.v("onCreate", "Setting up fare window")
		farePopupWindow = FarePopupWindow(this)

		// Setup all our routes.
		if (viewModel.routes.isEmpty()) {

			// Get the extras from the intent.
			val extraBundle: Bundle = intent.extras ?: return

			// Get the routes from the bundle as a parcelable array.
			if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				val routes: Array<Route> = extraBundle.getParcelableArray("Routes", Route::class.java) ?: return

				// Parse the routes from the parcelable to our hashmap.
				for (route in routes) {
					viewModel.routes[route.name] = route
				}
			} else {
				@Suppress("DEPRECATION") // Suppressed because corrected version doesn't exist for APIs earlier than Tiramisu
				val routesParcelable: Array<Parcelable> = extraBundle.getParcelableArray("Routes") ?: return

				for (route in routesParcelable) {
					if (route is Route) {
						viewModel.routes[route.name] = route
					}
				}
			}
		}
	}

	override fun onDestroy() {
		Log.v("onDestroy", "onDestroy called!")
		super.onDestroy()

		// Launch the following as a cleanup job (that way we can essentially multi-thread the onDestroy process)
		lifecycleScope.launch(Dispatchers.Main, start= CoroutineStart.UNDISPATCHED) {
			Log.i("onDestroy", "Beginning onDestroy cleanup coroutine...")

			// Iterate though each route to get access to its shared stops and regular stops.
			for (route: Route in viewModel.routes.values) {

				Log.d("onDestroy", "Removing stop circles")
				for (stop: Stop in route.stops.values) {
					stop.removeStopCircle()
					stop.removeMarker()
				}

				Log.d("onDestroy", "Removing shared stop circles")
				for (sharedStop: SharedStop in route.sharedStops.values) {
					sharedStop.removeSharedStopCircles()
					sharedStop.removeMarker()
				}

				// Remove route polylines.
				Log.d("onDestroy", "Removing route polyline")
				route.removePolyline()
			}

			Log.i("onDestroy", "Finished onDestroy cleanup coroutine")
		}

		Log.d("onDestroy", "Removing bus markers")
		for (bus: Bus in viewModel.buses) {
			bus.removeMarker()
		}

		// Stop the update thread.
		if (viewModel.updater != null) {
			viewModel.updater!!.run = false
			viewModel.updater = null
		}

		// Be sure to clear the map.
		if (viewModel.map != null) {
			viewModel.map!!.clear()
			viewModel.map = null
		}



		Log.v("onDestroy", "Finished onDestroy")
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		Log.v("onCreateOptionsMenu", "onCreateOptionsMenu has been called!")

		// Setup the inflater.
		menuInflater.inflate(R.menu.menu, menu)

		// Create the menu item that corresponds to the route object.
		for (name in viewModel.routes.keys) {

			// Make sure the item is checkable.
			menu.add(R.id.routes, name.hashCode(), Menu.NONE, name).isCheckable = true
		}

		// Return what ever the default behaviour would be when calling this method if it were not overridden.
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		Log.v("onPrepareOptionsMenu", "onPrepareOptionsMenu has been called!")

		// Iterate through all the routes that can be tracked (if allRoutes isn't null).
		for ((name, route) in viewModel.routes) {

			// Determine whether or not the menu item should be checked before hand.
			val checked: Boolean = route.enabled

			// Set the menu item to be checked if the route it corresponds to is enabled.
			Log.d("onPrepareOptionsMenu", "Setting $name to be enabled: $checked")
			menu.findItem(name.hashCode()).isChecked = checked
		}

		// Check if night mode should be enabled by default, and set the checkbox to that value.
		menu.findItem(R.id.night_mode).isChecked = (CurrentSettings.settingsImplementation as V2).darktheme

		// Return what ever the default behaviour would be when calling this method if it were not overridden.
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
		Log.v("onOptionsItemSelected", "onOptionsItemSelected has been called!")

		// Identify which method to call based on the item ID.
		// Check if the item that was selected belongs to the other group
		when (item.groupId) {
			R.id.other -> {

				// Identify what action to execute based on the item ID.
				when (item.itemId) {

					// Check if the item that was selected was the night mode toggle.
					R.id.night_mode -> {
						if (viewModel.map == null) {
							return false
						}

						Log.d("onOptionsItemSelected", "Toggling night mode...")

						// Create a boolean to store the resulting value of the menu item.
						val enabled = !item.isChecked

						// Toggle night mode
						MapsViewModel.toggleNightMode(viewModel.map!!, this, enabled)

						// Set the menu item's checked value to that of the enabled value.
						item.isChecked = enabled
					}

					// Check if the item that was selected was the settings button.
					R.id.settings -> {

						// Create the intent to launch the settings activity.
						val settingsIntent = android.content.
						Intent(this, SettingsActivity::class.java)

						// Add all the trackable routes as an extra to the intent.
						settingsIntent.putExtra("Routes", viewModel.routes.values.toTypedArray())

						// Start the settings activity.
						startActivity(settingsIntent)
					}

					// Check if the item that was selected was the fares button.
					R.id.fares -> farePopupWindow.showFarePopupWindow()

					// Since the item's ID was not part of anything accounted for (uh oh), log it as a warning!
					else -> Log.w("onOptionsItemSelected", "Unaccounted menu item in the other group was checked!")
				}
			}

			// Check if the item that was selected belongs to the routes group.
			R.id.routes -> {

				// Create a boolean to store the resulting value of the menu item.
				val enabled = !item.isChecked

				// Get the route that was selected.
				val route: Route = viewModel.routes[item.title] ?: return super.onOptionsItemSelected(item)

				// Set the route to enabled.
				route.enabled = enabled
				Log.d("onOptionsItemSelected", "Selected route ${route.name}")

				// If the map is null at this point just return early (skip redrawing).
				if (viewModel.map == null) {
					return super.onOptionsItemSelected(item)
				}

				// Try to (re)draw the buses onto the map.
				// Because we are iterating a static variable that is modified on a different thread
				// there is a possibility of a concurrent modification.
				try {
					viewModel.drawBuses()
				} catch (e: ConcurrentModificationException) {
					Log.e("onOptionsItemSelected",
					      "Unable to redraw all buses due to concurrent modification", e)
				}

				// (Re) draw the stops onto the map.
				viewModel.drawStops()

				// (Re) draw the routes onto the map (if enabled).
				if ((CurrentSettings.settingsImplementation as V2).polylines) {
					viewModel.drawRoutes()
				}

				// Set the menu item's checked value to that of the enabled value
				item.isChecked = enabled
			}

			// Since the item's ID and group was not part of anything accounted for (uh oh), log it as a warning!
			else -> Log.w("onOptionsItemSelected", "Unaccounted menu item was checked!")
		}

		// Return what ever the default behaviour would be when calling this method if it were not overridden.
		return super.onOptionsItemSelected(item)
	}

	override fun onResume() {
		Log.v("onResume", "onResume has been called!")
		super.onResume()

		// Update the map's dynamic settings.
		viewModel.updateMapSettings()

		// Resume the coroutine
		if (viewModel.updater != null) {
			viewModel.runUpdater()
		}
	}

	override fun onPause() {
		Log.v("onPause", "onPause has been called!")
		super.onPause()

		// Stop the coroutine
		if (viewModel.updater != null) {
			viewModel.updater!!.run = false
		}
	}

	companion object {

		/**
		 * Used to determine if the MapsActivity has been run before in the app's lifecycle.
		 * This will be set to true coming out of SplashActivity
		 */
		var firstRun: Boolean = true

	}
}
