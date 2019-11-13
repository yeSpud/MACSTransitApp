package fnsb.macstransit.ActivityListeners;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.MapsActivity;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-11 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class StopDeselected implements GoogleMap.OnInfoWindowCloseListener {

	/**
	 * TODO Documentation
	 */
	private MapsActivity activity;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public StopDeselected(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * TODO Documentation
	 *
	 * @param marker
	 */
	@Override
	public void onInfoWindowClose(Marker marker) {
		if (marker.getTag() instanceof Stop) {
			Log.d("onInfoWindowClose", "Closing stop window");
			marker.setVisible(false);
		} else if (marker.getTag() instanceof SharedStop) {
			Log.d("onInfoWindowClose", "Closing shared stop window");
			marker.setVisible(false);
		} else {
			Log.w("onInfoWindowClose", "Unhandled info window");
		}
	}
}
