package fnsb.macstransit.ActivityListeners;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fnsb.macstransit.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.RouteMatch;
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
	 * TODO Documentation
	 *
	 * @param circle
	 */
	@Override
	public void onCircleClick(com.google.android.gms.maps.model.Circle circle) {
		// Make sure the circle is visible first
		if (circle.isVisible()) {

			// Make sure that circle is part of the stop class
			if (circle.getTag() instanceof Stop) {
				Stop stop = (Stop) circle.getTag();

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
					JSONArray stopData = RouteMatch.parseData(this.activity.routeMatch.getStop(stop));
					int count = stopData.length();
					for (int index = 0; index < count; index++) {
						try {

							Log.d("onCircleClick", String.format("Parsing stop times for stop %d/%d", index, count));
							JSONObject object = stopData.getJSONObject(index);

							Pattern timeRegex = Pattern.compile("\\d\\d:\\d\\d");

							String arrival = object.getString("predictedArrivalTime"), departure = object.getString("predictedDepartureTime");
							Matcher arrivalRegex = timeRegex.matcher(arrival), departureRegex = timeRegex.matcher(departure);
							if (arrivalRegex.find() && departureRegex.find()) {
								snippetText.append(String.format("%s%s\n%s%s\n\n",
										this.activity.getString(R.string.expected_arrival), arrivalRegex.group(0),
										this.activity.getString(R.string.expected_departure), departureRegex.group(0)));
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					marker.setSnippet(snippetText.toString());

					marker.showInfoWindow();
				}
			}
		}
	}
}
