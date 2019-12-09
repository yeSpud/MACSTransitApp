package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;

/**
 * Created by Spud on 2019-11-11 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
 * @since Beta 7.
 */
public class StopDeselected implements com.google.android.gms.maps.GoogleMap.OnInfoWindowCloseListener {

	/**
	 * Called when the marker's info window is closed.
	 * <p>
	 * This is called on the Android UI thread.
	 *
	 * @param marker The marker of the info window that was closed.
	 */
	@Override
	public void onInfoWindowClose(com.google.android.gms.maps.model.Marker marker) {
		// Check if it was a stop info window that was closed.
		if (marker.getTag() instanceof fnsb.macstransit.RouteMatch.Stop ||
				marker.getTag() instanceof fnsb.macstransit.RouteMatch.SharedStop) {
			Log.d("onInfoWindowClose", "Closing stop window");
			marker.setVisible(false);
		} else {
			// Log that the info window that was closed was neither a Stop nor a SharedStop.
			Log.w("onInfoWindowClose", "Unhandled info window");
		}
	}
}
