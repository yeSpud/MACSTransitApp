package fnsb.macstransit.Activities.ActivityListeners;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Release 1.1.
 */
public class StreetViewListener implements GoogleMap.OnInfoWindowLongClickListener, OnStreetViewPanoramaReadyCallback {

	/**
	 * TODO Documentation
	 */
	private Context context;

	/**
	 * TODO Documentation
	 */
	private Marker marker;

	/**
	 * TODO Documentation
	 *
	 * @param context
	 */
	public StreetViewListener(Context context) {
		this.context = context;
	}

	@Override
	public void onInfoWindowLongClick(Marker marker) {
		this.marker = marker;
	}

	@Override
	public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
		if (this.marker != null) {
			streetViewPanorama.setPosition(this.marker.getPosition());
		}
	}

}
