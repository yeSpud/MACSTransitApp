package fnsb.macstransit.activities.mapsactivity

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.ktx.awaitMap
import fnsb.macstransit.R
import fnsb.macstransit.activities.mapsactivity.maplisteners.AdjustZoom
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.settings.V2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ConcurrentModificationException

/**
 * Created by Spud on 8/21/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class MapsViewModel(application: Application) : AndroidViewModel(application) {

	/**
	 * Create an array of all the buses that will be used
	 * (either shown or hidden) on the map at any given time.
	 * For now just initialize this array to 0.
	 */
	var buses: Array<Bus> = emptyArray()

	/**
	 * Documentation
	 */
	val routeMatch: RouteMatch = RouteMatch(this.getApplication<Application>().getString(R.string.routematch_url),
	                                        this.getApplication())

	/**
	 * Documentation
	 */
	private var loadedRouteMatch: Boolean = false

	/**
	 * Create the map object. This will be null until the map is ready to be used.
	 * Deprecated because this leaks memory in the static form. Use as dependency injection.
	 */
	var map: GoogleMap? = null

	/**
	 * Documentation
	 */
	var updater: UpdateCoroutine? = null

	/**
	 * Draws the stops and shared stops onto the map,
	 * and adjusts the stop sizes based on the zoom level.
	 */
	fun drawStops() {

		// Comments
		if (this.map == null) {
			return
		}

		// Comments
		viewModelScope.launch(Dispatchers.Main) {

			// Iterate though all the routes as we know at this point that they are not null.
			for (route in MapsActivity.allRoutes) {

				// Toggle the stop visibility for each route.
				route.stops.forEach { it.toggleStopVisibility(this@MapsViewModel.map!!) }

				// Iterate though the shared stops in the route.
				for (sharedStop in route.sharedStops) {
					if (route.enabled) {

						// Show the shared stops.
						sharedStop.showSharedStop(this@MapsViewModel.map!!)
					} else {

						// Hide the shared stops on the map.
						// Note that the stops should be hidden - not destroyed.
						sharedStop.hideStop()
					}
				}
			}

			// Adjust the circle sizes of the stops on the map given the current zoom.
			AdjustZoom.resizeStops(this@MapsViewModel.map!!)

		}
	}

	/**
	 * Draws the route's polylines to the map.
	 * While all polylines are drawn they will only be shown if the route that they belong to is enabled.
	 */
	fun drawRoutes() {

		// Comments
		if (this.map == null) {
			return
		}

		// Toggle the polyline visibility for all the routes on the map.
		MapsActivity.allRoutes.forEach { it.togglePolylineVisibility(this.map!!) }

	}

	/**
	 * (Re)draws the buses on the map.
	 * While all buses are drawn they will only be shown if the route that they belong to is enabled.
	 *
	 * @throws ConcurrentModificationException Concurrent exception may be thrown
	 * as it iterates through the bus list,
	 * which may be modified at the time of iteration.
	 */
	@Throws(ConcurrentModificationException::class)
	fun drawBuses() {

		// Comments
		if (this.map == null) {
			return
		}

		// Comments
		viewModelScope.launch(Dispatchers.Main) {

			// Start by iterating though all the buses on the map.
			for (bus in this@MapsViewModel.buses) {

				// Comments
				val route = bus.route
				if (bus.marker != null) {

					// Set the bus marker visibility based on if the bus's route is enabled or not.
					bus.marker!!.isVisible = route.enabled
				} else {
					if (route.enabled) {

						// Try creating a new marker for the bus (if its enabled).
						bus.addMarker(this@MapsViewModel.map!!, bus.color)
						bus.marker!!.isVisible = true

					} else {

						// If the marker was null simply log it as a warning.
						Log.w("drawBuses", "Bus doesn't have a marker for route ${route.routeName}!")
					}
				}
			}
		}
	}

	/**
	 * Documentation
	 * @param supportFragment
	 */
	suspend fun mapCoroutine(supportFragment: SupportMapFragment) {

		// Comments
		Log.v("MapCoroutine", "Awaiting for map...")
		this.map = supportFragment.awaitMap()
		Log.v("MapCoroutine", "Map has been set")

		// Move the camera to the 'home' position
		Log.v("MapCoroutine", "Moving camera to home position")
		this.map!!.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.
		newLatLngZoom(com.google.android.gms.maps.model.LatLng(64.8391975, -147.7684709), 11.0f))

		// Only execute the following if we have routes to iterate over.
		if (MapsActivity.allRoutes.isNotEmpty()) {

			// Add a listener for when the camera has become idle (ie was moving isn't anymore).
			Log.v("MapCoroutine", "Setting camera idle listener")
			this.map!!.setOnCameraIdleListener(AdjustZoom(this.map!!))

			// Comments
			val activity: Context = this.getApplication<Application>().applicationContext as Context

			// Add a listener for when a stop icon (circle) is clicked.
			Log.v("MapCoroutine", "Setting circle click listener")
			this.map!!.setOnCircleClickListener(fnsb.macstransit.activities.mapsactivity.
			maplisteners.StopClicked(activity, this.routeMatch, this.map!!))

			// Add a custom info window adapter, to add support for multiline snippets.
			Log.v("MapCoroutine", "Setting info window")
			this.map!!.setInfoWindowAdapter(fnsb.macstransit.activities.mapsactivity.mappopups.
			InfoWindowPopup(activity))

			// Set it so that if the info window was closed for a Stop marker,
			// make that marker invisible, so its just the dot.
			Log.v("MapCoroutine", "Setting info window close listener")
			this.map!!.setOnInfoWindowCloseListener(fnsb.macstransit.activities.
			mapsactivity.maplisteners.StopDeselected(this.routeMatch.networkQueue))

			// Set it so that when an info window is clicked on, it launches a popup window
			Log.v("MapCoroutine", "Setting info window click listener")
			this.map!!.setOnInfoWindowClickListener(fnsb.macstransit.activities.mapsactivity.mappopups.
			PopupWindow(activity))

			// Comments
			Log.v("MapCoroutine", "Launching update coroutine")
			this.updater = UpdateCoroutine(10000, this, this.map!!)
			this.runUpdater()
		}

		// Update the map's dynamic settings.
		Log.v("MapCoroutine", "Updating map settings")
		this.updateMapSettings()

	}

	/**
	 * Updates the various settings on the map object determined by the settings file.
	 * It also redraws the buses and stops that are active on the map, and draws the polylines if they are enabled.
	 * This should be called when the map has been setup and is ready to be refreshed.
	 */
	fun updateMapSettings() {

		// Make sure to only execute the following if the maps object is not null (map has been setup).
		if (this.map != null) {

			// Comments
			val settings = fnsb.macstransit.settings.CurrentSettings.settingsImplementation as V2

			// Enable traffic overlay based on settings.
			this.map!!.isTrafficEnabled = settings.traffic

			// Set the the type of map based on settings.
			this.map!!.mapType = settings.maptype

			// Toggle night mode at this time if enabled.
			this.toggleNightMode(settings.darktheme)

			if (MapsActivity.firstRun) {

				// Get the favorited routes from the settings object.
				val favoritedRoutes = settings.routes

				// If the favorited routes is not null, enable them.
				Route.enableFavoriteRoutes(favoritedRoutes)

				MapsActivity.firstRun = false
			}

			// TODO Update initial checkbox state if not first run for each route

			// Try redrawing the buses.
			// Because we are iterating a static variable that is modified on a different thread
			// there is a possibility of a concurrent modification.
			try {
				this.drawBuses()
			} catch (e: ConcurrentModificationException) {
				Log.e("updateMapSettings",
				      "Unable to draw all buses due to concurrent modification", e)
			}

			// Draw the stops.
			this.drawStops()

			// Draw the routes.
			if (settings.polylines) {
				this.drawRoutes()
			}
		} else {
			Log.w("updateMapSettings", "Map is not yet ready!")
		}
	}

	/**
	 * Documentation
	 * Comments
	 */
	fun runUpdater() {
		this.updater!!.run = true
		if (!this.updater!!.isRunning) {
			this.viewModelScope.launch(Dispatchers.Main) {
				this@MapsViewModel.updater!!.start()
			}
		}
	}

	/**
	 * Toggles the map's night mode (dark theme).
	 *
	 * @param enabled Whether to toggle the maps night mode
	 */
	fun toggleNightMode(enabled: Boolean) {
		if (this.map != null) {

			// Comments
			val activity: Context = this.getApplication<Application>().applicationContext as Context

			// Comments
			this.map!!.setMapStyle(
					if (enabled) MapStyleOptions.loadRawResourceStyle(activity, R.raw.nightmode)
					else MapStyleOptions.loadRawResourceStyle(activity, R.raw.standard))
		} else {
			Log.w("toggleNightMode", "Map is not yet ready")
		}
	}

}