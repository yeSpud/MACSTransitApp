package fnsb.macstransit.Activities.ActivityListeners;

/**
 * Created by Spud on 2019-11-11 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.2
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
	public void onInfoWindowClose(@org.jetbrains.annotations.NotNull
			                                  com.google.android.gms.maps.model.Marker marker) {
		// Check if it was a stop info window that was closed.
		if (marker.getTag() instanceof fnsb.macstransit.RouteMatch.Stop ||
				marker.getTag() instanceof fnsb.macstransit.RouteMatch.SharedStop) {
			marker.setVisible(false);
		} else {
			// Log that the info window that was closed was neither a Stop nor a SharedStop.
			android.util.Log.w("onInfoWindowClose", "Unhandled info window");
		}
	}
}
