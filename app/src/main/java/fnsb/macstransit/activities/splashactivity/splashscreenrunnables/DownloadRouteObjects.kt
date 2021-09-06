package fnsb.macstransit.activities.splashactivity.splashscreenrunnables

import fnsb.macstransit.routematch.Route
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Created by Spud on 9/1/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.3.1.
 */
abstract class DownloadRouteObjects(val viewModel: fnsb.macstransit.activities.splashactivity.SplashViewModel) {

	/**
	 * Downloads specific content pertaining to the provided route.
	 *
	 * @param route The route that will be used to find the downloadable content.
	 * @param index The index of the downloadable in terms of progress.
	 * @return The downloaded (un-parsed) content. TODO Replace with prototyping - return the prototype.
	 */
	abstract suspend fun download(route: Route, downloadProgress: Double, progressSoFar: Double,
	                              index: Int): JSONObject

	/**
	 * Callback used once the download function has finished,
	 * and the downloaded content needs to be parsed.
	 */
	internal abstract inner class Callback(

			/**
			 * The The suspended continuation coroutine to resume once the the callback has finished.
			 */
			private val continuation: kotlin.coroutines.Continuation<JSONObject>,

			/**
			 * The route that the download method is acting on.
			 */
			val route: Route,

			/**
			 * The message to display while parsing the download.
			 */
			private val message: Int): com.android.volley.Response.Listener<JSONObject> {

		/**
		 * Function that parses the provided JSON Array.
		 * @param jsonArray The JSON Array to be parsed.
		 */
		abstract fun parse(jsonArray: JSONArray)

		override fun onResponse(response: JSONObject) {

			// Set the message in the splash activity to the provided message.
			this@DownloadRouteObjects.viewModel.setMessage(this.message)

			// Get the data from all the stops and store it in a JSONArray.
			val data: JSONArray = fnsb.macstransit.routematch.RouteMatch.parseData(response)

			// Pass the data to our function.
			this.parse(data) // TODO Implement prototyping to get this to return the prototype.

			// Resume the coroutine as the downloadable has finished being parsed.
			android.util.Log.v("DownloadRouteObject", "Finished parsing downloadable object")
			this.continuation.resume(response)  // TODO Implement prototyping to get this to pass the prototype.
		}
	}
}