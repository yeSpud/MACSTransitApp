package fnsb.macstransit.ActivityListeners;

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

}
