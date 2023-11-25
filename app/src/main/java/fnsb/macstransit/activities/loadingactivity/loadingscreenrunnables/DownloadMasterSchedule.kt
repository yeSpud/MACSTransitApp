package fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables

import android.util.Log
import com.android.volley.VolleyError
import fnsb.macstransit.R
import fnsb.macstransit.activities.loadingactivity.LoadingActivity
import fnsb.macstransit.activities.loadingactivity.LoadingViewModel
import fnsb.macstransit.routematch.Route
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.Continuation

/**
 * Created by Spud on 8/16/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.1.
 * @since Release 1.3.
 */
class DownloadMasterSchedule(private val loadingActivity: LoadingActivity):
		DownloadRouteObjects<Unit>(loadingActivity.binding.viewmodel!!) {

	override suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): Unit = kotlin.coroutines.suspendCoroutine {

		// Set the progress.
		loadingActivity.binding.viewmodel!!.setProgressBar(downloadProgress)

		// Make the network request for the master route.
		loadingActivity.binding.viewmodel!!.routeMatch.callMasterSchedule(ParseMasterSchedule(it, loadingActivity, viewModel)) { error: VolleyError ->

			// Since there was an error retrieving the master route be sure to log it,
			// and show the retry message and button.
			Log.w("initializeApp", "MasterSchedule callback error", error)
			Log.w("initializeApp", "Error: ${error.message}\n${error.cause.toString()}")
			loadingActivity.binding.viewmodel!!.setMessage(R.string.routematch_timeout)
			loadingActivity.allowForRetry()
		}
	}

	internal class ParseMasterSchedule(continuation: Continuation<Unit>, private val loadingActivity: LoadingActivity,
	                                   viewModel: LoadingViewModel) : DownloadableCallback<Unit>(continuation, viewModel, R.string.parsing_master_schedule) {

		override fun parse(jsonArray: org.json.JSONArray) {

			// Get the route count from the JSON Array.
			val count = jsonArray.length()

			// If the routes length is 0, say that there are no buses for the day.
			if (count == 0) {
				viewModel.setMessage(R.string.its_sunday)

				// Also add a chance for the user to retry.
				loadingActivity.allowForRetry()
				loadingActivity.loaded = true
			}

			// Update the progress and message.
			val step = LoadingActivity.PARSE_MASTER_SCHEDULE.toDouble() / count
			var progress = LoadingActivity.DOWNLOAD_MASTER_SCHEDULE_PROGRESS.toDouble()

			// Iterate though each route in the master schedule.
			for (index in 0 until count) {
				Log.d("MasterScheduleCallback", "Parsing route ${index + 1}/$count")

				// Try to get the route data from the array.
				// If there's an issue parsing the data simply continue to the next iteration of the loop.
				val routeData: JSONObject = try {
					jsonArray.getJSONObject(index)
				} catch (e: JSONException) {
					Log.w("MasterScheduleCallback", "Issue retrieving the route data", e)
					continue
				}

				// Try to create the route using the route data obtained above.
				try {
					val route = Route.generateRoute(routeData)

					// Add the route to the hashmap in the view model.
					loadingActivity.binding.viewmodel!!.routes[route.name] = route
				} catch (exception: Exception) {

					// If there was a route exception thrown simply log it.
					Log.e("MasterScheduleCallback", "Issue creating route from route data", exception)
				}
				loadingActivity.binding.viewmodel!!.setProgressBar(progress + step)
				progress += step
			}
		}
	}
}