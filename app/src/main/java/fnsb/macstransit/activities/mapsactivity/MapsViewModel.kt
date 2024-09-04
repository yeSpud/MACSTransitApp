package fnsb.macstransit.activities.mapsactivity

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.orhanobut.dialogplus.OnDismissListener
import fnsb.macstransit.R
import fnsb.macstransit.activities.LoadedRoutes
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import fnsb.macstransit.routematch.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.ConcurrentModificationException
import java.util.Locale
import kotlin.math.pow

/**
 * Created by Spud on 8/21/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.2.
 * @since Release 1.3.
 */
class MapsViewModel(application: Application): androidx.lifecycle.AndroidViewModel(application) {

	/**
	 * Create an array of all the buses that will be used
	 * (either shown or hidden) on the map at any given time.
	 * For now just initialize this array to 0.
	 */
	var buses: Array<fnsb.macstransit.routematch.Bus> = emptyArray()

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
	 * The stop that has been selected and should show a popup window.
	 */
	private var selectedStop: com.google.android.gms.maps.model.Marker? = null

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
			for (route in LoadedRoutes.routes.values) {

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
		for (route: Route in LoadedRoutes.routes.values) {
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
		if (map == null) { return }

		// Launch the update function on a coroutine to free up some of the work on the main thread.
		viewModelScope.launch(Dispatchers.Main) {

			// Start by iterating though all the buses on the map.
			for (bus in buses) {
				bus.marker.isVisible = bus.route.enabled
			}
		}
	}

	/**
	 * Sets up the map by applying all the relevant listeners,
	 * and moving the map camera to the starting position.
	 *
	 * @param supportFragment The map component view in the activity.
	 */
	@android.annotation.SuppressLint("PotentialBehaviorOverride")
	@androidx.annotation.UiThread
	suspend fun setupMap(supportFragment: com.google.android.gms.maps.SupportMapFragment,
	                     activity: MapsActivity) {

		// Wait until the map object is ready.
		Log.v("MapCoroutine", "Awaiting map...")
		val map = supportFragment.awaitMap()

		// Move the camera to the 'home' position.
		Log.v("MapCoroutine", "Moving camera to home position")
		map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
				LatLng(64.8391975, -147.7684709), 11.0f))

		// Set the minimum zoom, maximum zoom, and bounds for the camera.
		map.setMinZoomPreference(10.0F)
		map.setMaxZoomPreference(18.5F)
		map.setLatLngBoundsForCameraTarget(
				com.google.android.gms.maps.model.LatLngBounds(LatLng(64.7164252379029,
				                                                      -148.05900312960148),
				             LatLng(64.91343346034542, -147.3037289455533)))

		// Only execute the following if we have routes to iterate over.
		if (LoadedRoutes.routes.isNotEmpty()) {

			// Add a listener for when the camera has become idle (ie was moving isn't anymore).
			Log.v("MapCoroutine", "Setting camera idle listener")
			map.setOnCameraIdleListener {

				// Resize the stops and shared stops.
				resizeStops()

				if (fnsb.macstransit.BuildConfig.DEBUG) {
					val cameraPosition: com.google.android.gms.maps.model.CameraPosition = map.cameraPosition
					Log.v("OnCameraIdle", "Bearing: ${cameraPosition.bearing}")
					Log.v("OnCameraIdle", "Target: ${cameraPosition.target}")
					Log.v("OnCameraIdle", "Tilt: ${cameraPosition.tilt}")
					Log.v("OnCameraIdle", "Zoom: ${cameraPosition.zoom}")
				}
			}

			// Add a custom info window adapter, to add support for multiline snippets.
			Log.v("MapCoroutine", "Setting info window")
			map.setInfoWindowAdapter(fnsb.macstransit.activities.mapsactivity.mappopups
								.InfoWindowPopup(activity)) // For now this is kept for buses

			// Set it so that if the info window was closed for a Stop marker,
			// make that marker invisible, so its just the dot.
			/*
			Log.v("MapCoroutine", "Setting info window close listener")
			map.setOnInfoWindowCloseListener {
				if (selectedStop != null) {
					selectedStop!!.isVisible = false
				}
			}*/

			// Set it so that when an info window is clicked on, it launches a popup window
			Log.v("MapCoroutine", "Setting info window click listener")
			map.setOnInfoWindowClickListener(fnsb.macstransit.activities.mapsactivity.mappopups
										.PopupWindow(activity))

			// Add a listener for when a stop icon (circle) is clicked.
			Log.v("MapCoroutine", "Setting circle click listener")
			map.setOnCircleClickListener { circle: com.google.android.gms.maps.model.Circle ->

				var stopRoutes: Array<Route> = emptyArray()

				if (circle.tag is Stop) {
					val stop = circle.tag as Stop

					if (selectedStop == null) {
						selectedStop = this.map!!.addMarker {
							title(stop.name)
							position(stop.location)
							icon(getMarkerColor(stop.route.color))
						}
					} else {
						selectedStop!!.title = stop.name
						selectedStop!!.position = stop.location
						selectedStop!!.setIcon(getMarkerColor(stop.route.color))
					}

					stopRoutes = arrayOf(stop.route)
				}

				if (circle.tag is SharedStop) {
					val sharedStop = circle.tag as SharedStop
					// TODO

					stopRoutes = getEnabledRoutesForStop(sharedStop)
				}

				if (circle.tag !is Stop && circle.tag !is SharedStop) {
					Log.w("onCircleClick", "Unknown class:" + circle.tag!!.javaClass)
					return@setOnCircleClickListener
				}

				selectedStop!!.isVisible = true

				val stopDialog = com.orhanobut.dialogplus.DialogPlus.newDialog(activity)
					.setAdapter(fnsb.macstransit.activities.mapsactivity.mappopups.
					StopDialog(activity, selectedStop!!.title!!, stopRoutes))
					.setExpanded(true).setOnDismissListener {
						if (selectedStop != null) {
							selectedStop!!.isVisible = false
						}
					}.create()

				stopDialog.show()
			}

			this.map = map
			Log.v("MapCoroutine", "Map has been set")

			// Set the update coroutine to update every 10 seconds.
			Log.v("MapCoroutine", "Launching update coroutine")
			updater = UpdateCoroutine(10000, this)
		}

		// Update the map's dynamic settings.
		Log.v("MapCoroutine", "Updating map settings")
		updateMapSettings()
	}

	/**
	 * Returns an array of routes that are enabled from all the routes in the shared stop.
	 *
	 * @param sharedStop The shared stop to get the times for.
	 * @return The routes in the shared stop that are enabled.
	 */
	private fun getEnabledRoutesForStop(sharedStop: SharedStop): Array<Route> {

		// Create a new routes array to store routes that have been verified to be enabled.
		val potentialRoutes = arrayOfNulls<Route>(sharedStop.routes.size)
		var routeCount = 0

		for (stopRoute in sharedStop.routes) {

			// Try to get the route with the shared stop route name from the hashmap of routes.
			val route: Route = try {
				LoadedRoutes.routes[stopRoute.name]!!
			} catch (nullPointerException: NullPointerException) {
				Log.e("getEnabledRoutes",
				      "Route for shared stop ${sharedStop.name} is invalid: ${stopRoute.name}}",
				      nullPointerException)
				continue
			}

			// Since the route was found only add it to the route count if the potentials if its enabled.
			if (route.enabled) {
				potentialRoutes[routeCount] = route
				routeCount++
			}
		}

		// Create a new routes array of selected routes that has the size of our verified count.
		val selectedRoutes = arrayOfNulls<Route>(routeCount)

		// Fill the selected routes array.
		System.arraycopy(potentialRoutes, 0, selectedRoutes, 0, routeCount)

		// Return our selected routes.
		@Suppress("UNCHECKED_CAST") // Suppressed because we are asserting that none of the routes are null
		return selectedRoutes as Array<Route>
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
			val settings = fnsb.macstransit.settings.CurrentSettings.settingsImplementation as fnsb.macstransit.settings.V2

			// Enable traffic overlay based on settings.
			map!!.isTrafficEnabled = settings.traffic

			// Set the the type of map based on settings.
			map!!.mapType = settings.maptype

			// Toggle night mode at this time if enabled.
			toggleNightMode(map!!, getApplication(), settings.darktheme)

			if (MapsActivity.firstRun) {

				// Get the favorited routes from the settings object.
				// If the favorited routes is not null, enable them.
				Route.enableFavoriteRoutes(LoadedRoutes.routes, settings.favoriteRouteNames)

				MapsActivity.firstRun = false
			}

			// Try redrawing the buses.
			// Because we are iterating a static variable that is modified on a different thread
			// there is a possibility of a concurrent modification.
			try {
				drawBuses()
			} catch (e: ConcurrentModificationException) {
				Log.e("updateMapSettings",
				      "Unable to draw all buses due to concurrent modification", e)
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
		for (route: Route in LoadedRoutes.routes.values) {

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
		fun toggleNightMode(map: GoogleMap, context: android.content.Context, enabled: Boolean) {

			// Set the map style to the appropriate resource determined by the provided boolean.
			val style: MapStyleOptions = if (enabled) {
				MapStyleOptions.loadRawResourceStyle(context, R.raw.nightmode)
			} else {
				MapStyleOptions.loadRawResourceStyle(context, R.raw.standard)
			}

			map.setMapStyle(style)
		}

		internal fun getMarkerColor(color: Int): com.google.android.gms.maps.model.BitmapDescriptor {
			val hsv = FloatArray(3)
			android.graphics.Color.colorToHSV(color, hsv)
			return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0])
		}

		/**
		 * Gets the the time (predicted arrival or predicted departure depending on the key)
		 * for the stop via its JSONObject.
		 *
		 * @param json The JSONObject containing the time for the stop.
		 * @param key  The specific key to search for within the JSONObject.
		 * @return The time found within the JSONObject.
		 */
		fun getTime(json: org.json.JSONObject, key: String): String {

			// Try to get the time string from the json object based on the key.
			val timeString: String = try {
				json.getString(key)
			} catch (e: org.json.JSONException) {

				// Try to manage the exception, as it may be thrown if the value is actually null.
				val message = e.message
				if (message != null) {
					if (message == "JSONObject[\"$key\"] is not a string.") {
						Log.w("getTime", "$key has the wrong type (not a string - probably null)")

						// Because the string was probably "Null", return an empty string.
						return ""
					}
				}

				// Log any errors and return empty if unsuccessful.
				Log.e("getTime", "Unable to get stop times.", e)
				return ""
			}

			// Get a matcher object from the time regex (example: 00:00), and have it match the key.
			val matcher = java.util.regex.Pattern.compile("\\d\\d:\\d\\d").matcher(timeString)

			// If the match was found, return it, if not return midnight.
			return if (matcher.find()) {
				try {
					matcher.group(0)!!
				} catch (nullPointerException: NullPointerException) {
					Log.e("getTime", "Regex returned null!", nullPointerException)
					""
				}
			} else {
				""
			}
		}

		/**
		 * Formats a given 24 hour time string into a 12 hour time string complete with AM/PM characters.
		 *
		 * @param time The time string (ie 13:15 for what should be 1:15 PM).
		 * @return The formatted time string with the AM/PM characters included.
		 */
		fun formatTime(time: String): String {

			// Create a date format for parsing 24 hour time.
			val fullTime = SimpleDateFormat("H:mm", Locale.US)

			// Create another date format for formatting 12 hour time.
			val halfTime = SimpleDateFormat("h:mm a", Locale.US)

			// Get the time in 24 hours.
			val fullTimeDate: java.util.Date = try {

				// Try to get the 24 hour time as a date.
				fullTime.parse(time)!!
			} catch (exception: Exception) {

				// If there was an exception raised during parsing simply return the parameter.
				Log.e("formatTime", "Could not parse full 24 hour time", exception)
				return time
			}

			// Format the 24 hour time date into 12 hour time and return it.
			val formattedTime = halfTime.format(fullTimeDate)
			Log.d("formatTime", "Formatted time: $formattedTime")
			return formattedTime
		}
	}
}