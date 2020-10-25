package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

import com.google.android.gms.maps.model.Circle;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.BasicStop;
import fnsb.macstransit.RouteMatch.SharedStop;
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
public class AdjustZoom implements com.google.android.gms.maps.GoogleMap.OnCameraIdleListener {

	/**
	 * Constant used for calculating new circle sizes.
	 */
	private static final float zoomConstant = 11.0f;

	/**
	 * The MapsActivity that this listener will apply to.
	 * This is used to get access to all the public variables within the class.
	 */
	private final MapsActivity activity;

	/**
	 * Constructor for the listener.
	 *
	 * @param activity The MapsActivity that will be using this listener (just pass {@code this} as the argument in the activity).
	 */
	public AdjustZoom(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * Adjusts the circle size based on the current zoom level.
	 *
	 * @param zoomLevel   The current zoom level.
	 * @param sharedStops The array of shared stops to update the circles sizes to.
	 *                    It should be noted that the regular stops will be adjusted on their own
	 *                    (as those are declared as a static variable within the maps activity),
	 *                    and do not need to be passed as an argument.
	 */
	public static void adjustCircleSize(float zoomLevel, SharedStop[] sharedStops) {
		// Get how much it has changed from the default zoom (11).
		float zoomChange = AdjustZoom.zoomConstant / zoomLevel;

		// Iterate through all the routes.
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

		// Iterate through all the shared stops and execute the following:
		for (SharedStop sharedStop : sharedStops) {

			// FIXME

			// Get the rest of the circles from the shared stop.
			Circle[] circles = sharedStop.getCircles();

			// Iterate through all the circles and adjust their sizes.
			for (int index = 0; index < sharedStop.childRoutes.length; index++) {
				Circle circle = circles[index];
				if (circle != null) {
					// Calculate the new size of the parent circle.
					double size = (BasicStop.PARENT_RADIUS * (1d / (index + 2))) * Math.pow(zoomChange, 6);

					// Set the parent circle size.
					circle.setRadius(size);
				}
			}
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
			AdjustZoom.adjustCircleSize(zoom, this.activity.sharedStops);
		} catch (NullPointerException e) {
			Log.w("onCameraIdle", "Routes are null!");
		}
	}
}
