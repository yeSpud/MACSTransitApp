package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Spud on 2019-11-20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
 * @since Beta 8.
 */
public class MarkedObject {

	/**
	 * The marker of the marker of the marked object.
	 */
	private Marker marker;

	/**
	 * Retrieves the marker of the object.
	 *
	 * @return The marker.
	 */
	public Marker getMarker() {
		return this.marker;
	}

	/**
	 * Sets the marker of the object.
	 *
	 * @param marker The marker to be set.
	 */
	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	/**
	 * TODO TODO Documentation
	 *
	 * @param map
	 * @param latitude
	 * @param longitude
	 * @param color
	 * @param title
	 * @return
	 */
	public Marker addMarker(com.google.android.gms.maps.GoogleMap map, double latitude, double longitude, int color, String title) {
		// Create a new maker options object
		MarkerOptions options = new MarkerOptions();

		// Set the position of the marker via the latitude and longitude.
		Log.d("addMarker", String.format("Setting marker position to %f, %f", latitude, longitude));
		options.position(new com.google.android.gms.maps.model.LatLng(latitude, longitude));

		// Set the color of the marker.
		Log.d("addMarker", "Applying marker color");
		options.icon(fnsb.macstransit.Activities.ActivityListeners.Helpers.getMarkerIcon(color));

		// Add the marker to the map.
		Log.d("addMarker", "Adding marker to the map");
		Marker marker = map.addMarker(options);

		// Set the marker title.
		Log.d("addMarker", "Setting marker title to: " + title);
		marker.setTitle(title);

		// Set the marker's tag.
		Log.d("addMarker", "Setting the markers tag to: " + this);
		marker.setTag(this);

		// Return the generated marker.
		return marker;
	}

}
