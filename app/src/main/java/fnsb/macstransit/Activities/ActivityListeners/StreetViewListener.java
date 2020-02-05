package fnsb.macstransit.Activities.ActivityListeners;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.Activities.StreetViewActivity;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.1.
 */
@Deprecated
public class StreetViewListener implements
		com.google.android.gms.maps.GoogleMap.OnInfoWindowLongClickListener {

	/**
	 * The activity that this listener belongs to.
	 */
	private MapsActivity activity;

	/**
	 * Creates a listener to launch the street view activity from the Maps activity.
	 *
	 * @param activity The Maps activity.
	 */
	public StreetViewListener(MapsActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onInfoWindowLongClick(com.google.android.gms.maps.model.Marker marker) {
		if (marker != null) {
			StreetViewActivity.marker = marker;
			this.activity.startActivity(new android.content.Intent(this.activity, StreetViewActivity.class));
		}
	}
}
