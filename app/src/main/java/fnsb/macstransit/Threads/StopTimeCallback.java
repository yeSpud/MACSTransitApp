package fnsb.macstransit.Threads;

import org.json.JSONObject;

/**
 * Created by Spud on 2021-02-10 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.2.
 */
public class StopTimeCallback {

	/**
	 * The callback used once stop times have been retrieved from the RouteMatch server.
	 */
	private final AsyncCallback callback;

	/**
	 * Constructor for the StopTimeCallback class.
	 * This class itself is not a callback, but rather is the class that runs the callback when ready.
	 *
	 * @param callback The callback to be used once the stop times have been retrieved.
	 */
	public StopTimeCallback(AsyncCallback callback) {
		this.callback = callback;
	}

	/**
	 * Retrieves the stop time information for the provided stop from the RouteMatch server.
	 *
	 * @param stopName The name of the stop to get departure (and arrival) times for.
	 */
	public void retrieveStopTime(String stopName) {

		// The following needs to be run on a separate thread otherwise it may crash / hang the app.
		Thread async = new Thread(() -> {

			// Get the departure json object from the RouteMatch server.
			JSONObject departures = fnsb.macstransit.Activities.MapsActivity.routeMatch.getDeparturesByStop(stopName);

			// If the callback isn't null then pass te departures object to the receivedStopTime method.
			if (this.callback != null) {
				this.callback.receivedStopTime(departures);

			} else {

				// Since the callback was null be sure to log it as this may be a potential issue...
				android.util.Log.w("StopTimeCallback", "Callback method was never set!");
			}
		});

		// Set the name of the thread and start it.
		async.setName("StopTimeCallback");
		async.start();
	}


	/**
	 * Interface for the asynchronous callback for retrieving stop times.
	 */
	public interface AsyncCallback {

		/**
		 * Called when the stop time has been received. While this callback does not return anything,
		 * it does take the JSONObject of the departures as an argument.
		 *
		 * @param departures The raw departure json retrieved from the url.
		 *                   No processing has been done to it at this point.
		 */
		void receivedStopTime(JSONObject departures);
	}
}
