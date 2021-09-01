package fnsb.macstransit.routematch

import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker

/**
 * Created by Spud on 2019-11-20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Beta 8.
 */
open class MarkedObject(val name: String, var location: LatLng, val route: Route) {

	/**
	 * The marker of the marker of the marked object.
	 * TODO Mention private set
	 */
	var marker: Marker? = null
		private set

	override fun equals(other: Any?): Boolean { // Comments

		if (other == null) {
			return false
		}

		return if (other is Stop) {

			this.location.latitude == other.location.latitude &&
			this.location.longitude == other.location.longitude && this.name == other.name

		} else {
			false
		}
	}

	override fun hashCode(): Int { // Comments
		var result = name.hashCode()
		result = 0x1F * result + location.hashCode()
		result = 0x1F * result + route.hashCode()
		result = 0x1F * result + (marker?.hashCode() ?: 0)
		return result
	}

	/**
	 * Documentation
	 */
	@UiThread
	fun updateLocation(updatedLocation: LatLng) {
		this.location = updatedLocation
		if (this.marker != null) {
			this.marker!!.position = this.location
		}
	}

	/**
	 * Adds a marker to the map. Note that this method does not save the marker to the marked object.
	 * It only adds it to the map, and returns the newly added marker.
	 *
	 * @param map The map to add the marker to.
	 * This will try to get the closest approximation to the color as there are a limited number of marker colors.
	 * @return The newly added marker.
	 */
	@UiThread
	fun addMarker(map: com.google.android.gms.maps.GoogleMap) {

		// Add the marker to the map.
		Log.d("addMarker", "Adding marker to the map")
		val marker: Marker? = map.addMarker {

			// Set the position of the marker via the latitude and longitude.
			this.position(this@MarkedObject.location)

			// Set the color of the marker.
			Log.d("addMarker", "Applying marker color")
			val hsv = FloatArray(3)
			android.graphics.Color.colorToHSV(this@MarkedObject.route.color, hsv)
			this.icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0]))
		}

		// If the marker isn't null add more fields.
		if (marker != null) {

			// Set the marker title.
			Log.d("addMarker", "Setting marker title to: ${this.name}")
			marker.title = this.name

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