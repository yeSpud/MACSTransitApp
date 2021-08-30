package fnsb.macstransit.activities.mapsactivity

import fnsb.macstransit.routematch.Route.RouteException
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.settings.V2
import fnsb.macstransit.R
import android.widget.Toast
import android.util.Log
import android.view.Menu
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import fnsb.macstransit.activities.mapsactivity.mappopups.FarePopupWindow
import fnsb.macstransit.databinding.ActivityMapsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ConcurrentModificationException

class MapsActivity: androidx.fragment.app.FragmentActivity() {

	/**
	 * Documentation
	 */
	lateinit var viewModel: MapsViewModel

	/**
	 * Documentation
	 */
	private val currentSettings = fnsb.macstransit.settings.CurrentSettings

	/**
	 * Create a variable to store our fare popup window instance.
	 */
	private lateinit var farePopupWindow: FarePopupWindow

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		Log.v("onCreate", "onCreate has been called!")
		super.onCreate(savedInstanceState)

		// Setup view model.
		this.viewModel = androidx.lifecycle.ViewModelProvider(this).get(MapsViewModel::class.java)

		// Setup binding.
		val binding: ActivityMapsBinding = ActivityMapsBinding.inflate(this.layoutInflater)
		binding.viewmodel = this.viewModel
		binding.lifecycleOwner = this

		// Set the activity view to the map activity layout.
		this.setContentView(binding.root)

		// Comments
		if (this.intent.extras != null) {
			this.viewModel.setRouteMatch(this.intent.extras!!)
		}

		// Load in the current settings.
		try {
			this.currentSettings.loadSettings(this)
		} catch (e: org.json.JSONException) {

			// If there was an exception loading the settings simply log it and return.
			Log.e("onCreate", "Exception when loading settings", e)
			return
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val supportFragment: SupportMapFragment =
				(this.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

		// Comments
		this.lifecycleScope.launchWhenCreated { this@MapsActivity.viewModel.mapCoroutine(supportFragment) }

		// Comments
		this.farePopupWindow = FarePopupWindow(this)
	}

	override fun onDestroy() {
		Log.v("onDestroy", "onDestroy called!")
		super.onDestroy()

		// Launch the following as a cleanup job (that way we can essentially multi-thread the onDestroy process)
		lifecycleScope.launch(Dispatchers.Main, start=kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
			Log.i("onDestroy", "Beginning onDestroy cleanup coroutine...")

			// Iterate though each route to get access to its shared stops and regular stops.
			allRoutes.forEach { route ->

				// Iterate though each stop.
				Log.d("onDestroy", "Removing stop circles")
				route.stops.forEach {

					// Remove the stop's circle.
					it.removeStopCircle()

					// Remove stop's marker.
					it.removeMarker()
				}

				// Get the shared stops for the route.
				Log.d("onDestroy", "Removing shared stop circles")
				route.sharedStops.forEach {

					// Remove each shared stop circles.
					it.removeSharedStopCircles()

					// Remove the shared stop's marker.
					it.removeMarker()
				}

				// Remove route polylines.
				Log.d("onDestroy", "Removing route polyline")
				route.removePolyline()
			}

			Log.i("onDestroy", "Finished onDestroy cleanup coroutine")
		}

		// Iterate though all the buses, and remove its marker.
		Log.d("onDestroy", "Removing bus markers")
		for (bus in this.viewModel.buses) {
			bus.removeMarker()
		}

		// Stop the update thread.
		if (this.viewModel.updater != null) {
			this.viewModel.updater!!.run = false
			this.viewModel.updater = null
		}

		// Be sure to clear the map.
		if (this.viewModel.map != null) {
			this.viewModel.map!!.clear()
			this.viewModel.map = null
		}

		Log.i("onDestroy", "Finished onDestroy")
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		Log.v("onCreateOptionsMenu", "onCreateOptionsMenu has been called!")

		// Setup the inflater
		this.menuInflater.inflate(R.menu.menu, menu)

		// Iterate through all the routes that can be tracked (if allRoutes isn't null).
		for (i in allRoutes.indices) {

			// Get the route object that we will be using from all the routes.
			val route = allRoutes[i]

			// Create the menu item that corresponds to the route object.
			val menuItem = menu.add(R.id.routes, Menu.NONE, 0, route.routeName)

			// Make sure the item is checkable.
			menuItem.isCheckable = true

			// Determine whether or not the menu item should be checked before hand.
			Log.i("onCreateOptionsMenu", "Setting ${route.routeName} to be enabled: ${route.enabled}")
			menuItem.isChecked = route.enabled
		}

		// Check if night mode should be enabled by default, and set the checkbox to that value.
		menu.findItem(R.id.night_mode).isChecked =
				(currentSettings.settingsImplementation as V2).darktheme

		// Return true, otherwise the menu wont be displayed.
		return true
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
						Log.d("onOptionsItemSelected", "Toggling night mode...")

						// Create a boolean to store the resulting value of the menu item
						val enabled = !item.isChecked

						// Toggle night mode
						this.viewModel.toggleNightMode(enabled)

						// Set the menu item's checked value to that of the enabled value
						item.isChecked = enabled
					}

					// Check if the item that was selected was the settings button.
					R.id.settings -> {

						// Launch the settings activity
						this.startActivity(android.content.Intent(this, fnsb.
						macstransit.activities.SettingsActivity::class.java))
					}

					// Check if the item that was selected was the fares button.
					R.id.fares -> {
						this.farePopupWindow.showFarePopupWindow()
					}

					else -> {

						// Since the item's ID was not part of anything accounted for (uh oh), log it as a warning!
						Log.w("onOptionsItemSelected",
						      "Unaccounted menu item in the other group was checked!")
					}
				}
			}

			// Check if the item that was selected belongs to the routes group.
			R.id.routes -> {
				try {

					// Create a boolean to store the resulting value of the menu item.
					val enabled = !item.isChecked

					// Determine which route from all the routes was just selected.
					var selectedRoute: Route? = null
					for (route in allRoutes) {
						if (route.routeName == item.title.toString()) {
							selectedRoute = route
							break
						}
					}

					// Make sure the selected route was found.
					if (selectedRoute == null) {
						throw RouteException("Unable to determine selected route!")
					}

					// Updated the selected route's boolean.
					selectedRoute.enabled = enabled

					// Comment
					if (this.viewModel.map == null) {
						return super.onOptionsItemSelected(item)
					}

					// Try to (re)draw the buses onto the map.
					// Because we are iterating a static variable that is modified on a different thread
					// there is a possibility of a concurrent modification.
					try {
						this.viewModel.drawBuses()
					} catch (e: ConcurrentModificationException) {
						Log.e("onOptionsItemSelected",
						      "Unable to redraw all buses due to concurrent modification", e)
					}

					// (Re) draw the stops onto the map.
					this.viewModel.drawStops()

					// (Re) draw the routes onto the map (if enabled).
					if ((currentSettings.settingsImplementation as V2).polylines) {
						this.viewModel.drawRoutes()
					}

					// Set the menu item's checked value to that of the enabled value
					item.isChecked = enabled
				} catch (e: RouteException) {
					Toast.makeText(this, "An error occurred while toggling that route",
					               Toast.LENGTH_LONG).show()
					e.printStackTrace()
				}
			}

			else -> {
				// Since the item's ID and group was not part of anything accounted for (uh oh),
				// log it as a warning!
				Log.w("onOptionsItemSelected", "Unaccounted menu item was checked!")
			}
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onResume() {
		Log.v("onResume", "onResume has been called!")
		super.onResume()

		// Update the map's dynamic settings.
		this.viewModel.updateMapSettings()

		// Resume the coroutine
		if (this.viewModel.updater != null) {
			this.viewModel.runUpdater()
		}
	}

	override fun onPause() {
		Log.v("onPause", "onPause has been called!")
		super.onPause()

		// Stop the coroutine
		if (this.viewModel.updater != null) {
			this.viewModel.updater!!.run = false
		}
	}

	companion object {

		/**
		 * Create an array to store all the routes that we will track.
		 * This is not to say that all routes in this array are enabled - they can also be disabled (hidden).
		 * This array is initialized in DownloadMasterSchedule.
		 */
		// FIXME Memory leak (due to routes containing polylines)
		var allRoutes: Array<Route> = emptyArray() // TODO Check for concurrent exceptions

		var firstRun: Boolean = true

	}
}
