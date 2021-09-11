package fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables

import android.util.Log
import fnsb.macstransit.R
import fnsb.macstransit.activities.loadingactivity.LoadingViewModel
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.Stop
import java.lang.NullPointerException

/**
 * Created by Spud on 8/6/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.1.
 * @since Release 1.3.
 */
class DownloadBusStops(viewModel: LoadingViewModel): DownloadRouteObjects<Unit>(viewModel) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): Unit = kotlin.coroutines.suspendCoroutine {

		// Set the message that we are downloading bus stops.
		this.viewModel.setMessage(R.string.loading_bus_stops)

		// Download the bus stops.
		this.viewModel.routeMatch.callAllStops(route, ParseBusStops(it, this.viewModel, route), {
			error: com.android.volley.VolleyError -> Log.w("loadStops",
			                                               "Unable to get stops from RouteMatch server",
			                                               error)
		})

		// Get the progress step.
		val step: Double = downloadProgress / this.viewModel.routes.size

		// Update the progress bar.
		this.viewModel.setProgressBar(progressSoFar + step + index)
	}

	internal class ParseBusStops(continuation: kotlin.coroutines.Continuation<Unit>,
	                             viewModel: LoadingViewModel, private val route : Route) :
			DownloadableCallback<Unit>(continuation, viewModel, R.string.mapping_bus_stops) {

		override fun parse(jsonArray: org.json.JSONArray) {

			// Create an array of stops that will be filled using the information from the json array.
			val count = jsonArray.length()

			// Iterate though the json array.
			for (i in 0 until count) {

				// Try to create a new stop object using the information in the json array.
				val stop: Stop = try {
					Stop(jsonArray.getJSONObject(i), this.route)
				} catch (e: org.json.JSONException) {

					// If unsuccessful simply log the exception and continue iterating.
					Log.e("generateStops", "Exception occurred while creating stop!", e)
					continue
				}

				// Try to add the stop the route's stop hashmap in the view model.
				try {
					this.viewModel.routes[this.route.name]!!.stops[stop.name] = stop
				} catch (NullPointerException : NullPointerException) {

					// If the route of the stop is not in the routes hashmap then log it as an error.
					Log.e("ParseBusStops",
					      "Could not add bus stop to hashmap - invalid route", NullPointerException)
				}
			}
		}
	}
}