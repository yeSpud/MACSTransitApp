package fnsb.macstransit.Activities.ActivityListeners.Async;

import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import fnsb.macstransit.Activities.ActivityListeners.StopClicked;
import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-23 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class GetSharedStopTimes extends android.os.AsyncTask<SharedStop, Void, JSONObject[]> {

	/**
	 * TODO Documentation
	 */
	private Marker marker;

	/**
	 * TODO Documentation
	 */
	private String expectedArrival, expectedDeparture;

	private boolean is24Hour;


	public GetSharedStopTimes(Marker marker, MapsActivity activity) {
		this.marker = marker;
		this.is24Hour = android.text.format.DateFormat.is24HourFormat(activity);
		this.expectedArrival = activity.getString(R.string.expected_arrival);
		this.expectedDeparture = activity.getString(R.string.expected_departure);
	}

	/**
	 * TODO Documentation
	 *
	 * @param sharedStops
	 * @return
	 */
	@Override
	protected JSONObject[] doInBackground(SharedStop... sharedStops) {
		SharedStop sharedStop = sharedStops[0];
		JSONObject[] json = new JSONObject[sharedStop.routes.length];
		for (int index = 0; index < json.length; index++) {
			Route route = sharedStop.routes[index];

			Stop stop = null;
			for (Stop stops : route.stops) {
				if (stops.stopID.equals(sharedStop.stopID)) {
					stop = stops;
					break;
				}
			}

			if (stop != null) {
				json[index] = MapsActivity.routeMatch.getStop(stop);
			}
		}
		return json;
	}

	/**
	 * TODO Documentation
	 *
	 * @param results
	 */
	@Override
	protected void onPostExecute(JSONObject[] results) {
		if (this.marker != null) {
			this.marker.setSnippet(StopClicked.getSharedStopTimes((SharedStop) marker.getTag(),
					results, is24Hour, expectedArrival, expectedDeparture));
			this.marker.showInfoWindow();
		}
	}
}
