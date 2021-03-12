package fnsb.macstransit.RouteMatch;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jetbrains.annotations.NotNull;


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
	@Nullable
	public Marker marker;

	/**
	 * The name / title / ID of the marked object.
	 */
	public final String name;

	/**
	 * TODO Documentation
	 * @param name
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
	 * TODO Documentation
	 * @param map
	 * @param coordinates
	 * @param color
	 * @param title
	 * @return
	 */
	public Marker addMarker(@NotNull GoogleMap map, LatLng coordinates, int color, String title) {
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
		Log.d("addMarker", "Setting marker title to: " + title);
		marker.setTitle(title);

		// Set the marker's tag.
		Log.d("addMarker", "Setting the markers tag to: " + this.getClass());
		marker.setTag(this);

		// Return the generated marker.
		return marker;
	}
}
