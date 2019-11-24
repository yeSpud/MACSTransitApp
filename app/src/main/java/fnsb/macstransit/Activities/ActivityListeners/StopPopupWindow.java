package fnsb.macstransit.Activities.ActivityListeners;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import fnsb.macstransit.R;

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
		this.showDialog(marker.getTitle());
	}

	private void showDialog(String title) {
		ViewGroup viewGroup = this.findViewById(android.R.id.content);

		View dialogView = LayoutInflater.from(this.context).inflate(R.layout.stop_popup, viewGroup, false);

		TextView titleView = dialogView.findViewById(R.id.title), content = dialogView.findViewById(R.id.body);

		Button closeButton = dialogView.findViewById(R.id.close_popup);

		titleView.setText(title);
		content.setText(StopPopupWindow.body);

		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);

		builder.setView(dialogView);

		AlertDialog alertDialog = builder.create();

		closeButton.setOnClickListener((click) -> alertDialog.cancel());

		alertDialog.show();
	}
}
