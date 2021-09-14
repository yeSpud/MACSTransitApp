package fnsb.macstransit.routematch

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.addPolyline
import org.json.JSONArray
import org.json.JSONException
import java.io.UnsupportedEncodingException

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
	private var polyline: com.google.android.gms.maps.model.Polyline? = null

	/**
	 * Creates a new Route object using information from the provided parcel.
	 * @param parcel The parcel containing all the route information.
	 */
	constructor(parcel: Parcel) {
		Log.v("Route", "Reading from parcel")

		// Load the name, color, and formatted name from the parcel.
		this.name = parcel.readString()!!
		this.color = parcel.readInt()
		this.urlFormattedName = parcel.readString()!!

		// Load the array of stops from the parcel.
		val stopArray: Array<Parcelable> = parcel.readParcelableArray(Stop::class.java.classLoader)
				as Array<Parcelable>
		for (entry in stopArray) {
			val stop: Stop = entry as Stop
			this.stops[stop.name] = stop
		}

		// Load the array of shared stops from the parcel.
		val sharedStopParcelableArray: Array<Parcelable> = parcel.
		readParcelableArray(SharedStop::class.java.classLoader) as Array<Parcelable>
		for (entry in sharedStopParcelableArray) {
			val sharedStop: SharedStop = entry as SharedStop
			this.sharedStops[sharedStop.name] = sharedStop
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
		this.urlFormattedName = java.util.regex.Pattern.compile("\\+").matcher(java.net.URLEncoder.
		encode(this.name, "UTF-8")).replaceAll("%20")
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
		this.polyline = map.addPolyline {

			// Create new polyline options from the array of polyline coordinates stored for the route.
			this.add(*coordinates)

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
	 * This must be run on the UI Thread.
	 *
	 * @param routeMatch The RouteMatch object to use if the polyline coordinates need to be retrieved.
	 * @param map        The map to add the polyline to.
	 */
	@UiThread
	fun togglePolylineVisibility(routeMatch: RouteMatch, map: GoogleMap) {

		// Check if the route has a polyline to set visible.
		if (this.polyline == null) {

			// Get the polyline coordinates for the route from the RouteMatch server.
			routeMatch.callLandRoute(this,  {

				// Get the data from all the stops and store it in a JSONArray.
				val data: JSONArray = RouteMatch.parseData(it)

				// Get the land route points object from the land route data array.
				val landRoutePoints: org.json.JSONObject = data.getJSONObject(0)

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
				this.createPolyline(coordinates as Array<LatLng>, map)

				// Log if there was any error getting the polyline coordinates.
			} , { error: com.android.volley.VolleyError ->
				Log.e("togglePolylineVisible", "Unable to get polyline coordinates", error)
			}, this)
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
		 * Iterates though all the provided routes and enables those that have been marked favorited.
		 *
		 * @param routes             All the routes that can be tracked.
		 * @param favoriteRouteNames The names of the favorite routes.
		 */
		@JvmStatic
		fun enableFavoriteRoutes(routes: HashMap<String, Route>, favoriteRouteNames: Array<String>) {

			// Iterate though each favorite route and try to enable it.
			favoriteRouteNames.forEach {
				try {
					routes[it]!!.enabled = true
				} catch (NullPointerException: NullPointerException) {

					// If the route was not found log it as an error.
					Log.w("enableFavoriteRoutes", "$it route is not running today", NullPointerException)
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