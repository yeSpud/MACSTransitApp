package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jetbrains.annotations.NotNull;


/**
 * Created by Spud on 2019-11-20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.1.
 * @since Beta 8.
 */
public class MarkedObject {

	/**
	 * The marker of the marker of the marked object.
	 */
	@androidx.annotation.Nullable
	public Marker marker;

	/**
	 * The name / title / ID of the marked object.
	 */
	public final String name;

	/**
	 * Constructor for a marked object.
	 *
	 * @param name The name of the marked object. This will later be used as the markers title.
	 */
	public MarkedObject(String name) {
		this.name = name;
	}

	/**
	 * Static helper function that determines and returns the marker's BitmapDescriptor color
	 * based off of the provided color.
	 *
	 * @param color The color to set the marker to (or something close to it).
	 * @return The resulting BitmapDescriptor. This will almost certainly not be the exact color,
	 * but rather will be something close to it.
	 */
	@NotNull
	private static com.google.android.gms.maps.model.BitmapDescriptor getMarkerIcon(int color) {
		float[] hsv = new float[3];
		android.graphics.Color.colorToHSV(color, hsv);
		return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0]);
	}

	/**
	 * Adds a marker to the map. Note that this method does not save the marker to the marked object.
	 * It only adds it to the map, and returns the newly added marker.
	 *
	 * @param map         The map to add the marker to.
	 * @param coordinates The LatLng coordinates of the marker.
	 * @param color       The color of the marker.
	 *                    This will try to get the closest approximation to the color as there are a limited number of marker colors.
	 * @return The newly added marker.
	 */
	public Marker addMarker(@NotNull com.google.android.gms.maps.GoogleMap map,
	                        com.google.android.gms.maps.model.LatLng coordinates, int color) {

		// Create a new maker options object
		MarkerOptions options = new MarkerOptions();

		// Set the position of the marker via the latitude and longitude.
		options.position(coordinates);

		// Set the color of the marker.
		Log.d("addMarker", "Applying marker color");
		options.icon(MarkedObject.getMarkerIcon(color));

		// Add the marker to the map.
		Log.d("addMarker", "Adding marker to the map");
		Marker marker = map.addMarker(options);

		// Set the marker title.
		Log.d("addMarker", "Setting marker title to: " + this.name);
		marker.setTitle(this.name);

		// Set the marker's tag.
		Log.d("addMarker", "Setting the markers tag to: " + this.getClass());
		marker.setTag(this);

		// Return the generated marker.
		return marker;
	}
}
