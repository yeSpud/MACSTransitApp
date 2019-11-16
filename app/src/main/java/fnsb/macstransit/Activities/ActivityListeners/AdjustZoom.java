package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

import com.google.android.gms.maps.model.Circle;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-28 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
 * @since Beta 7
 */
public class AdjustZoom implements com.google.android.gms.maps.GoogleMap.OnCameraIdleListener {

	/**
	 * The MapsActivity that this listener will apply to.
	 * This is used to get access to all the public variables.
	 */
	private MapsActivity activity;

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
	 *                    It should be noted that the regular stops will be adjusted on their own,
	 *                    and do not need to be passed as an argument.
	 */
	public static void adjustCircleSize(float zoomLevel, SharedStop[] sharedStops) {
		// Get how much it has changed from the default zoom (11).
		float zoomChange = 11.0f / zoomLevel;
		Log.d("CameraChange", "Zoom change: " + zoomChange);

		// Iterate through all the routes.
		for (fnsb.macstransit.RouteMatch.Route route : MapsActivity.allRoutes) {
			// If the route isn't null, execute the following:
			if (route != null) {
				// Iterate through all the stops in the route.
				for (Stop stop : route.stops) {
					// Get the stop's icon
					Circle icon = stop.getIcon();
					// If the icon isn't null, change its radius in proportion to the zoom change.
					if (icon != null) {
						icon.setRadius(Stop.RADIUS * (Math.pow(zoomChange, 6)));
					}
				}
			}
		}

		// Iterate through all the shared stops.
		for (SharedStop sharedStop : sharedStops) {
			// Get the circles from the shared stop
			Circle[] circles = sharedStop.getCircles();

			// Iterate through all the circles to adjust their radius.
			for (int index = 0; index < sharedStop.routes.length; index++) {
				Circle c = circles[index];
				if (c != null) {
					c.setRadius((Stop.RADIUS * (1d / (index + 1))) * Math.pow(zoomChange, 6));
				}
			}
		}
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
		float zoom = this.activity.map.getCameraPosition().zoom;
		Log.d("CameraChange", "Zoom level: " + zoom);

		// Adjust the circle size based on zoom level
		AdjustZoom.adjustCircleSize(zoom, this.activity.sharedStops);
	}
}
