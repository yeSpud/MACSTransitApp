package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.routematch.SharedStop;

/**
 * Created by Spud on 2019-10-28 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Beta 7.
 */
@androidx.annotation.UiThread
public class AdjustZoom implements com.google.android.gms.maps.GoogleMap.OnCameraIdleListener {

	/**
	 * Resizes the stop and shared stop circles on the map.
	 * This works regardless of whether or not a particular route is enabled or disabled.
	 */
	public static void resizeStops() {

		// Make sure the map is not null before continuing.
		if (MapsActivity.map == null) {
			return;
		}

		/*
		 * Calculate meters per pixel.
		 * This will be used to determine the circle size as we want it it be 4 meters in size.
		 * To calculate this we will need the current zoom as well as the cameras latitude.
		 */
		float zoom = MapsActivity.map.getCameraPosition().zoom;
		double lat = MapsActivity.map.getCameraPosition().target.latitude;

		// With the zoom and latitude determined we can then calculate meters per pixel.
		@SuppressWarnings("MagicNumber")
		double metersPerPixel = 156543.03392 * Math.cos(lat * Math.PI / 180.0) / Math.pow(2, zoom);
		Log.v("resizeStops", "Meters / Pixel: " + metersPerPixel);

		// Check that there are routes to iterate though.
		if (MapsActivity.allRoutes != null) {

			// Get the size of the circle to resize to.
			double size = metersPerPixel * 4;
			Log.d("resizeStops", String.format("Setting circle size to: %f", metersPerPixel * 4));

			// Iterate though each route.
			for (fnsb.macstransit.routematch.Route route : MapsActivity.allRoutes) {

				// Start by resizing the stop circles first.
				for (fnsb.macstransit.routematch.Stop stop : route.getStops()) {
					if (stop.circle != null) {
						stop.circle.setRadius(size);
					}
				}

				// Then resize the route's shared stop circles.
				SharedStop[] sharedStops = route.getSharedStops();
				if (sharedStops.length != 0) {
					for (SharedStop sharedStop : sharedStops) {
						sharedStop.setCircleSizes(size);
					}
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

		// Simply call the resize function.
		AdjustZoom.resizeStops();
	}
}
