package fnsb.macstransit.activities.mapsactivity.mappopups

import android.app.AlertDialog
import android.content.Intent
import fnsb.macstransit.databinding.FaresPopupBinding

/**
 * Created by Spud on 2020-02-03 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0.
 * @since Release 1.1.
 */
@androidx.annotation.UiThread
class FarePopupWindow(activity: fnsb.macstransit.activities.mapsactivity.MapsActivity) : AlertDialog(activity) {

	/**
	 * Creates and shows the popup window.
	 */
	fun showFarePopupWindow() {

		// Get the binder for the fares popup window.
		val binder: FaresPopupBinding = FaresPopupBinding.inflate(this.layoutInflater)

		// Setup the hyperlink to open the bus fares page.
		binder.link.setOnClickListener {
			this.context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(
					"https://www.fnsb.gov/352/Bus-Fares")))
		}

		// Create the dialog via the alert dialog builder.
		val builder = Builder(this.context)
		builder.setView(binder.root)
		val alertDialog: AlertDialog = builder.create()

		// Setup the cancel button.
		binder.close.setOnClickListener { alertDialog.cancel() }

		// Show the dialog.
		alertDialog.show()
	}
}