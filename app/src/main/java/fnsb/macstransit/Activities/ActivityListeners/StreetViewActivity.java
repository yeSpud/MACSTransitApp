package fnsb.macstransit.Activities.ActivityListeners;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.1.
 */
public class StreetViewActivity extends FragmentActivity implements
		com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback {

	/**
	 * TODO Documentation
	 */
	public static Marker marker;

	@Override
	public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
		if (StreetViewActivity.marker != null) {
			streetViewPanorama.setPosition(StreetViewActivity.marker.getPosition());
		}
	}
}
