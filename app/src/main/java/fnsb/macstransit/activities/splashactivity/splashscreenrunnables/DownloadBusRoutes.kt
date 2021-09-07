package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.routematch.Route
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by Spud on 8/5/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusRoutes(viewModel: fnsb.macstransit.activities.splashactivity.SplashViewModel):
		DownloadRouteObjects<LatLng>(viewModel, fnsb.macstransit.R.string.mapping_bus_routes) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): Array<LatLng> = kotlin.coroutines.suspendCoroutine {

		// Comment
		this.continuation = it

		// Get the land route from the routematch API using an asynchronous process.
		this.viewModel.routeMatch.callLandRoute(route, this, {
			error: com.android.volley.VolleyError ->
			Log.e("downloadRoute", "Unable to get polyline from routematch server", error)
		}, this)

		// Get the progress step.
		val step: Double = downloadProgress / fnsb.macstransit.activities.mapsactivity.MapsActivity.
		allRoutes.size

		// Update the progress bar.
		this.viewModel.setProgressBar(progressSoFar + step + index)
	}

	override fun parse(jsonArray: JSONArray): Array<LatLng> {
		try {

			// Get the land route points object from the land route data array.
			val landRoutePoints: JSONObject = jsonArray.getJSONObject(0)

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

			// Comments
			return coordinates as Array<LatLng>
		} catch (exception: org.json.JSONException) {

			// If there was a JSON Exception thrown while parsing simply log it.
			Log.e("BusRoutesCallback", "Exception thrown while parsing JSON in callback", exception)

			// Return a zero length array.
			return emptyArray()
		}
	}
}