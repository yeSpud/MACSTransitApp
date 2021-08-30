package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.ktx.addPolyline
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
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
class Route(val routeName: String, color: Int) : Serializable {

	/**
	 * The name of the route formatted to be parsed as a URL.
	 */
	val urlFormattedName: String = Pattern.compile("\\+").matcher(URLEncoder.
	encode(this.routeName, "UTF-8")).replaceAll("%20") // TODO Add regex check

	/**
	 * The color of the route.
	 * This is optional, as there is a high chance that the route does not have one.
	 * This is an int instead of a Color object as android stores its colors as an integer.
	 */
	var color: Int
		private set

	/**
	 * The array of stops for this route.
	 * This may be empty / null if the route has not been initialized,
	 * and the stops haven't been loaded.
	 */
	var stops: Array<Stop> = emptyArray() // TODO Test length of 0 instead of null for iteration

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
	 * The array of shared stops for this route.
	 * This may be empty if the route has not been initialized,
	 * and the the shared stops haven't been loaded, or if there are no shared stops for the route.
	 */
	var sharedStops: Array<SharedStop> = emptyArray() // TODO Test length of 0 instead of null for iteration
		private set

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
	constructor(routeName: String) : this(routeName, 0)

	/**
	 * Creates and sets the polyline for the route.
	 * If there are no polyline coordinates for the route then this simply returns early and does not create the polyline.
	 *
	 * @param map TODO
	 */
	@UiThread
	fun createPolyline(map: GoogleMap) {
		Log.v("createPolyline", "Creating route polyline")

		// Make sure the polyline coordinates is not null or 0. If it is then return early.
		if (this.polyLineCoordinates.isEmpty()) {
			Log.w("createPolyline", "There are no polyline coordinates to work with!")
			return
		}

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
	 * Adds the shared to the routes shared stop array.
	 *
	 * @param sharedStop The shared stop to add to the route.
	 */
	fun addSharedStop(sharedStop: SharedStop) {

		if (this.sharedStops.isEmpty()) {

			// If there was no shared stop array before then simply set the array to just contain our shared stop.
			this.sharedStops = arrayOf(sharedStop)
			return
		} else {

			// Create a new shared stop array that will contain our current shared stop array + the new shared stop.
			// Since our current array of shared stops has content
			// simply insert our shared stop to the array and copy the rest using System.arraycopy.
			val newSharedStops: Array<SharedStop?> = arrayOfNulls(this.sharedStops.size + 1)
			newSharedStops[0] = sharedStop
			System.arraycopy(this.sharedStops, 0, newSharedStops, 1, sharedStops.size)

			// Set the routes shared stop array to the newly created shared stop array.
			this.sharedStops = newSharedStops.requireNoNulls()
		}
	}

	/**
	 * Documentation
	 * @param map
	 * @param attempted
	 */
	@UiThread
	fun togglePolylineVisibility(map: GoogleMap, attempted: Boolean = false) {

		// Check if the route has a polyline to set visible.
		if (this.polyline == null) {

			// Check to make sure we haven't already attempted to load the polyline.
			if (attempted) {
				Log.w("enablePolyline", "Could not load polyline for route ${this.routeName}")
				return
			}

			// Create a new polyline for the route since it didn't have one before.
			this.createPolyline(map)
			this.togglePolylineVisibility(map, true)
		} else {

			// Set the polyline's visibility to whether the route is enabled or not
			Log.d("tgglPolylineVisibility", "Setting route ${this.routeName} to visible:"
			                                  +"${this.enabled}")
			this.polyline!!.isVisible = this.enabled
		}
	}

	/**
	 * Removes the routes polyline from the map, and sets it to null.
	 * This must be run on the UI thread.
	 */
	@UiThread
	fun removePolyline() {
		if (polyline != null) {
			polyline!!.remove()
			polyline = null
		}
	}

	/**
	 * Exception class used for throwing any exception relating to Routes.
	 */
	class RouteException : Exception {
		/**
		 * Constructor for a new exception with a (hopefully detailed) message.
		 *
		 * @param message The (ideally detailed) message for why this was thrown.
		 */
		constructor(message: String?) : super(message)

		/**
		 * Constructor for a new exception with a (hopefully detailed) message, and a cause.
		 *
		 * @param message The (ideally detailed) message.
		 * @param cause   The cause for the exception. This may be null if the cause is undetermined.
		 */
		constructor(message: String?, cause: Throwable?) : super(message, cause)
	}

	companion object {

		/**
		 * Creates a new route object from the provided json object.
		 * If the json object is null then a RouteException will be thrown.
		 *
		 * @param jsonObject The json object contain the data to create a new route object.
		 * @return The newly created route object.
		 * @throws RouteException               Thrown if the json object is null, or if the route name is unable to be parsed.
		 * @throws UnsupportedEncodingException Thrown if the route name cannot be formatted to a URL.
		 */
		@JvmStatic
		@Throws(RouteException::class, UnsupportedEncodingException::class)
		fun generateRoute(jsonObject: JSONObject): Route {

			// First, parse the name.
			val name: String = try {
				jsonObject.getString("routeId")
			} catch (e: JSONException) {
				throw RouteException("Unable to get route name from JSON", e.cause)
			}

			// Now try to parse the route and route color and return the resulting object.
			return try {
				val colorName = jsonObject.getString("routeColor")
				val color = android.graphics.Color.parseColor(colorName)
				Route(name, color)
			} catch (e: JSONException) {
				Log.w("generateRoute", "Unable to parse color")

				// Since there was an issue parsing the color, and we have the name at this point...
				// Simply create the route without a color.
				Route(name)
			} catch (e: IllegalArgumentException) {
				Log.w("generateRoute", "Unable to parse color")
				Route(name)
			}
		}

		/**
		 * Iterates though all the routes in MapsActivity.allRoutes
		 * and enables those that have been favorited (as determined by being in the favoritedRoutes array).
		 *
		 * This should only be run once.
		 *
		 * @param favoritedRoutes The selected routes to be enabled from MapsActivity.allRoutes.
		 */
		@JvmStatic
		fun enableFavoriteRoutes(favoritedRoutes: Array<Route>) {

			// Iterate through all the routes that will be used in the activity.
			for (allRoute in MapsActivity.allRoutes) {

				// Iterate though the favorite routes.
				for (favoritedRoute in favoritedRoutes) {

					// If the route name matches the favorited route name, enable it.
					if (allRoute.routeName == favoritedRoute.routeName) {
						allRoute.enabled = true
						break
					}
				}
			}
		}
	}

	init {

		// Set the route color.
		this.color = color
	}
}