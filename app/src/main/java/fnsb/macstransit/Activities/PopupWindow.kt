package fnsb.macstransit.Activities

import android.app.AlertDialog
import android.widget.TextView
import fnsb.macstransit.R
import fnsb.macstransit.routematch.Bus
import java.util.*

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
	 *
	 *
	 * For this use case we simply want to show the popup window dialog.
	 *
	 * @param marker The marker of the info window that was clicked.
	 */
	override fun onInfoWindowClick(marker: com.google.android.gms.maps.model.Marker) {

		// Comments
		val context = this.context

		// First, find the dialog view via the layout inflater, and inflate the info_window_popup layout.
		val dialogView = android.view.LayoutInflater.from(context)
				.inflate(R.layout.info_window_popup, findViewById(android.R.id.content), false)

		// Then, find the title and content textViews in the dialog view.
		val titleView = dialogView.findViewById<TextView>(R.id.title)
		val content = dialogView.findViewById<TextView>(R.id.body)

		// Set the title to the provided title string.
		titleView.text = marker.title

		// Check the marker instance to determine the content text.
		// If its a stop or shared stop, just set it to the body.
		if (marker.tag is fnsb.macstransit.routematch.Stop || marker.tag is fnsb.macstransit.routematch.SharedStop) {
			content.text = body
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
				builder.append("Heading: ${
					heading.replaceFirstChar {
						if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
					}
				}\n")
			}

			// Append the speed in mph if its not 0.
			if (bus.speed != 0) {
				builder.append("Speed: ${bus.speed} mph\n")
			}

			// Then set the text to the determined content.
			content.text = builder.toString()
		}

		// Create the alert dialog based off of the dialog view.
		val builder = Builder(context)
		builder.setView(dialogView)
		val alertDialog: AlertDialog = builder.create()

		// Setup the close button to simply close the dialog.
		dialogView.findViewById<android.view.View>(R.id.close_popup)
				.setOnClickListener { alertDialog.cancel() }

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