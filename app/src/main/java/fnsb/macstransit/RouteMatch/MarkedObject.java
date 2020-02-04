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
	 * Static helper function that determines and returns the marker's BitmapDescriptor color
	 * based off of the provided color.
	 *
	 * @param color The color to set the marker to (or something close to it).
	 * @return The resulting BitmapDescriptor. This will almost certainly not be the exact color,
	 * but rather will be something close to it.
	 */
	private static com.google.android.gms.maps.model.BitmapDescriptor getMarkerIcon(int color) {
		float[] hsv = new float[3];
		android.graphics.Color.colorToHSV(color, hsv);
		return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0]);
	}

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
	 * Creates and adds a marker to the provided map.
	 *
	 * @param map       The map to add the marker to.
	 * @param latitude  The latitude of the marker.
	 * @param longitude The longitude of the marker.
	 * @param color     The desired color of the marker.
	 * @param title     The marker's title.
	 * @return The newly created marker that has also been added to the map.
	 */
	@SuppressWarnings("WeakerAccess")
	public Marker addMarker(com.google.android.gms.maps.GoogleMap map, double latitude,
	                        double longitude, int color, String title) {
		// Create a new maker options object
		MarkerOptions options = new MarkerOptions();

		// Set the position of the marker via the latitude and longitude.
		Log.d("addMarker", String.format("Setting marker position to %f, %f", latitude, longitude));
		options.position(new com.google.android.gms.maps.model.LatLng(latitude, longitude));

		// Set the color of the marker.
		Log.d("addMarker", "Applying marker color");
		options.icon(MarkedObject.getMarkerIcon(color));

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
