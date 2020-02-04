package fnsb.macstransit.Activities.ActivityListeners;

import android.content.Intent;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.1.
 */
public class StreetViewListener implements GoogleMap.OnInfoWindowLongClickListener {

	/**
	 * TODO Documentation
	 */
	private MapsActivity activity;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public StreetViewListener(MapsActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onInfoWindowLongClick(Marker marker) {
		if (marker != null) {
			StreetViewActivity.marker = marker;
			this.activity.startActivity(new Intent(this.activity, StreetViewActivity.class));
		}
	}
}
