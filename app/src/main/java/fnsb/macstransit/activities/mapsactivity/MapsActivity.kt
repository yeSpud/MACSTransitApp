package fnsb.macstransit.activities.mapsactivity

import android.content.Intent
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.settings.V2
import fnsb.macstransit.R
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.SupportMapFragment
import fnsb.macstransit.activities.SettingsActivity
import fnsb.macstransit.activities.mapsactivity.mappopups.FarePopupWindow
import fnsb.macstransit.databinding.ActivityMapsBinding
import fnsb.macstransit.settings.CurrentSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MapsActivity: androidx.fragment.app.FragmentActivity() {

	/**
	 * The view model for the maps activity.
	 * This is usually where all the large functions and additional properties are.
	 */
	lateinit var viewModel: MapsViewModel

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

		// Load in the current settings.
		try {
			CurrentSettings.loadSettings(this)
		} catch (e: org.json.JSONException) {

			// If there was an exception loading the settings simply log it and return.
			Log.e("onCreate", "Exception when loading settings", e)
			return
		}

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		val supportFragment: SupportMapFragment =
				(this.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

		// Launch the maps coroutine (which sets up the Google Map object).
		this.lifecycleScope.launchWhenCreated { this@MapsActivity.viewModel.mapCoroutine(supportFragment,
		                                                                                 this@MapsActivity) }

		// Setup the fares popup window.
		this.farePopupWindow = FarePopupWindow(this) // TODO Set this up in view model?
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
		this.viewModel.buses.forEach { it.removeMarker() }

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

		Log.v("onDestroy", "Finished onDestroy")
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		Log.v("onCreateOptionsMenu", "onCreateOptionsMenu has been called!")

		// Setup the inflater.
		this.menuInflater.inflate(R.menu.menu, menu)

		// Create the menu item that corresponds to the route object.
		for (i in allRoutes.indices) {

			// Make sure the item is checkable.
			menu.add(R.id.routes, i, Menu.NONE, allRoutes[i].name).isCheckable = true
		}

		// Return what ever the default behaviour would be when calling this method if it were not overridden.
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		Log.v("onPrepareOptionsMenu", "onPrepareOptionsMenu has been called!")

		// Iterate through all the routes that can be tracked (if allRoutes isn't null).
		for (i in allRoutes.indices) {

			// Determine whether or not the menu item should be checked before hand.
			val checked: Boolean = allRoutes[i].enabled

			// Set the menu item to be checked if the route it corresponds to is enabled.
			Log.d("onPrepareOptionsMenu", "Setting ${allRoutes[i].name} to be enabled: $checked")
			menu.findItem(i).isChecked = checked
		}

		// Check if night mode should be enabled by default, and set the checkbox to that value.
		menu.findItem(R.id.night_mode).isChecked = (CurrentSettings.settingsImplementation as V2).darktheme

		// Return what ever the default behaviour would be when calling this method if it were not overridden.
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

						// Create a boolean to store the resulting value of the menu item.
						val enabled = !item.isChecked

						// Toggle night mode
						this.viewModel.toggleNightMode(enabled)

						// Set the menu item's checked value to that of the enabled value.
						item.isChecked = enabled
					}

					// Check if the item that was selected was the settings button.
					// Launch the settings activity if it was.
					R.id.settings -> this.startActivity(Intent(this, SettingsActivity::class.java))

					// Check if the item that was selected was the fares button.
					R.id.fares -> this.farePopupWindow.showFarePopupWindow()

					// Since the item's ID was not part of anything accounted for (uh oh), log it as a warning!
					else -> Log.w("onOptionsItemSelected",
					              "Unaccounted menu item in the other group was checked!")
				}
			}

			// Check if the item that was selected belongs to the routes group.
			R.id.routes -> {

				// Create a boolean to store the resulting value of the menu item.
				val enabled = !item.isChecked

				// Updated the selected route's enabled boolean.
				allRoutes[item.itemId].enabled = enabled
				Log.d("onOptionsItemSelected", "Selected route ${allRoutes[item.itemId].name}")

				// If the map is null at this point just return early (skip redrawing).
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
				if ((CurrentSettings.settingsImplementation as V2).polylines) {
					this.viewModel.drawRoutes()
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
		 *
		 * Because this is a static member that stores items that take in contexts it is a potential memory leak,
		 * so an alternative is strongly recommend.
		 */
		@Deprecated("Memory leak")
		var allRoutes: Array<Route> = emptyArray()

		/**
		 * Used to determine if the MapsActivity has been run before in the app's lifecycle.
		 * This will be set to true coming out of SplashActivity
		 */
		var firstRun: Boolean = true

	}
}
