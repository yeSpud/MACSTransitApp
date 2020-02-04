package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;

import fnsb.macstransit.R;

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Release 1.1.
 */
@SuppressWarnings("WeakerAccess")
public class WarningPopup extends AlertDialog {

	/**
	 * Creates a popup window that shows a warning message if there are too many routes selected.
	 *
	 * @param context The context that is launching this popup window.
	 */
	public WarningPopup(Context context) {
		super(context);
	}

	/**
	 * Creates and shows the popup window.
	 */
	public void showWarningPopup() {

		Context context = this.getContext();

		// Find and inflate the settings view.
		android.view.View dialogView = android.view.LayoutInflater.from(context)
				.inflate(R.layout.warning, this.findViewById(R.id.content), false);

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
