package fnsb.macstransit.Activities

import android.content.Intent
import android.view.View
import fnsb.macstransit.R

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0.
 * @since Release 1.1.
 */
@androidx.annotation.UiThread
class FarePopupWindow(context: android.content.Context) : android.app.AlertDialog(context) {

	/**
	 * Creates and shows the popup window.
	 */
	fun showFarePopupWindow() {

		// Find and inflate the settings view.
		val dialogView = android.view.LayoutInflater.from(this.context)
				.inflate(R.layout.fares_popup, findViewById(R.id.content), false)

		// Setup the hyperlink.
		dialogView.findViewById<View>(R.id.link).setOnClickListener {
			this.context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(
					"https://www.fnsb.gov/352/Bus-Fares")))
		}

		// Create the dialog via the alert dialog builder.
		val builder = Builder(this.context)
		builder.setView(dialogView)
		val alertDialog = builder.create()

		// Setup the cancel button.
		dialogView.findViewById<View>(R.id.close).setOnClickListener { alertDialog.cancel() }

		// Show the dialog.
		alertDialog.show()
	}
}