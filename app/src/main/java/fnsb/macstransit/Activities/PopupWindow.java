package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;

import com.google.android.gms.maps.model.Marker;

import java.util.Locale;

import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Bus;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
 * @since Beta 8.
 */
public class PopupWindow extends AlertDialog implements
		com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener {

	/**
	 * The body text of the popup window.
	 */
	public static String body;

	/**
	 * Constructor for PopupWindow. All that's needed for this is the application context.
	 * This can be achieved by simply passing `this` as the parameter in the activity class.
	 *
	 * @param context The application context.
	 */
	@SuppressWarnings("WeakerAccess")
	public PopupWindow(Context context) {
		super(context);
	}

	/**
	 * Called when the marker's info window is clicked.
	 * <p>
	 * This is called on the Android UI thread.
	 * <p>
	 * For this use case we simply want to show the popup window dialog.
	 *
	 * @param marker The marker of the info window that was clicked.
	 */
	@Override
	public void onInfoWindowClick(Marker marker) {
		this.showDialog(marker.getTitle(), marker);
	}

	/**
	 * Shows the popup window dialog.
	 *
	 * @param marker The marker this popup dialog belongs to.
	 * @param title  The title of the popup.
	 */
	private void showDialog(String title, Marker marker) {

		Context context = this.getContext();

		// First, find the dialog view via the layout inflater, and inflate the info_window_popup layout.
		android.view.View dialogView = android.view.LayoutInflater.from(context)
				.inflate(R.layout.info_window_popup, this.findViewById(android.R.id.content), false);

		// Then, find the title and content textViews in the dialog view.
		android.widget.TextView titleView = dialogView.findViewById(R.id.title),
				content = dialogView.findViewById(R.id.body);

		// Set the title to the provided title string.
		titleView.setText(title);

		// Check the marker instance to determine the content text.
		// If its a stop or shared stop, just set it to the body.
		if (marker.getTag() instanceof fnsb.macstransit.RouteMatch.Stop || marker.getTag()
				instanceof fnsb.macstransit.RouteMatch.SharedStop) {
			content.setText(PopupWindow.body);
		} else if (marker.getTag() instanceof Bus) {
			// Since the instance is that of a bus, set the content to the heading, speed, and current capacity.
			Bus bus = (Bus) marker.getTag();
			StringBuilder builder = new StringBuilder();

			// Make sure to set the heading if it exists, and format it to be all lower case, except the first character.
			if (!bus.heading.equals("")) {
				String lowercaseHeading = bus.heading.toLowerCase();
				builder.append(String.format("Heading: %s\n", lowercaseHeading.substring(0, 1).toUpperCase() + lowercaseHeading.substring(1)));
			}

			// Append the speed in mph.
			builder.append(String.format(Locale.ENGLISH, "Speed: %d mph\n", bus.speed));


			// Then set the text to the determined content.
			content.setText(builder.toString());
		}

		// Create the alert dialog based off of the dialog view.
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(dialogView);
		AlertDialog alertDialog = builder.create();

		// Setup the close button to simply close the dialog.
		dialogView.findViewById(R.id.close_popup).setOnClickListener((click) -> alertDialog.cancel());

		// Show the dialog.
		alertDialog.show();
	}
}
