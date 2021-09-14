package fnsb.macstransit.activities.loadingactivity.loadingscreenrunnables

import fnsb.macstransit.activities.loadingactivity.LoadingViewModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by Spud on 9/1/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.3.1.
 */
abstract class DownloadRouteObjects<T>(val viewModel: LoadingViewModel) {

	/**
	 * Downloads specific content pertaining to the provided route.
	 *
	 * @param route            The route that will be used to find the downloadable content.
	 * @param downloadProgress The download progress amount.
	 * @param progressSoFar    The progress that has been completed so far.
	 * @param index            The index of the downloadable in terms of progress.
	 *
	 * @return The parsed data from the download.
	 */
	abstract suspend fun download(route: fnsb.macstransit.routematch.Route, downloadProgress: Double,
	                              progressSoFar: Double, index: Int): T

	internal abstract class DownloadableCallback<T>(private val continuation: kotlin.coroutines.
	Continuation<T>, val viewModel: LoadingViewModel, private val parseMessage: Int):
			com.android.volley.Response.Listener<JSONObject> {

		/**
		 * Function that parses the provided JSON Array.
		 * @param jsonArray The JSON Array to be parsed.
		 *
		 * @return The parsed data as a single object.
		 */
		abstract fun parse(jsonArray: JSONArray): T

		override fun onResponse(response: JSONObject) {

			// Set the message in the splash activity to the provided message.
			this.viewModel.setMessage(this.parseMessage)

			// Get the data from all the stops and store it in a JSONArray.
			val data: JSONArray = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// Parse the downloaded data.
			val parsedData: T = this.parse(data)

			// Resume the coroutine and return our parsed data.
			android.util.Log.v("DownloadRouteObject", "Finished parsing downloadable object")
			this.continuation.resumeWith(Result.success(parsedData))
		}
	}
}