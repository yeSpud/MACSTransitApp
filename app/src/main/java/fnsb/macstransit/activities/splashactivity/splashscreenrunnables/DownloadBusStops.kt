package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import fnsb.macstransit.activities.splashactivity.SplashViewModel
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.Stop

/**
 * Created by Spud on 8/6/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.
 */
class DownloadBusStops(viewModel: SplashViewModel): DownloadRouteObjects<Stop>(viewModel) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): Array<Stop> = kotlin.coroutines.suspendCoroutine {

		this.viewModel.routeMatch.callAllStops(route, ParseBusStops(it, this.viewModel, route), {
			error: com.android.volley.VolleyError ->
			android.util.Log.w("loadStops", "Unable to get stops from RouteMatch server", error)
		})

		// Get the progress step.
		val step: Double = downloadProgress / this.viewModel.routes.size

		// Update the progress bar.
		this.viewModel.setProgressBar(progressSoFar + step + index)
	}

	internal class ParseBusStops(continuation: kotlin.coroutines.Continuation<Array<Stop>>,
	                             viewModel: SplashViewModel, private val route : Route) :
			DownloadableCallback<Stop>(continuation, viewModel, fnsb.macstransit.R.string.mapping_bus_stops) {

		override fun parse(jsonArray: org.json.JSONArray): Array<Stop> {

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
			potentialStops.forEach {

				// Check to see if the stop is in our array of validated stops. If its not,
				// add it to the array and add 1 to the true index size of stops that have been validated.
				if (!Stop.isDuplicate(it, validatedStops)) {
					validatedStops[validatedSize] = it
					validatedSize++
				}
			}

			// Create an array for our actual stops.
			// Since we now know the number of validated stops we can use that as its size.
			val actualStops: Array<Stop?> = arrayOfNulls(validatedSize)

			// Copy our validated stops into our smaller actual stops array, and return it.
			System.arraycopy(validatedStops, 0, actualStops, 0, actualStops.size)

			// At this point duplicate stops have now been handled and removed.
			// Return our actual stops.
			@Suppress("UNCHECKED_CAST")
			return actualStops as Array<Stop>
		}
	}
}