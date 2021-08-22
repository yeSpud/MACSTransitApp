package fnsb.macstransit.activities.mapsactivity

import com.google.android.gms.maps.GoogleMap
import fnsb.macstransit.routematch.Route.RouteException
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.settings.V2
import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.R
import com.google.android.gms.maps.SupportMapFragment
import android.widget.Toast
import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.MapStyleOptions
import fnsb.macstransit.activities.InfoWindowAdapter
import fnsb.macstransit.activities.PopupWindow
import fnsb.macstransit.activities.SettingsActivity
import fnsb.macstransit.databinding.ActivityMapsBinding
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import java.util.ConcurrentModificationException

class MapsActivity : androidx.fragment.app.FragmentActivity(), com.google.android.gms.maps.OnMapReadyCallback {

	/**
	 * Documentation
	 */
	lateinit var viewModel: MapsViewModel

	/**
	 * Create the map object. This will be null until the map is ready to be used.
	 * Deprecated because this leaks memory in the static form. Use as dependency injection.
	 */
	private var map: GoogleMap? = null

	/**
	 * Documentation
	 */
	private val currentSettings = fnsb.macstransit.settings.CurrentSettings

	/**
	 * Documentation
	 */
	private var updater: UpdateCoroutine? = null

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
			currentSettings.loadSettings(this)
		} catch (e: JSONException) {
			// If there was an exception loading the settings simply log it and return.
			Log.e("onCreate", "Exception when loading settings", e)
			return
		}

		// Set the map to null for now. It will be set when the callback is ready.
		this.map = null

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		(this.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
	}

	override fun onDestroy() {
		Log.v("onDestroy", "onDestroy called!")
		super.onDestroy()

		// Launch the following as a cleanup job (that way we can essentially multi-thread the onDestroy process)
		lifecycleScope.launch(Dispatchers.Main, start=CoroutineStart.UNDISPATCHED) {
			Log.i("onDestroy", "Beginning onDestroy cleanup coroutine...")

			// Iterate though each route to get access to its shared stops and regular stops.
			for (route in allRoutes) {

				// Iterate though each stop.
				Log.d("onDestroy", "Removing stop circles")
				for (stop in route.stops) {

					// Remove the stop's circle.
					stop.removeStopCircle()

					// Remove stop's marker.
					stop.removeMarker()
				}

				// Get the shared stops for the route.
				Log.d("onDestroy", "Removing shared stop circles")
				val sharedStops = route.sharedStops

				// Iterate though each shared stop.
				for (sharedStop in sharedStops) {

					// Remove each shared stop circles.
					sharedStop.removeSharedStopCircles()

					// Remove the shared stop's marker.
					sharedStop.removeMarker()
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

		// TODO Stop the update coroutine
		if (this.updater != null) {
			this.updater!!.run = false
		}

		// Be sure to clear the map.
		if (this.map != null) {
			this.map!!.clear()
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
			menuItem.isChecked = route.enabled
		}

		// Check if night mode should be enabled by default, and set the checkbox to that value.
		menu.findItem(R.id.night_mode).isChecked = (currentSettings.settingsImplementation as V2).darktheme

		// Return true, otherwise the menu wont be displayed.
		return true
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

						// Create a boolean to store the resulting value of the menu item
						val enabled = !item.isChecked

						// Toggle night mode
						toggleNightMode(enabled)

						// Set the menu item's checked value to that of the enabled value
						item.isChecked = enabled
					}

					// Check if the item that was selected was the settings button.
					R.id.settings -> {

						// Launch the settings activity
						this.startActivity(Intent(this, SettingsActivity::class.java))
					}

					// Check if the item that was selected was the fares button.
					R.id.fares -> {
						this.viewModel.farePopupWindow.showFarePopupWindow()
					}
					else -> {

						// Since the item's ID was not part of anything accounted for (uh oh), log it as a warning!
						Log.w("onOptionsItemSelected", "Unaccounted menu item in the other group was checked!")
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
					if (this.map == null) {
						return super.onOptionsItemSelected(item)
					}

					// Try to (re)draw the buses onto the map.
					// Because we are iterating a static variable that is modified on a different thread
					// there is a possibility of a concurrent modification.
					try {
						this.viewModel.drawBuses(this.map!!)
					} catch (e: ConcurrentModificationException) {
						Log.e("onOptionsItemSelected",
						      "Unable to redraw all buses due to concurrent modification", e)
					}

					// (Re) draw the stops onto the map.
					this.viewModel.drawStops(this.map!!)

					// (Re) draw the routes onto the map (if enabled).
					if ((currentSettings.settingsImplementation as V2).polylines) {
						this.viewModel.drawRoutes(this.map!!)
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
		this.updateMapSettings()

		// Resume the coroutine
		if (this.updater != null) {
			this.runUpdater()
		}
	}

	override fun onPause() {
		Log.v("onPause", "onPause has been called!")
		super.onPause()

		// Stop the coroutine
		if (this.updater != null) {
			this.updater!!.run = false
		}
	}

	/**
	 * Manipulates the map once available. This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera.
	 * If Google Play services is not installed on the device,
	 * the user will be prompted to install it inside the SupportMapFragment.
	 * This method will only be triggered once the user has installed Google Play services and returned to the app.
	 */
	override fun onMapReady(googleMap: GoogleMap) {
		Log.v("onMapReady", "onMapReady has been called!")

		// Setup the map object at this point as it is finally initialized and ready.
		this.map = googleMap

		// Move the camera to the 'home' position
		this.map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(64.8391975, -147.7684709), 11.0f))

		// Add a listener for when the camera has become idle (ie was moving isn't anymore).
		this.map!!.setOnCameraIdleListener(AdjustZoom(map!!))

		// Add a listener for when a stop icon (circle) is clicked.
		this.map!!.setOnCircleClickListener(StopClicked(this, map!!))

		// Add a custom info window adapter, to add support for multiline snippets.
		this.map!!.setInfoWindowAdapter(InfoWindowAdapter(this))

		// Set it so that if the info window was closed for a Stop marker,
		// make that marker invisible, so its just the dot.
		this.map!!.setOnInfoWindowCloseListener(
				StopDeselected(this.viewModel.routeMatch.networkQueue))

		// Set it so that when an info window is clicked on, it launches a popup window
		this.map!!.setOnInfoWindowClickListener(PopupWindow(this))

		// Update the map's dynamic settings.
		this.updateMapSettings()

		// TODO Update coroutine
		this.updater = UpdateCoroutine(5000, this.viewModel, this.map!!)
		this.runUpdater()
	}

	/**
	 * Updates the various settings on the map object determined by the settings file.
	 * It also redraws the buses and stops that are active on the map, and draws the polylines if they are enabled.
	 * This should be called when the map has been setup and is ready to be refreshed.
	 */
	private fun updateMapSettings() {

		// Make sure to only execute the following if the maps object is not null (map has been setup).
		if (this.map != null) {

			// Comments
			val settings = currentSettings.settingsImplementation as V2

			// Enable traffic overlay based on settings.
			this.map!!.isTrafficEnabled = settings.traffic

			// Set the the type of map based on settings.
			this.map!!.mapType = settings.maptype

			// Toggle night mode at this time if enabled.
			toggleNightMode(settings.darktheme)

			// Get the favorited routes from the settings object.
			val favoritedRoutes = settings.routes
			if (!selectedFavorites) {

				// If the favorited routes is not null, enable them.
				Route.enableFavoriteRoutes(favoritedRoutes)
			}

			// Try redrawing the buses.
			// Because we are iterating a static variable that is modified on a different thread
			// there is a possibility of a concurrent modification.
			try {
				this.viewModel.drawBuses(this.map!!)
			} catch (e: ConcurrentModificationException) {
				Log.e("updateMapSettings",
				      "Unable to draw all buses due to concurrent modification", e)
			}

			// Draw the stops.
			this.viewModel.drawStops(this.map!!)

			// Draw the routes.
			if (settings.polylines) {
				this.viewModel.drawRoutes(this.map!!)
			}
		} else {
			Log.w("updateMapSettings", "Map is not yet ready!")
		}
	}

	/**
	 * Documentation
	 * Comments
	 */
	private fun runUpdater() {
		this.updater!!.run = true
		if (!this.updater!!.isRunning) {
			lifecycleScope.launch(Dispatchers.Main) {
				this@MapsActivity.updater!!.start()
			}
		}
	}

	/**
	 * Toggles the map's night mode (dark theme).
	 *
	 * @param enabled Whether to toggle the maps night mode
	 */
	private fun toggleNightMode(enabled: Boolean) {
		if (this.map != null) {
			this.map!!.setMapStyle(
					if (enabled) MapStyleOptions.loadRawResourceStyle(this, R.raw.nightmode)
					else MapStyleOptions.loadRawResourceStyle(this, R.raw.standard))
		} else {
			Log.w("toggleNightMode", "Map is not yet ready")
		}
	}

	companion object {

		/**
		 * Create an array to store all the routes that we will track.
		 * This is not to say that all routes in this array are enabled - they can also be disabled (hidden).
		 * This array is initialized in DownloadMasterSchedule.
		 */
		var allRoutes: Array<Route> = emptyArray() // TODO Check for concurrent exceptions

		/**
		 * Bool used to check if we have selected favorited routes from all routes.
		 * If this is set to true then we do not need to select favorite routes again as it should only be selected once.
		 */
		var selectedFavorites = false

	}
}