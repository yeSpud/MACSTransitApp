package fnsb.macstransit.ActivityListeners;

import android.widget.Toast;

import fnsb.macstransit.MapsActivity;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class StopClicked implements com.google.android.gms.maps.GoogleMap.OnCircleClickListener {

	/**
	 * TODO Documentation
	 */
	private MapsActivity activity;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public StopClicked(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * TODO Documentation
	 *
	 * @param circle
	 */
	@Override
	public void onCircleClick(com.google.android.gms.maps.model.Circle circle) {
		// Make sure the circle is visible first
		if (circle.isVisible()) {

			// Make sure that circle is part of the stop class
			if (circle.getTag() instanceof Stop) {
				Stop stop = (Stop) circle.getTag();

				// If the stop doesn't have a marker, just use toast to display the stop ID.
				if (stop.getMarker() == null) {
					Toast.makeText(this.activity, stop.stopID, Toast.LENGTH_SHORT).show();
				} else {
					// If the stop does have a marker, set the marker to be visible, and show the info window corresponding to that marker.
					com.google.android.gms.maps.model.Marker marker = stop.getMarker();
					marker.setVisible(true);
					marker.showInfoWindow();
				}
			}
		}
	}
}
