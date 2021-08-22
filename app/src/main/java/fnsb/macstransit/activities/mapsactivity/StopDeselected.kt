package fnsb.macstransit.activities.mapsactivity

import com.android.volley.RequestQueue
import fnsb.macstransit.routematch.MarkedObject

/**
 * Created by Spud on 2019-11-11 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0.
 * @since Beta 7.
 */
@androidx.annotation.UiThread
class StopDeselected(private val networkQueue: RequestQueue) : com.google.android.gms.maps.GoogleMap.OnInfoWindowCloseListener {

	/**
	 * Called when the marker's info window is closed.
	 * This is called on the Android UI thread.
	 *
	 * @param marker The marker of the info window that was closed.
	 */
	override fun onInfoWindowClose(marker: com.google.android.gms.maps.model.Marker) {

		// Get the tag as a marked object for easier lookup.
		val potentialStop: MarkedObject = marker.tag as MarkedObject

		// Check if it was a stop info window that was closed.
		if (potentialStop is fnsb.macstransit.routematch.Stop || potentialStop is fnsb.macstransit.routematch.SharedStop) {

			// Cancel the network request.
			this.networkQueue.cancelAll(marker)

			// Just hide the marker, since we don't want to destroy it just yet.
			marker.isVisible = false
		} else {

			// Log that the info window that was closed was neither a Stop nor a SharedStop.
			android.util.Log.w("onInfoWindowClose", "Unhandled info window")
		}
	}
}