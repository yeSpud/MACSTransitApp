package fnsb.macstransit.Activities;


/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.1.
 */
@Deprecated
public class StreetViewActivity extends androidx.fragment.app.FragmentActivity implements
		com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback {

	/**
	 * The marker that was clicked on.
	 */
	public static com.google.android.gms.maps.model.Marker marker;

	@Override
	protected void onCreate(android.os.Bundle savedInstance) {
		super.onCreate(savedInstance);
		this.setContentView(fnsb.macstransit.R.layout.streetview);
	}

	@Override
	public void onStreetViewPanoramaReady(com.google.android.gms.maps.StreetViewPanorama streetViewPanorama) {
		if (StreetViewActivity.marker != null) {
			streetViewPanorama.setPosition(StreetViewActivity.marker.getPosition());
		}
	}
}
