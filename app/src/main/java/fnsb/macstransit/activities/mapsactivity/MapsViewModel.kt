package fnsb.macstransit.activities.mapsactivity

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.ktx.awaitMap
import fnsb.macstransit.R
import fnsb.macstransit.activities.mapsactivity.mappopups.InfoWindowPopup
import fnsb.macstransit.activities.mapsactivity.mappopups.PopupWindow
import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.MarkedObject
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import fnsb.macstransit.routematch.Stop
import fnsb.macstransit.settings.V2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.NullPointerException
import java.util.ConcurrentModificationException
import kotlin.math.pow

/**
 * Created by Spud on 8/21/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.2.
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
	 * All of the routes that are trackable by the app.
	 */
	val routes: HashMap<String, Route> = HashMap()

	/**
	 * The RouteMatch object used to make calls to the RouteMatch server in order to update the bus positions,
	 * and to determine the arrival and departure times for stops.
	 */
	val routeMatch: RouteMatch = RouteMatch(getApplication<Application>().getString(R.string.routematch_url), getApplication())

	/**
	 * Create the map object. This will be null until the map is ready to be used.
	 */
	var map: GoogleMap? = null

	/**
	 * The update coroutine used to update the bus positions on the map.
	 */
	var updater: UpdateCoroutine? = null

	/**
	 * Draws the stops and shared stops onto the map,
	 * and adjusts the stop sizes based on the zoom level.
	 */
	fun drawStops() {

		// If the map is null at this point return early.
		if (map == null) {
			return
		}

		// Launch the toggle function on a coroutine to free up some of the work on the main thread.
		viewModelScope.launch(Dispatchers.Main) {

			// Iterate though all the routes as we know at this point that they are not null.
			for (route in routes.values) {

				// Toggle the stop visibility for each route.
				for (stop: Stop in route.stops.values) {
					stop.toggleStopVisibility(map!!, route.enabled)
				}

				// Iterate though the shared stops in the route.
				for (sharedStop: SharedStop in route.sharedStops.values) {
					if (route.enabled) {
						sharedStop.showSharedStop(map!!)
					} else {
						sharedStop.hideStop()
					}
				}
			}

			// Adjust the circle sizes of the stops on the map given the current zoom.
			resizeStops()
		}
	}

	/**
	 * Draws the route's polylines to the map.
	 * While all polylines are drawn they will only be shown if the route that they belong to is enabled.
	 */
	fun drawRoutes() {

		// If the map is null at this point return early.
		if (map == null) {
			return
		}

		// Toggle the polyline visibility for all the routes on the map.
		for (route: Route in routes.values) {
			route.togglePolylineVisibility(routeMatch, map!!)
		}
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

		// If the map is null at this point return early.
		if (map == null) {
			return
		}

		// Launch the update function on a coroutine to free up some of the work on the main thread.
		viewModelScope.launch(Dispatchers.Main) {

			// Start by iterating though all the buses on the map.
			for (bus: Bus in buses) {

				if (bus.marker != null) {

					// Set the bus marker visibility based on if the bus's route is enabled or not.
					bus.marker!!.isVisible = bus.route.enabled
				} else {
					if (bus.route.enabled) {

						// Try creating a new marker for the bus (if its enabled).
						try {
							bus.addMarker(map!!)
							bus.marker!!.isVisible = true // Marker may be null here if unsuccessful!
						} catch (nullPointerException: NullPointerException) {

							// Log that the marker was unable to be added to the map.
							Log.e("drawBuses", "Unable to add marker to map", nullPointerException)
						}
					} else {

						// If the marker was null simply log it as a warning.
						Log.w("drawBuses", "Bus doesn't have a marker for route ${bus.route.name}!")
					}
				}
			}
		}
	}

	/**
	 * Sets up the map by applying all the relevant listeners,
	 * and moving the map camera to the starting position.
	 *
	 * @param supportFragment The map component view in the activity.
	 */
	@SuppressLint("PotentialBehaviorOverride")
	@MainThread
	suspend fun setupMap(supportFragment: SupportMapFragment, activity: MapsActivity) {

		// Wait until the map object is ready.
		Log.v("MapCoroutine", "Awaiting map...")
		val map = supportFragment.awaitMap()

		// Move the camera to the 'home' position.
		Log.v("MapCoroutine", "Moving camera to home position")
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(64.8391975, -147.7684709), 11.0f))

		// Set the minimum zoom, maximum zoom, and bounds for the camera.
		map.setMinZoomPreference(10.0F)
		map.setMaxZoomPreference(18.5F)
		map.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(64.7164252379029, -148.05900312960148),
		                                                  LatLng(64.91343346034542, -147.3037289455533)))

		// Only execute the following if we have routes to iterate over.
		if (routes.isNotEmpty()) {

			// Add a listener for when the camera has become idle (ie was moving isn't anymore).
			Log.v("MapCoroutine", "Setting camera idle listener")
			map.setOnCameraIdleListener {

				// Resize the stops and shared stops.
				this.resizeStops()

				// Log the camera information.
				val cameraPosition: CameraPosition = map.cameraPosition
				Log.v("OnCameraIdle", "Bearing: ${cameraPosition.bearing}")
				Log.v("OnCameraIdle", "Target: ${cameraPosition.target}")
				Log.v("OnCameraIdle", "Tilt: ${cameraPosition.tilt}")
				Log.v("OnCameraIdle", "Zoom: ${cameraPosition.zoom}")
			}

			// Add a custom info window adapter, to add support for multiline snippets.
			Log.v("MapCoroutine", "Setting info window")
			map.setInfoWindowAdapter(InfoWindowPopup(activity))

			// Set it so that if the info window was closed for a Stop marker,
			// make that marker invisible, so its just the dot.
			Log.v("MapCoroutine", "Setting info window close listener")
			map.setOnInfoWindowCloseListener {

				// Get the tag as a marked object for easier lookup.
				val potentialStop: MarkedObject = it.tag as MarkedObject

				// Check if it was a stop info window that was closed.
				if (potentialStop is Stop || potentialStop is SharedStop) {

					// Cancel the network request.
					routeMatch.networkQueue.cancelAll(it)

					// Just hide the marker, since we don't want to destroy it just yet.
					it.isVisible = false
				} else {

					// Log that the info window that was closed was neither a Stop nor a SharedStop.
					Log.w("onInfoWindowClose", "Unhandled info window")
				}
			}

			// Set it so that when an info window is clicked on, it launches a popup window
			Log.v("MapCoroutine", "Setting info window click listener")
			map.setOnInfoWindowClickListener(PopupWindow(activity))

			this.map = map
			Log.v("MapCoroutine", "Map has been set")

			// Add a listener for when a stop icon (circle) is clicked.
			Log.v("MapCoroutine", "Setting circle click listener")
			map.setOnCircleClickListener(StopClicked(activity, routeMatch, this.map!!, routes))

			// Set the update coroutine to update every 10 seconds.
			Log.v("MapCoroutine", "Launching update coroutine")
			updater = UpdateCoroutine(10000, this)
		}

		// Update the map's dynamic settings.
		Log.v("MapCoroutine", "Updating map settings")
		updateMapSettings()

	}

	/**
	 * Updates the various settings on the map object determined by the settings file.
	 * It also redraws the buses and stops that are active on the map, and draws the polylines if they are enabled.
	 * This should be called when the map has been setup and is ready to be refreshed.
	 */
	fun updateMapSettings() {

		// Make sure to only execute the following if the maps object is not null (map has been setup).
		if (map != null) {

			// Get the current settings values.
			val settings = fnsb.macstransit.settings.CurrentSettings.settingsImplementation as V2

			// Enable traffic overlay based on settings.
			map!!.isTrafficEnabled = settings.traffic

			// Set the the type of map based on settings.
			map!!.mapType = settings.maptype

			// Toggle night mode at this time if enabled.
			toggleNightMode(map!!, getApplication(), settings.darktheme)

			if (MapsActivity.firstRun) {

				// Get the favorited routes from the settings object.
				// If the favorited routes is not null, enable them.
				Route.enableFavoriteRoutes(routes, settings.favoriteRouteNames)

				MapsActivity.firstRun = false
			}

			// Try redrawing the buses.
			// Because we are iterating a static variable that is modified on a different thread
			// there is a possibility of a concurrent modification.
			try {
				drawBuses()
			} catch (e: ConcurrentModificationException) {
				Log.e("updateMapSettings", "Unable to draw all buses due to concurrent modification", e)
			}

			// Draw the stops.
			drawStops()

			// Draw the routes.
			if (settings.polylines) {
				drawRoutes()
			}
		} else {
			Log.w("updateMapSettings", "Map is not yet ready!")
		}
	}

	/**
	 * Runs the update coroutine.
	 */
	fun runUpdater() {

		if (updater == null) {
			return
		}

		// Set the update coroutine to run.
		updater!!.run = true

		// If the coroutine isn't running then start it.
		if (!updater!!.isRunning) {
			viewModelScope.launch(Dispatchers.IO) { updater!!.start() }
		}
	}

	/**
	 * Resizes the stop and shared stop circles on the map.
	 * This works regardless of whether or not a particular route is enabled or disabled.
	 */
	private fun resizeStops() {

		// If the map is null at this point return early.
		if (map == null) {
			return
		}

		// Calculate meters per pixel.
		// This will be used to determine the circle size as we want it it be 4 meters in size.
		// To calculate this we will need the current zoom as well as the cameras latitude.
		val zoom = map!!.cameraPosition.zoom
		val lat = map!!.cameraPosition.target.latitude

		// With the zoom and latitude determined we can then calculate meters per pixel.
		val metersPerPixel = 156543.03392 * kotlin.math.cos(lat * Math.PI / 180.0) / 2.0.pow(zoom.toDouble())

		// Get the size of the circle to resize to.
		val size = metersPerPixel * 4

		// Iterate though each route.
		for (route: Route in routes.values) {

			// Start by resizing the stop circles first.
			for (stop: Stop in route.stops.values) {
				if (stop.circle != null) {
					stop.circle!!.radius = size
				}
			}

			// Then resize the route's shared stop circles.
			for (sharedStop: SharedStop in route.sharedStops.values) {
				sharedStop.setCircleSizes(size)
			}
		}
	}

	companion object {

		/**
		 * Toggles the map's night mode (dark theme).
		 *
		 * @param enabled Whether to toggle the maps night mode
		 */
		fun toggleNightMode(map: GoogleMap, context: Context, enabled: Boolean) {

			// Set the map style to the appropriate resource determined by the provided boolean.
			val style: MapStyleOptions = if (enabled) {
				MapStyleOptions.loadRawResourceStyle(context, R.raw.nightmode)
			} else{
				MapStyleOptions.loadRawResourceStyle(context, R.raw.standard)
			}

			map.setMapStyle(style)
		}
	}
}