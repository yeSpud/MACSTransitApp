package fnsb.macstransit.Activities.ActivityListeners.Async;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import fnsb.macstransit.Activities.ActivityListeners.StopClicked;
import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class GetStopTimes extends android.os.AsyncTask<Stop, Void, JSONObject> {

	/**
	 * TODO Documentation
	 */
	private Marker marker;

	/**
	 * TODO Documentation
	 */
	private String expectedArrival, expectedDeparture;

	/**
	 * TODO Documentation
	 */
	private boolean is24Hour;


	/**
	 * TODO Documentation
	 */
	public GetStopTimes(Marker marker, MapsActivity activity) {
		this.marker = marker;
		this.is24Hour = android.text.format.DateFormat.is24HourFormat(activity);
		this.expectedArrival = activity.getString(R.string.expected_arrival);
		this.expectedDeparture = activity.getString(R.string.expected_departure);
	}

	/**
	 * TODO Documentation
	 *
	 * @param stops
	 * @return
	 */
	@Override
	protected JSONObject doInBackground(Stop... stops) {
		return MapsActivity.routeMatch.getStop(stops[0]);
	}

	/**
	 * TODO Documentation
	 *
	 * @param result
	 */
	@Override
	protected void onPostExecute(JSONObject result) {
		if (this.marker != null) {
			this.marker.setSnippet(StopClicked.getStopTime((Stop) marker.getTag(), result, is24Hour,
					expectedArrival, expectedDeparture));
			this.marker.showInfoWindow();
		}
	}
}
