package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.activities.splashactivity.SplashActivity
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Spud on 8/5/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusRoutes(private val activity: SplashActivity) {

	/**
	 * Documentation
	 * Comments
	 * @param route
	 */
	suspend fun downloadRoute(route: Route, index: Int): JSONObject = suspendCoroutine { continuation ->
		val step: Double = SplashActivity.DOWNLOAD_BUS_ROUTES.toDouble() / MapsActivity.allRoutes.size
		val progress: Double = (SplashActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS + SplashActivity.PARSE_MASTER_SCHEDULE).toDouble()
		Log.d("downloadRoute", "Step value: $step")

		// Get the land route from the routematch API using an asynchronous process.
		this.activity.routeMatch.callLandRoute(route, BusRoutesCallback(continuation, route),
		                                       { error: com.android.volley.VolleyError ->
			Log.w("downloadRoute", "Unable to get polyline from routematch server", error)
		}, this)
		this.activity.viewModel.setProgressBar(progress + step + index)
	}


	internal inner class BusRoutesCallback(private val continuation: Continuation<JSONObject>,
	                                       private val route: Route) : com.android.volley.Response.Listener<JSONObject> {

		override fun onResponse(response: JSONObject) {

			// Display that we are mapping bus routes to the user.
			this@DownloadBusRoutes.activity.viewModel.setMessage(fnsb.macstransit.R.string.mapping_bus_routes)
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
				this.route.polyLineCoordinates = coordinates.requireNoNulls()

				// Comments
				Log.v("BusRoutesCallback", "Finished bus callback")
				this.continuation.resume(response)
			} catch (exception: org.json.JSONException) {
				Log.e("BusRoutesCallback", "Error parsing json", exception)
				this.continuation.resumeWithException(exception)
			}
		}
	}
}