package fnsb.macstransit.Activities.ActivityListeners.Async;

import org.json.JSONObject;

/**
 * Created by Spud on 2021-02-10 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0.
 * @since Release 1.2.
 */
public class StopTimeCallback {

	/**
	 * TODO Documentation
	 */
	private final AsyncCallback callback;

	/**
	 * TODO Documentation
	 * @param callback
	 */
	public StopTimeCallback(AsyncCallback callback) {
		this.callback = callback;
	}

	/**
	 * TODO Documentation
	 * @param stopName
	 */
	public void retrieveStopTime(String stopName) {

		Thread async = new Thread(() -> {
			JSONObject departures = fnsb.macstransit.Activities.MapsActivity.routeMatch.getDeparturesByStop(stopName);

			if (callback != null) {

				callback.receivedStopTime(departures);

			} else {

				android.util.Log.w("StopTimeCallback", "Callback method was never set!");
			}
		});

		async.setName("StopTimeCallback");
		async.start();
	}


	public interface AsyncCallback {

		/**
		 * TODO Documentation
		 * @param departures
		 */
		void receivedStopTime(JSONObject departures);

	}
}
