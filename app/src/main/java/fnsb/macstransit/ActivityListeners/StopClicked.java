package fnsb.macstransit.ActivityListeners;

import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fnsb.macstransit.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-10-30 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class StopClicked implements com.google.android.gms.maps.GoogleMap.OnCircleClickListener {

	/**
	 * TODO Documentation
	 */
	private MapsActivity activity;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public StopClicked(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * Called when a circle is clicked.
	 * <p>
	 * This is called on the Android UI thread.
	 *
	 * @param circle The circle that is clicked.
	 */
	@Override
	public void onCircleClick(com.google.android.gms.maps.model.Circle circle) {
		// Make sure that circle is part of the stop class
		if (circle.getTag() instanceof Stop) {
			// Make sure the circle is visible first
			Log.d("onCircleClick", "Showing stop infowindow");
			this.showStopInfoWindow((Stop) circle.getTag());
		} else if (circle.getTag() instanceof SharedStop) {
			Log.d("onCircleClick", "Showing sharedStop infowindow");
			this.showSharedStopInfoWindow((SharedStop) circle.getTag());
		} else {
			Log.w("onCircleClick", String.format("Circle object (%s) unaccounted for!", circle.getTag().toString()));
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @param stop
	 */
	private void showStopInfoWindow(Stop stop) {
		// If the stop doesn't have a marker, just use toast to display the stop ID.
		if (stop.getMarker() == null) {
			Toast.makeText(this.activity, stop.stopID, Toast.LENGTH_SHORT).show();
		} else {
			// If the stop does have a marker, set the marker to be visible, and show the info window corresponding to that marker.
			com.google.android.gms.maps.model.Marker marker = stop.getMarker();
			marker.setVisible(true);

			// Get the stops departures and arrivals
			// Example: url https://fnsb.routematch.com/feed/departures/byStop/484%20-%20Bentley%20Trust%20%40%20KFC
			// TODO Comments
			StringBuilder snippetText = new StringBuilder();
			JSONArray stopData = RouteMatch.parseData(MapsActivity.routeMatch.getStop(stop));
			int count = stopData.length();
			for (int index = 0; index < count && index < 2; index++) {
				try {
					Log.d("onCircleClick", String.format("Parsing stop times for stop %d/%d", index, count));
					JSONObject object = stopData.getJSONObject(index);

					Pattern timeRegex = Pattern.compile("\\d\\d:\\d\\d");

					Matcher arrivalRegex = timeRegex.matcher(object.getString("predictedArrivalTime")),
							departureRegex = timeRegex.matcher(object.getString("predictedDepartureTime"));

					if (arrivalRegex.find() && departureRegex.find()) {

						String arrivalTime, departureTime;

						if (DateFormat.is24HourFormat(this.activity)) {
							arrivalTime = arrivalRegex.group(0);
							departureTime = departureRegex.group(0);

						} else {
							try {
								SimpleDateFormat parser = new SimpleDateFormat("H:mm", Locale.US),
										formatter = new SimpleDateFormat("K:mm a", Locale.US);
								arrivalTime = formatter.format(parser.parse(arrivalRegex.group(0)));
								departureTime = formatter.format(parser.parse(departureRegex.group(0)));
							} catch (java.text.ParseException dateError) {
								arrivalTime = arrivalRegex.group(0);
								departureTime = departureRegex.group(0);
							}
						}

						snippetText.append(String.format("%s %s\n%s %s\n\n",
								this.activity.getString(R.string.expected_arrival),
								arrivalTime, this.activity.getString(R.string.expected_departure),
								departureTime));

					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			marker.setSnippet(snippetText.toString());
			marker.showInfoWindow();
		}
	}

	/**
	 * TODO Documentation
	 *
	 * @param sharedStop
	 */
	private void showSharedStopInfoWindow(SharedStop sharedStop) {
		// If the shared stop doesn't have a marker, just use toast to display the stop ID.
		if (sharedStop.getMarker() == null) {
			Toast.makeText(this.activity, sharedStop.stopID, Toast.LENGTH_SHORT).show();
		} else {
			// TODO
			Toast.makeText(this.activity, sharedStop.stopID, Toast.LENGTH_SHORT).show();
		}
	}
}
