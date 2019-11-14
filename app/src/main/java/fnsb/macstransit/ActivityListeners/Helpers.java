package fnsb.macstransit.ActivityListeners;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import fnsb.macstransit.RouteMatch.BasicStop;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class Helpers {

	/**
	 * Gets the color of the marker icon based off of the color value given.
	 * The reason why there needs to be a function for this is because there are only 10 colors that a marker icon can be.
	 *
	 * @param color The desired color value as an int.
	 * @return The BitmapDescriptor used for defining the color of a markers's icon.
	 */
	public static com.google.android.gms.maps.model.BitmapDescriptor getMarkerIcon(int color) {
		float[] hsv = new float[3];
		android.graphics.Color.colorToHSV(color, hsv);
		return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hsv[0]);
	}

	/**
	 * TODO Documentation and comments
	 *
	 * @param routes
	 * @return
	 */
	public static BasicStop[] loadAllStops(Route[] routes) {
		ArrayList<BasicStop> stops = new ArrayList<>();
		for (Route r : routes) {
			for (fnsb.macstransit.RouteMatch.Stop s : r.stops) {
				stops.add(new BasicStop(s.stopID, s.latitude, s.longitude, s.route));
			}
		}
		return stops.toArray(new BasicStop[0]);
	}

	/**
	 * TODO Documentation
	 *
	 * @param map
	 * @param options
	 * @param tag
	 * @param clickable
	 * @return
	 */
	public static Circle addCircle(GoogleMap map, CircleOptions options, Object tag, boolean clickable) {
		String status = "Creating circle";
		if (clickable) {
			status += ", and setting it to be clickable";
		}
		status += "...";
		Log.d("addCircle", status);
		Circle circle = map.addCircle(options);
		circle.setTag(tag);
		circle.setClickable(clickable);
		return circle;
	}

	/**
	 * TODO Documentation
	 *
	 * @param map
	 * @param latitude
	 * @param longitude
	 * @param color
	 * @param title
	 * @param tag
	 * @return
	 */
	public static Marker addMarker(GoogleMap map, double latitude, double longitude, int color, String title, Object tag) {
		MarkerOptions options = new MarkerOptions();
		options.position(new LatLng(latitude, longitude));
		options.icon(Helpers.getMarkerIcon(color));
		Marker marker = map.addMarker(options);
		marker.setTitle(title);
		marker.setTag(tag);
		return marker;
	}


}
