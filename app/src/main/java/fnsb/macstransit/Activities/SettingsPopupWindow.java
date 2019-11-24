package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fnsb.macstransit.R;

/**
 * Created by Spud on 2019-11-24 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class SettingsPopupWindow extends AlertDialog {

	/**
	 * TODO Documentation
	 */
	public static boolean ENABLE_TRAFFIC_VIEW, DEFAULT_NIGHT_MODE, SHOW_POLYLINES;

	private Context context;

	public SettingsPopupWindow(Context context) {
		super(context);
		this.context = context;
	}

	/**
	 * TODO Documentation
	 */
	public static void loadSettings() {
		// TODO
	}

	/**
	 * TODO Documentation
	 */
	public static void writeSettings() {
		// TODO
	}

	/**
	 * TODO Documentation
	 */
	public void showSettingsPopup() {
		ViewGroup viewGroup = this.findViewById(R.id.content);
		View dialogView = LayoutInflater.from(this.context).inflate(R.layout.settings_popup, viewGroup, false);

		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setView(dialogView);
		AlertDialog alertDialog = builder.create();

		// Setup the cancel button
		dialogView.findViewById(R.id.cancel).setOnClickListener((click) -> alertDialog.cancel());

		alertDialog.show();
	}

}
