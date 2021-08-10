package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

/**
 * Created by Spud on 2019-11-20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Beta 8.
 */
open class MarkedObject(val name: String) {

	/**
	 * The marker of the marker of the marked object.
	 * TODO Mention private set
	 */
	var marker: Marker? = null
		private set

	/**
	 * Documentation
	 */
	@UiThread
	fun updateLocation(latitude: Double, longitude: Double) {
		if (this.marker != null) {
			this.marker!!.position = LatLng(latitude, longitude)
		}
	}

	/**
	 * Adds a marker to the map. Note that this method does not save the marker to the marked object.
	 * It only adds it to the map, and returns the newly added marker.
	 *
	 * @param map         The map to add the marker to.
	 * @param coordinates The LatLng coordinates of the marker.
	 * @param color       The color of the marker.
	 * This will try to get the closest approximation to the color as there are a limited number of marker colors.
	 * @return The newly added marker.
	 */
	@UiThread
	fun addMarker(map: com.google.android.gms.maps.GoogleMap, coordinates: LatLng, color: Int) {

		// Create a new maker options object
		val options = com.google.android.gms.maps.model.MarkerOptions()

		// Set the position of the marker via the latitude and longitude.
		options.position(coordinates)

		// Set the color of the marker.
		Log.d("addMarker", "Applying marker color")
		val hsv = FloatArray(3)
		android.graphics.Color.colorToHSV(color, hsv)
		options.icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0]))

		// Add the marker to the map.
		Log.d("addMarker", "Adding marker to the map")
		val marker: Marker? = map.addMarker(options)

		// If the marker isn't null add more fields.
		if (marker != null) {

			// Set the marker title.
			Log.d("addMarker", "Setting marker title to: $name")
			marker.title = name

			// Set the marker's tag.
			Log.d("addMarker", "Setting the markers tag to: ${this.javaClass}")
			marker.tag = this
		}

		// Set the marker to the generated marker
		this.marker = marker
	}

	/**
	 * Removes the marker from the map, and sets it to null.
	 * This must be run on the UI thread.
	 */
	@UiThread
	fun removeMarker() {
		if (marker != null) {
			marker!!.remove()
			marker = null
		}
	}
}