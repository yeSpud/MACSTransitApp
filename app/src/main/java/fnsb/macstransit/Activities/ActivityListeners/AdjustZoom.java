package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-28 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0
 * @since Beta 7.
 */
public class AdjustZoom implements com.google.android.gms.maps.GoogleMap.OnCameraIdleListener {

	/**
	 * Constant used for calculating new circle sizes.
	 */
	@Deprecated
	private static final float zoomConstant = 11.0f;

	/**
	 * TODO Documentation
	 * FIXME
	 */
	public static void resizeStops() {

		// Get the camera's new zoom position TODO
		float zoom = MapsActivity.map.getCameraPosition().zoom;
		double lat = MapsActivity.map.getCameraPosition().target.latitude;
		double metersPerPixel = 156543.03392 * Math.cos(lat * Math.PI / 180.0) / Math.pow(2, zoom);
		Log.v("onCameraIdle", "Meters / Pixel: " + metersPerPixel);

		// TODO
		for (Route route : MapsActivity.allRoutes) {
			for (Stop stop : route.stops) {
				if (stop.circle != null) {
					stop.circle.setRadius(metersPerPixel * 4);
				}
			}
		}

		// TODO Shared stops
	}

	/**
	 * Called when camera movement has ended,
	 * there are no pending animations and the user has stopped interacting with the map.
	 * <p>
	 * This is called on the Android UI thread.
	 */
	@Override
	public void onCameraIdle() {

		// TODO
		AdjustZoom.resizeStops();
	}
}
