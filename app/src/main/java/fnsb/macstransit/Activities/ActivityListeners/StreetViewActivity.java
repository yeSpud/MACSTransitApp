package fnsb.macstransit.Activities.ActivityListeners;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.R;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.1.
 */
@Deprecated
public class StreetViewActivity extends FragmentActivity implements
		com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback {

	/**
	 * TODO Documentation
	 */
	public static Marker marker;

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		this.setContentView(R.layout.streetview);
	}

	@Override
	public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
		if (StreetViewActivity.marker != null) {
			streetViewPanorama.setPosition(StreetViewActivity.marker.getPosition());
		}
	}
}
