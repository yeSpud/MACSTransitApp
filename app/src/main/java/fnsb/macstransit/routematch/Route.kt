package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.ktx.addPolyline
import org.json.JSONException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.0.
 * @since Beta 3.
 */
class Route : java.io.Serializable {

	/**
	 * Documentation
	 */
	val name: String

	/**
	 * Documentation
	 */
	var color: Int = 0

	/**
	 * The name of the route formatted to be parsed as a URL.
	 */
	val urlFormattedName: String

	/**
	 * Documentation
	 */
	val stops: HashMap<String, Stop> = HashMap()

	/**
	 * Whether or not the route is enabled or disabled (to be shown or hidden).
	 * Default is false (disabled).
	 */
	var enabled = false

	/**
	 * The array of LatLng coordinates that will be used to create the polyline (if enabled).
	 * This is private as we don't want this variable to be set outside the class.
	 */
	var polyLineCoordinates: Array<com.google.android.gms.maps.model.LatLng> = emptyArray()

	/**
	 * Documentation
	 */
	val sharedStops: HashMap<String, SharedStop> = HashMap()

	/**
	 * The polyline that corresponds to this route.
	 */
	private var polyline: com.google.android.gms.maps.model.Polyline? = null

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does **NOT** contain any whitespace characters!
	 *
	 * @param routeName The name of the route. Be sure this does **NOT**
	 * contain any whitespace characters!
	 * @throws UnsupportedEncodingException Thrown if the route name cannot be formatted to a URL.
	 */
	@Throws(UnsupportedEncodingException::class)
	constructor(routeName: String) : this(routeName, 0)

	/**
	 * Documentation
	 *
	 * @param name
	 * @param color
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	constructor(name: String, color: Int) {

		// Comments
		if (name.contains(Regex("\\s"))) {
			throw UnsupportedEncodingException("Route name contains whitespace!")
		}

		// Comments
		this.name = name
		this.color = color

		// Comments
		this.urlFormattedName = Pattern.compile("\\+").matcher(URLEncoder.
		encode(this.name, "UTF-8")).replaceAll("%20")
	}

	/**
	 * Creates and sets the polyline for the route.
	 * If there are no polyline coordinates for the route then this simply returns early and does not create the polyline.
	 *
	 * This must be run on the UI Thread.
	 *
	 * @param map The map to add the polyline to.
	 */
	@UiThread
	fun createPolyline(map: GoogleMap) {
		Log.v("createPolyline", "Creating route polyline")

		// Make sure the polyline coordinates is not null or 0. If it is then return early.
		if (this.polyLineCoordinates.isEmpty()) {
			Log.w("createPolyline", "There are no polyline coordinates to work with!")
			return
		}

		// Add the polyline to the map with the following options:
		this.polyline = map.addPolyline {

			// Create new polyline options from the array of polyline coordinates stored for the route.
			this.add(*this@Route.polyLineCoordinates)

			// Make sure its not clickable.
			this.clickable(false)

			// Set the color of the polylines based on the route color.
			this.color(this@Route.color)

			// Set the polyline visibility to whether or not the route is enabled.
			this.visible(this@Route.enabled)
		}
	}

	/**
	 * Removes stops that have shared stops.
	 */
	fun purgeStops() {

		// Iterate though each shared stop.
		this.sharedStops.forEach {

			// Try to remove stops by shared stop name.
			if (this.stops.remove(it.key) != null) {

				// If the value for removed wasn't null then log the stop for debugging.
				Log.d("purgeStops", "Stop removed: ${it.key}")
			}
		}
	}

	/**
	 * Sets the polylines visibly to whether or not the route is enabled or not.
	 * If the polyline didn't previously exist it will be created in this method.
	 *
	 * THis must be run on the UI Thread.
	 *
	 * @param map The map to add the polyline to.
	 * @param attempted Whether or not this method has been run (for recursion). Default is false.
	 */
	@UiThread
	fun togglePolylineVisibility(map: GoogleMap, attempted: Boolean = false) {

		// Check if the route has a polyline to set visible.
		if (this.polyline == null) {

			// Check to make sure we haven't already attempted to load the polyline.
			if (attempted) {

				// Since we already have attempted to create the polyline simply return early with a warning.
				Log.w("enablePolyline", "Could not load polyline for route ${this.name}")
				return
			}

			// Create a new polyline for the route since it didn't have one before.
			this.createPolyline(map)
			this.togglePolylineVisibility(map, true)
		} else {

			// Set the polyline's visibility to whether the route is enabled or not
			Log.d("togglePolyline", "Setting route ${this.name} to visible: ${this.enabled}")
			this.polyline!!.isVisible = this.enabled
		}
	}

	/**
	 * Removes the routes polyline from the map, and sets it to null.
	 *
	 * This must be run on the UI thread.
	 */
	@UiThread
	fun removePolyline() {
		if (polyline != null) {
			polyline!!.remove()
			polyline = null
		}
	}

	companion object {

		/**
		 * Creates a new route object from the provided json object.
		 * If the json object is null then a RouteException will be thrown.
		 *
		 * @param jsonObject The json object contain the data to create a new route object.
		 * @return The newly created route object.
		 * @throws JSONException                Thrown if the json object is null,
		 *                                      or if the route name is unable to be parsed.
		 * @throws UnsupportedEncodingException Thrown if the route name cannot be formatted to a URL.
		 */
		@JvmStatic
		@Throws(JSONException::class, UnsupportedEncodingException::class)
		fun generateRoute(jsonObject: org.json.JSONObject): Route {

			// First, parse the name.
			val name = jsonObject.getString("routeId")

			return try {

				// Now try to parse the route color.
				val colorName = jsonObject.getString("routeColor")
				val color = android.graphics.Color.parseColor(colorName)

				// Return our newly created route with color!
				Route(name, color)
			} catch (Exception: Exception) {
				Log.w("generateRoute", "Unable to parse color", Exception)

				// Since there was an issue parsing the color,
				// and we have the name at this point simply create the route without a color.
				Route(name)
			}
		}

		/**
		 * Iterates though all the routes in MapsActivity.allRoutes
		 * and enables those that have been favorited (as determined by being in the favoritedRoutes array).
		 *
		 * @param favoritedRoutes The selected routes to be enabled from MapsActivity.allRoutes.
		 */
		@JvmStatic
		fun enableFavoriteRoutes(favoritedRoutes: Array<Route>) {

			// Iterate through all the routes that will be used in the activity.
			fnsb.macstransit.activities.mapsactivity.MapsActivity.allRoutes.forEach {

				// Iterate though the favorite routes.
				for (favoritedRoute: Route in favoritedRoutes) {

					// If the route name matches the favorited route name, enable it in all routes.
					if (it.name == favoritedRoute.name) {
						it.enabled = true
						break
					}
				}
			}
		}
	}
}

@Throws(ClassNotFoundException::class)
@Deprecated("Use a hashmap ya goof!")
operator fun Array<Route>.get(name: String): Route {

	// Iterate though the array.
	this.forEach {

		// If the name matches our name passed as a parameter then return that route.
		if (it.name == name) {
			return it
		}
	}

	// If we've made it this far that means the route was not found in the array, so throw a ClassNotFoundException.
	throw ClassNotFoundException("$name was not found in the array!")
}