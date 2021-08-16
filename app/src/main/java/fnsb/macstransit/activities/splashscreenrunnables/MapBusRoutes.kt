package fnsb.macstransit.activities.splashscreenrunnables

import android.util.Log
import android.util.Pair
import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.activities.MapsActivity
import fnsb.macstransit.activities.SplashActivity
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Created by Spud on 8/5/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class MapBusRoutes(private val routeMatch: RouteMatch) {

	/**
	 * Documentation
	 */
	private val pairs: MutableCollection<Pair<Route, SplashListener>> = ArrayList() // TODO Don't use arrayList

	/**
	 * Documentation
	 *
	 * @param pair TODO
	 */
	fun addListener(pair: Pair<Route, SplashListener>) {
		this.pairs.add(pair)
	}

	/**
	 * Loads the polyline coordinates for the route object by retrieving the array from the RouteMatch server.
	 * This method will either set the polyline coordinates for the route,
	 * or will return early if the route match object is null.
	 *
	 * @param activity TODO
	 */
	fun getBusRoutes(activity: SplashActivity) { // Comments

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("getBusRoutes", "All routes is null!")
			return
		}

		val step: Double = SplashActivity.DOWNLOAD_BUS_ROUTES.toDouble() / MapsActivity.allRoutes!!.size
		var progress: Double = (SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + SplashActivity.PARSE_MASTER_SCHEDULE).toDouble()
		Log.d("getBusRoutes", "Step value: $step")

		// Iterate though each route, and try to load the polyline in each of them.
		for (pair: Pair<Route, SplashListener> in pairs) {

			val callback = BusRoutesCallback(pair, activity)

			// Get the land route from the routematch API using an asynchronous process.
			this.routeMatch.callLandRoute(pair.first, callback, { error: com.android.volley.VolleyError ->
				Log.w("getBusRoutes", "Unable to get polyline from routematch server", error)
			}, this)
			progress += step
			activity.setProgressBar(progress)
		}
	}


	internal inner class BusRoutesCallback(private val pair: Pair<Route, SplashListener>,
		private val activity: SplashActivity) : com.android.volley.Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) { // Comments

			if (MapsActivity.allRoutes == null) {
				return  // TODO Log
			}

			// Display that we are mapping bus routes to the user.
			this.activity.setMessage(fnsb.macstransit.R.string.mapping_bus_routes)
			try {

				// Get the land route data array from the land route object.
				val landRouteData: JSONArray = RouteMatch.parseData(response)

				// Get the land route points object from the land route data array.
				val landRoutePoints: JSONObject = landRouteData.getJSONObject(0)

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

					// Create a new LatLng object using the latitude and longitude.
					val latLng = LatLng(latitude, longitude)

					// Add the newly created LatLng object to the LatLng array.
					coordinates[i] = latLng
				}

				// Set the polyline coordinates array to the finished LatLng array.
				this.pair.first.polyLineCoordinates = coordinates.requireNoNulls()

				// Remove our pair from the listener.
				pairs.remove(this.pair)

				// Comments
				this.pair.second.splashRunnableFinished()
				Log.v("BusRoutesCallback", "Finished bus callback")

			} catch (exception: org.json.JSONException) {
				Log.e("BusRoutesCallback", "Error parsing json", exception)
			}
		}
	}
}