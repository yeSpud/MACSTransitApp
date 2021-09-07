package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import fnsb.macstransit.activities.splashactivity.SplashViewModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by Spud on 9/1/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.1.
 */
abstract class DownloadRouteObjects<T>(val viewModel: SplashViewModel) {

	/**
	 * Downloads specific content pertaining to the provided route.
	 *
	 * @param route The route that will be used to find the downloadable content.
	 * @param downloadProgress Documentation
	 * @param progressSoFar Documentation
	 * @param index The index of the downloadable in terms of progress.
	 *
	 * @return Documentation
	 */
	abstract suspend fun download(route: fnsb.macstransit.routematch.Route, downloadProgress: Double,
	                              progressSoFar: Double, index: Int): Array<T>

	internal abstract class DownloadableCallback<T>(private val continuation: kotlin.coroutines.
	Continuation<Array<T>>, val viewModel: SplashViewModel, private val parseMessage: Int):
			com.android.volley.Response.Listener<JSONObject> {

		/**
		 * Function that parses the provided JSON Array.
		 * @param jsonArray The JSON Array to be parsed.
		 *
		 * @return Documentation
		 */
		abstract fun parse(jsonArray: JSONArray): Array<T>

		override fun onResponse(response: JSONObject) {

			// Set the message in the splash activity to the provided message.
			this.viewModel.setMessage(this.parseMessage)

			// Get the data from all the stops and store it in a JSONArray.
			val data: JSONArray = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// Comments
			val parsedData: Array<T> = this.parse(data)

			// Comments
			android.util.Log.v("DownloadRouteObject", "Finished parsing downloadable object")
			this.continuation.resumeWith(Result.success(parsedData))
		}
	}
}