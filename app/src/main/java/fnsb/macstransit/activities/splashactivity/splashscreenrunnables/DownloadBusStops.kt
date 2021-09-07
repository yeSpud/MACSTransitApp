package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import android.util.Log
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
class DownloadBusStops(viewModel: SplashViewModel): DownloadRouteObjects<HashMap<String, Stop>>(viewModel) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): HashMap<String, Stop> = kotlin.coroutines.suspendCoroutine {

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

	internal class ParseBusStops(continuation: kotlin.coroutines.Continuation<HashMap<String, Stop>>,
	                             viewModel: SplashViewModel, private val route : Route) :
			DownloadableCallback<HashMap<String, Stop>>(continuation, viewModel, fnsb.macstransit.R.string.mapping_bus_stops) {

		override fun parse(jsonArray: org.json.JSONArray): HashMap<String, Stop> {

			// Comments
			val hashMap: HashMap<String, Stop> = HashMap()

			// Create an array of stops that will be filled using the information from the json array.
			val count = jsonArray.length()

			// Iterate though the json array.
			for (i in 0 until count) {

				// Try to create a new stop object using the information in the json array.
				val stop: Stop = try {
					Stop(jsonArray.getJSONObject(i), route)
				} catch (e: org.json.JSONException) {

					// If unsuccessful simply log the exception and continue iterating.
					Log.e("generateStops", "Exception occurred while creating stop!", e)
					continue
				}

				// Comments
				hashMap[stop.name] = stop
			}

			// Comments
			return hashMap
		}
	}
}