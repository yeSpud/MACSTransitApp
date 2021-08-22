package fnsb.macstransit.activities

import android.app.AlertDialog
import fnsb.macstransit.databinding.InfoWindowPopupBinding
import fnsb.macstransit.routematch.Bus

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0.
 * @since Beta 8.
 */
@androidx.annotation.UiThread
class PopupWindow(context: android.content.Context) : AlertDialog(context),
		com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener {

	/**
	 * Called when the marker's info window is clicked.
	 * This is called on the Android UI thread.
	 * For this use case we simply want to show the popup window dialog.
	 *
	 * @param marker The marker of the info window that was clicked.
	 */
	override fun onInfoWindowClick(marker: com.google.android.gms.maps.model.Marker) {

		// Comments
		val binder: InfoWindowPopupBinding = InfoWindowPopupBinding.inflate(this.layoutInflater)

		// Comments
		binder.title.text = marker.title

		// Check the marker instance to determine the content text.
		// If its a stop or shared stop, just set it to the body.
		if (marker.tag is fnsb.macstransit.routematch.Stop || marker.tag is fnsb.macstransit.routematch.SharedStop) {
			binder.body.text = body
		} else if (marker.tag is Bus) {

			// Since the instance is that of a bus, set the content to the heading, speed,
			// and current capacity.
			val bus: Bus = marker.tag as Bus
			val builder = StringBuilder()

			// Make sure to set the heading if it exists, and format it to be all lower case,
			// except the first character.
			var heading: String? = bus.heading
			if (heading != null && heading.isEmpty()) {
				heading = heading.lowercase()
				builder.append("Heading: ${heading.replaceFirstChar {
						if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
					}}\n")
			}

			// Append the speed in mph if its not 0.
			if (bus.speed != 0) {
				builder.append("Speed: ${bus.speed} mph\n")
			}

			// Then set the text to the determined content.
			binder.body.text = builder.toString()
		}

		// Create the alert dialog based off of the dialog view.
		val builder = Builder(this.context)
		builder.setView(binder.root)
		val alertDialog: AlertDialog = builder.create()

		// Setup the close button to simply close the dialog.
		binder.closePopup.setOnClickListener { alertDialog.cancel() }

		// Show the dialog.
		alertDialog.show()
	}

	companion object {

		/**
		 * The body text of the popup window.
		 */
		var body: String? = null
	}
}