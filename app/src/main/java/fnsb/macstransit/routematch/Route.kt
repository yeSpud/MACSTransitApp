package fnsb.macstransit.routematch

import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.UiThread
import com.android.volley.VolleyError
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.ktx.addPolyline
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 3.1.
 * @since Beta 3.
 */
class Route: Parcelable {

	/**
	 * The name of the route.
	 */
	val name: String

	/**
	 * The color of the route.
	 */
	var color: Int = 0

	/**
	 * The name of the route formatted to be parsed as a URL.
	 */
	val urlFormattedName: String

	/**
	 * All the stops in the route.
	 */
	val stops: HashMap<String, Stop> = HashMap()

	/**
	 * All the shared stops in the route.
	 */
	val sharedStops: HashMap<String, SharedStop> = HashMap()

	/**
	 * Whether or not the route is enabled or disabled (to be shown or hidden).
	 * Default is false (disabled).
	 */
	@Transient
	var enabled = false

	/**
	 * The polyline that corresponds to this route.
	 */
	@Transient
	private var polyline: Polyline? = null

	/**
	 * Creates a new Route object using information from the provided parcel.
	 * @param parcel The parcel containing all the route information.
	 */
	constructor(parcel: Parcel) {
		Log.v("Route", "Reading from parcel")

		// Load the name, color, and formatted name from the parcel.
		name = parcel.readString()!!
		color = parcel.readInt()
		urlFormattedName = parcel.readString()!!

		// Load the array of stops from the parcel.
		if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			val stopArray: Array<Stop>? = parcel.readParcelableArray(Stop::class.java.classLoader, Stop::class.java)
			if (stopArray != null) {
				for (stop in stopArray) {
					stops[stop.name] = stop
				}
			}
		} else {
			@Suppress("DEPRECATION") // Suppressed because the function is replaced in newer APIs
			val stopParcelableArray: Array<Parcelable>? = parcel.readParcelableArray(Stop::class.java.classLoader)
			if (stopParcelableArray != null) {
				for (stop in stopParcelableArray) {
					if (stop is Stop) {
						stops[stop.name] = stop
					}
				}
			}
		}

		// Load the array of shared stops from the parcel.
		if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			val sharedStopParcelableArray: Array<SharedStop>? = parcel.readParcelableArray(SharedStop::class.java.classLoader, SharedStop::class.java)
			if (sharedStopParcelableArray != null) {
				for (sharedStop in sharedStopParcelableArray) {
					sharedStops[sharedStop.name] = sharedStop
				}
			}
		} else {
			@Suppress("DEPRECATION") // Suppressed because the function is replaced in newer APIs
			val sharedStopParcelableArray: Array<Parcelable>? = parcel.readParcelableArray(SharedStop::class.java.classLoader)
			if (sharedStopParcelableArray != null) {
				for (sharedStop in sharedStopParcelableArray) {
					if (sharedStop is SharedStop) {
						sharedStops[sharedStop.name] = sharedStop
					}
				}
			}
		}

	}

	/**
	 * Constructor for a new Route object with only a name.
	 *
	 * @param routeName The name of the route. This cannot contain any whitespace characters.
	 * @throws UnsupportedEncodingException Thrown if the route name cannot be formatted to a URL.
	 */
	@Throws(UnsupportedEncodingException::class)
	constructor(routeName: String) : this(routeName, 0)

	/**
	 * Creates a new Route object.
	 *
	 * @param name The name of the route. This cannot contain any whitespace characters.
	 * @param color The route color.
	 * @throws UnsupportedEncodingException Thrown if the route name contains whitespace.
	 */
	@Throws(UnsupportedEncodingException::class)
	constructor(name: String, color: Int) {

		// Make sure the route name does not contain any whitespace characters.
		if (name.contains(Regex("\\s"))) {
			throw UnsupportedEncodingException("Route name contains whitespace!")
		}

		// Set the route name and color.
		this.name = name
		this.color = color

		// Set the urlFormattedName from the name.
		urlFormattedName = Pattern.compile("\\+").matcher(URLEncoder.encode(this.name, "UTF-8"))
			.replaceAll("%20")
	}

	/**
	 * Creates and sets the polyline for the route.
	 *
	 * This must be run on the UI Thread.
	 *
	 * @param coordinates The coordinates of the polyline.
	 * @param map The map to add the polyline to.
	 */
	@UiThread
	fun createPolyline(coordinates: Array<LatLng>, map: GoogleMap) {
		Log.v("createPolyline", "Creating route polyline")

		// Add the polyline to the map with the following options:
		polyline = map.addPolyline {

			// Create new polyline options from the array of polyline coordinates stored for the route.
			add(*coordinates)

			// Make sure its not clickable.
			clickable(false)

			// Set the color of the polylines based on the route color.
			color(color)

			// Set the polyline visibility to whether or not the route is enabled.
			visible(enabled)
		}
	}

	/**
	 * Removes stops that have shared stops.
	 */
	fun purgeStops() {

		for (stopName: String in sharedStops.keys) {

			// Try to remove stops by shared stop name.
			if (stops.remove(stopName) != null) {

				// If the value for removed wasn't null then log the stop for debugging.
				Log.d("purgeStops", "Stop removed: $stopName")
			}
		}
	}

	/**
	 * Sets the polylines visibly to whether or not the route is enabled or not.
	 * If the polyline didn't previously exist it will be created in this method.
	 *
	 * This must be run on the UI Thread.
	 *
	 * @param routeMatch The RouteMatch object to use if the polyline coordinates need to be retrieved.
	 * @param map        The map to add the polyline to.
	 */
	@UiThread
	fun togglePolylineVisibility(routeMatch: RouteMatch, map: GoogleMap) {

		if (polyline != null) {
			Log.d("togglePolyline", "Setting route $name to visible: $enabled")
			polyline!!.isVisible = enabled
			return
		}

		// We know the polyline is null here...
		// Get the polyline coordinates for the route from the RouteMatch server.
		routeMatch.callLandRoute(this,  {

			// Get the data from all the stops and store it in a JSONArray.
			val data: JSONArray = RouteMatch.parseData(it)

			// Get the land route points object from the land route data array.
			val landRoutePoints: JSONObject = data.getJSONObject(0)

			// Get the land route points array from the land route points object.
			val landRoutePointsArray: JSONArray = landRoutePoints.getJSONArray("points")

			// Get the number of points in the array.
			val count: Int = landRoutePointsArray.length()

			// Create a new LatLng array to store all the coordinates.
			val coordinates = arrayOfNulls<LatLng>(count)

			// Initialize the array of coordinates by iterating through the land route points array.
			for (i in 0 until count) {

				// Get the land route point object from the land route points array.
				val landRoutePoint = landRoutePointsArray.getJSONObject(i)

				// Get the latitude and longitude from the land route point.
				val latitude: Double = landRoutePoint.getDouble("latitude")
				val longitude: Double = landRoutePoint.getDouble("longitude")

				// Add the newly created LatLng object to the LatLng array.
				coordinates[i] = LatLng(latitude, longitude)
			}

			// Create a new polyline for the route since it didn't have one before.
			@Suppress("UNCHECKED_CAST") // Suppressed because we are asserting that none of the coordinates are null
			createPolyline(coordinates as Array<LatLng>, map)

			// Log if there was any error getting the polyline coordinates.
		} , { error: VolleyError -> Log.e("togglePolylineVisible", "Unable to get polyline coordinates", error) }, this)

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
		fun generateRoute(jsonObject: JSONObject): Route {

			// First, parse the name.
			val name = jsonObject.getString("routeId")

			return try {

				// Now try to parse the route color.
				val colorName = jsonObject.getString("routeColor")
				val color = Color.parseColor(colorName)

				// Return our newly created route with color!
				Route(name, color)
			} catch (exception: Exception) {
				Log.w("generateRoute", "Unable to parse color", exception)

				// Since there was an issue parsing the color,
				// and we have the name at this point simply create the route without a color.
				Route(name)
			}
		}

		/**
		 * Iterates though all the provided routes and enables those that have been marked favorited.
		 *
		 * @param routes             All the routes that can be tracked.
		 * @param favoriteRouteNames The names of the favorite routes.
		 */
		@JvmStatic
		fun enableFavoriteRoutes(routes: HashMap<String, Route>, favoriteRouteNames: Array<String>) {

			for (routeName: String in favoriteRouteNames) {
				try {
					routes[routeName]!!.enabled = true
				} catch (nullPointerException: NullPointerException) {

					// If the route was not found log it as an error.
					Log.w("enableFavoriteRoutes", "$routeName route is not running today", nullPointerException)
				}
			}
		}

		@JvmField
		val CREATOR = object : Parcelable.Creator<Route> {

			override fun createFromParcel(parcel: Parcel): Route { return Route(parcel) }

			override fun newArray(size: Int): Array<Route?> { return arrayOfNulls(size) }
		}
	}

	override fun describeContents(): Int { return this.hashCode() }

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(this.name)
		parcel.writeInt(this.color)
		parcel.writeString(this.urlFormattedName)
		parcel.writeParcelableArray(this.stops.values.toTypedArray(), flags)
		parcel.writeParcelableArray(this.sharedStops.values.toTypedArray(), flags)
	}
}