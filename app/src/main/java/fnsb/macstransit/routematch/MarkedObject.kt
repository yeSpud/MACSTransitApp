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
 * @version 2.1.
 * @since Beta 8.
 */
open class MarkedObject(val name: String, var location: LatLng, val routeName: String, val color: Int) {

	/**
	 * The marker of the marker of the marked object.
	 * This has a private set as we only want to create (set) the marker in the marked object class.
	 */
	var marker: Marker? = null
		private set

	override fun equals(other: Any?): Boolean {

		// If the comparison object is null just return false.
		if (other == null) {
			return false
		}

		// If the comparison object is a Stop object...
		return if (other is Stop) {

			// Return if the locations and names of the stop and our comparison match.
			this.location.latitude == other.location.latitude &&
			this.location.longitude == other.location.longitude && this.name == other.name

		} else {
			false // If the comparison object is not a Stop object return false.
		}
	}

	override fun hashCode(): Int {

		// Start by getting the hash code for the name.
		var result: Int = name.hashCode()

		// Now keep multiplying the result by a shift for each marked object properties to get the hash.
		// https://www.baeldung.com/java-hashcode
		result = 0b00011111 * result + this.location.hashCode()
		result = 0b00011111 * result + this.routeName.hashCode()
		result = 0b00011111 * result + this.color
		result = 0b00011111 * result + (this.marker?.hashCode() ?: 0)

		// Return our resulting hash code.
		return result
	}

	/**
	 * Update the location of the marked object.
	 * This also updates the marker position if it has one. Insert fairly-odd-parents meme.
	 * @param updatedLocation The updated location of the object.
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
			android.graphics.Color.colorToHSV(this@MarkedObject.color, hsv)
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