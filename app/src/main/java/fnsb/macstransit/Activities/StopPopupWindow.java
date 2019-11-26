package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import java.util.Locale;

import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class StopPopupWindow extends AlertDialog implements GoogleMap.OnInfoWindowClickListener {

	/**
	 * TODO Documentation
	 */
	public static String body;

	/**
	 * TODO Documentation
	 */
	private Context context;

	/**
	 * TODO Documentation
	 *
	 * @param context
	 */
	public StopPopupWindow(Context context) {
		super(context);
		this.context = context;
	}

	/**
	 * TODO Documentation
	 *
	 * @param marker
	 */
	@Override
	public void onInfoWindowClick(Marker marker) {
		this.showDialog(marker.getTitle(), marker);
	}

	/**
	 * TODO Documentation
	 *
	 * @param marker
	 * @param title
	 */
	private void showDialog(String title, Marker marker) {
		ViewGroup viewGroup = this.findViewById(android.R.id.content);
		View dialogView = LayoutInflater.from(this.context)
				.inflate(R.layout.stop_popup, viewGroup, false);
		TextView titleView = dialogView.findViewById(R.id.title),
				content = dialogView.findViewById(R.id.body);
		Button closeButton = dialogView.findViewById(R.id.close_popup);
		titleView.setText(title);
		if (marker.getTag() instanceof Stop || marker.getTag() instanceof SharedStop) {
			content.setText(StopPopupWindow.body);
		} else if (marker.getTag() instanceof Bus) {
			Bus bus = (Bus) marker.getTag();
			StringBuilder builder = new StringBuilder();
			if (!bus.heading.equals("")) {
				String lowercaseHeading = bus.heading.toLowerCase();
				builder.append(String.format("Heading: %s\n", lowercaseHeading.substring(0, 1).toUpperCase() + lowercaseHeading.substring(1)));
			}
			builder.append(String.format(Locale.ENGLISH, "Speed: %d mph\n", bus.speed));
			builder.append(String.format(Locale.ENGLISH, "Current capacity: %d\n",
					bus.currentCapacity));
			content.setText(builder.toString());
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setView(dialogView);
		AlertDialog alertDialog = builder.create();
		closeButton.setOnClickListener((click) -> alertDialog.cancel());
		alertDialog.show();
	}
}
