package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.Stop
import org.json.JSONObject

/**
 * Created by Spud on 8/6/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusStops(viewModel: fnsb.macstransit.activities.splashactivity.SplashViewModel):
		DownloadRouteObjects(viewModel) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): JSONObject = kotlin.coroutines.suspendCoroutine {

		this.viewModel.routeMatch.callAllStops(route, BusStopCallback(it, route), {
			error: com.android.volley.VolleyError ->
			android.util.Log.w("loadStops", "Unable to get stops from RouteMatch server", error)
		})

		val step: Double = downloadProgress / fnsb.macstransit.activities.mapsactivity.MapsActivity.
		allRoutes.size
		this.viewModel.setProgressBar(progressSoFar + step + index)
	}

	internal inner class BusStopCallback(continuation: kotlin.coroutines.Continuation<JSONObject>,
	                                     route: Route):
			DownloadRouteObjects.Callback(continuation, route, fnsb.macstransit.R.string.mapping_bus_stops) {

		override fun function(jsonArray: org.json.JSONArray) {

			// Load in all the potential stops for the route.
			// The reason why this is considered potential stops is because at this stage duplicate
			// stops have not yet been handled.
			val potentialStops: Array<Stop> = Stop.generateStops(jsonArray, this.route)

			// Create a variable to store the true size of the stops that have been validated.
			var validatedSize = 0

			// Create an array to store the validated stops.
			// While we don't know the specific size of this array until done, we do know the maximum size,
			// so use that for setting the array size.
			val validatedStops = arrayOfNulls<Stop>(potentialStops.size)

			// Iterate through each stop in our array of potential stops.
			for (stop: Stop in potentialStops) {

				// Check to see if the stop is in our array of validated stops. If its not,
				// add it to the array and add 1 to the true index size of stops that have been validated.
				if (!Stop.isDuplicate(stop, validatedStops)) {
					validatedStops[validatedSize] = stop
					validatedSize++
				}
			}

			// Create an array for our actual stops.
			// Since we now know the number of validated stops we can use that as its size.
			val actualStops = arrayOfNulls<Stop>(validatedSize)

			// Copy our validated stops into our smaller actual stops array, and return it.
			System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.size)

			// At this point duplicate stops have now been handled and removed.
			this.route.stops = actualStops.requireNoNulls()
		}
	}
}