package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

import com.google.android.gms.maps.model.Circle;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-28 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 * <p>
 * This is used to adjust the circle sizes of the stops and shared stops when the zoom level is changed by the user.
 *
 * @version 1.4
 * @since Beta 7.
 */
public class AdjustZoom implements com.google.android.gms.maps.GoogleMap.OnCameraIdleListener { // FIXME

	/**
	 * Constant used for calculating new circle sizes.
	 */
	private static final float zoomConstant = 11.0f;

	/**
	 * TODO Documentation
	 *
	 * @param zoomLevel
	 */
	public static void adjustCircleSize(float zoomLevel) {
		// Get how much it has changed from the default zoom (11).
		float zoomChange = AdjustZoom.zoomConstant / zoomLevel;

		// Iterate through all the routes.
		if (MapsActivity.allRoutes != null) {
			for (fnsb.macstransit.RouteMatch.Route route : MapsActivity.allRoutes) {
				// If the route isn't null, execute the following:
				if (route != null) {
					// Iterate through all the stops in the route.
					for (Stop stop : route.stops) {

						// If the circle isn't null, change its radius in proportion to the zoom change.
						if (stop.circle != null) {
							AdjustZoom.adjustParentCircleSize(zoomChange, stop.circle);
						}
					}
				}
			}
			// TODO Shared stops!
		}
	}

	/**
	 * Adjusts the circle size of the parent circle.
	 *
	 * @param zoomChange The value representing how much the view has changed relative to that of the original zoom level.
	 * @param circle     The parent circle.
	 */
	private static void adjustParentCircleSize(float zoomChange, Circle circle) {
		// Calculate the new size of the parent circle.
		double size = circle.getRadius() * (Math.pow(zoomChange, 6)); // TODO Look into changing the rate of adjustment.

		// Set the parent circle size.
		circle.setRadius(size);
	}

	/**
	 * Called when camera movement has ended,
	 * there are no pending animations and the user has stopped interacting with the map.
	 * <p>
	 * This is called on the Android UI thread.
	 */
	@Override
	public void onCameraIdle() {
		// Get the camera's new zoom position
		float zoom = MapsActivity.map.getCameraPosition().zoom;

		// Adjust the circle size based on zoom level
		try {
			AdjustZoom.adjustCircleSize(zoom);
		} catch (NullPointerException e) {
			Log.w("onCameraIdle", "Routes are null!");
		}
	}
}
