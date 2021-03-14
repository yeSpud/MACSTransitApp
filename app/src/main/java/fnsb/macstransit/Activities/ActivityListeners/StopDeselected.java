package fnsb.macstransit.Activities.ActivityListeners;

import fnsb.macstransit.RouteMatch.MarkedObject;

/**
 * Created by Spud on 2019-11-11 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2.
 * @since Beta 7.
 */
@androidx.annotation.UiThread
public class StopDeselected implements com.google.android.gms.maps.GoogleMap.OnInfoWindowCloseListener {

	/**
	 * Called when the marker's info window is closed.
	 * <p>
	 * This is called on the Android UI thread.
	 *
	 * @param marker The marker of the info window that was closed.
	 */
	@Override
	public void onInfoWindowClose(@androidx.annotation.NonNull com.google.android.gms.maps.model.Marker marker) {

		// Get the tag as a marked object for easier lookup.
		MarkedObject potentialStop = (MarkedObject) marker.getTag();

		// Check if it was a stop info window that was closed.
		if (potentialStop instanceof fnsb.macstransit.RouteMatch.Stop ||
				potentialStop instanceof fnsb.macstransit.RouteMatch.SharedStop) {

			// Just hide the marker, since we don't want to destroy it just yet.
			marker.setVisible(false);
		} else {

			// Log that the info window that was closed was neither a Stop nor a SharedStop.
			android.util.Log.w("onInfoWindowClose", "Unhandled info window");
		}
	}
}
