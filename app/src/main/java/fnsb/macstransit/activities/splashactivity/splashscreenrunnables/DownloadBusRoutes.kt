package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import fnsb.macstransit.activities.splashactivity.SplashViewModel
import org.json.JSONArray

/**
 * Created by Spud on 8/5/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusRoutes(viewModel: SplashViewModel):
		DownloadRouteObjects<Array<LatLng>>(viewModel) {

	override suspend fun download(route: fnsb.macstransit.routematch.Route, downloadProgress: Double,
	                              progressSoFar: Double, index: Int): Array<LatLng> = kotlin.
	coroutines.suspendCoroutine {

		// Get the land route from the routematch API using an asynchronous process.
		this.viewModel.routeMatch.callLandRoute(route, ParseBusRoutes(it, this.viewModel), {
			error: com.android.volley.VolleyError ->
			Log.e("downloadRoute", "Unable to get polyline from routematch server", error)
		}, this)

		// Get the progress step.
		val step: Double = downloadProgress / this.viewModel.routes.size

		// Update the progress bar.
		this.viewModel.setProgressBar(progressSoFar + step + index)
	}

	internal class ParseBusRoutes(continuation: kotlin.coroutines.Continuation<Array<LatLng>>,
	                              viewModel: SplashViewModel) :
			DownloadableCallback<Array<LatLng>>(continuation, viewModel, fnsb.macstransit.R.string.mapping_bus_routes) {

		override fun parse(jsonArray: JSONArray): Array<LatLng> {

			// Comments
			val hashMap: HashMap<String, LatLng> = HashMap()

			try {

				// Get the land route points object from the land route data array.
				val landRoutePoints: org.json.JSONObject = jsonArray.getJSONObject(0)

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

				hashMap["array", ]

				// Comments
				@Suppress("UNCHECKED_CAST")
				return coordinates as Array<LatLng>
			} catch (exception: org.json.JSONException) {

				// If there was a JSON Exception thrown while parsing simply log it.
				Log.e("BusRoutesCallback", "Exception thrown while parsing JSON in callback", exception)

				// Return a zero length array.
				return emptyArray()
			}
		}
	}
}