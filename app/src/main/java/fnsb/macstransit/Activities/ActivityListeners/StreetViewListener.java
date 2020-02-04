package fnsb.macstransit.Activities.ActivityListeners;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.R;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Release 1.1.
 */
public class StreetViewListener extends FragmentActivity implements
		GoogleMap.OnInfoWindowLongClickListener, com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback {

	/**
	 * TODO Documentation
	 */
	private MapsActivity activity;

	/**
	 * TODO Documentation
	 */
	private Marker marker;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public StreetViewListener(MapsActivity activity) {
		this.activity = activity;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.streetview);
	}

	@Override
	public void onInfoWindowLongClick(Marker marker) {
		this.marker = marker;

		this.startActivity(new Intent(this, StreetViewListener.class));
	}

	@Override
	public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
		if (this.marker != null) {
			streetViewPanorama.setPosition(this.marker.getPosition());
		}
	}

}
