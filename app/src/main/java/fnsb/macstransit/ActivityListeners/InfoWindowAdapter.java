package fnsb.macstransit.ActivityListeners;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.MapsActivity;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class InfoWindowAdapter implements com.google.android.gms.maps.GoogleMap.InfoWindowAdapter {

	/**
	 * TODO Documentation
	 */
	private MapsActivity activity;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public InfoWindowAdapter(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * TODO Documentation
	 *
	 * @param marker
	 * @return
	 */
	@Override
	public View getInfoWindow(Marker marker) {
		return null;
	}

	/**
	 * TODO Documentation
	 * <p>
	 * https://stackoverflow.com/questions/13904651/android-google-maps-v2-how-to-add-marker-with-multiline-snippet
	 *
	 * @param marker
	 * @return
	 * @author Hiren Patel - https://stackoverflow.com/users/4233197/hiren-patel
	 */
	@Override
	public View getInfoContents(Marker marker) {
		LinearLayout info = new LinearLayout(this.activity);
		info.setOrientation(LinearLayout.VERTICAL);

		TextView title = new TextView(this.activity);
		title.setTextColor(Color.BLACK);
		title.setGravity(android.view.Gravity.CENTER);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		title.setText(marker.getTitle());

		TextView snippet = new TextView(this.activity);
		snippet.setTextColor(Color.GRAY);
		snippet.setText(marker.getSnippet());
		snippet.setMaxLines(15); // TODO Make this scrollable

		info.addView(title);
		if (!snippet.getText().equals("") && snippet.getText() != null) {
			info.addView(snippet);
		}
		return info;
	}
}
