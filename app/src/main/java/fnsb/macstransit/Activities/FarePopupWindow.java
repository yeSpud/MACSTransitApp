package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import fnsb.macstransit.R;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.1
 * @since Release 1.1.
 */
public class FarePopupWindow extends AlertDialog {

	/**
	 * Creates a popup window that shows the bus fares.
	 *
	 * @param context The context that is launching this popup window.
	 */
	public FarePopupWindow(Context context) {
		super(context);
	}

	/**
	 * Creates and shows the popup window.
	 */
	public void showFarePopupWindow() {

		Context context = this.getContext();

		// Find and inflate the settings view.
		android.view.View dialogView = android.view.LayoutInflater.from(context)
				.inflate(R.layout.fares_popup, this.findViewById(R.id.content), false);

		// Setup the hyperlink.
		dialogView.findViewById(R.id.link).setOnClickListener((click) ->
				context.startActivity(new Intent(Intent.ACTION_VIEW,
						android.net.Uri.parse("https://www.fnsb.gov/352/Bus-Fares"))));

		// Create the dialog via the alert dialog builder.
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(dialogView);
		AlertDialog alertDialog = builder.create();

		// Setup the cancel button.
		dialogView.findViewById(R.id.close).setOnClickListener((click) -> alertDialog.cancel());

		// Show the dialog.
		alertDialog.show();
	}
}
