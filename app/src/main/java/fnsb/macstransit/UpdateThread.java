package fnsb.macstransit;

import android.util.Log;

import org.json.JSONException;

import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Heading;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 2019-10-13 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 3
 */
public class UpdateThread {

	/**
	 * Create a boolean that will be used to determine if the update thread should be running or not.
	 */
	public boolean run = false;

	/**
	 * How quickly the thread should loop after its completed.
	 * Keep in mind that the smaller this number is the quicker it loops,
	 * and thus the more frequently its pulls data from the routematch server,
	 * and thus the more data it will consume.
	 * <p>
	 * This number is stored as a long, as it is the time in <i>milliseconds</i>,
	 * with the default being 4000 (4 seconds).
	 */
	private long updateFrequency = 4000;

	/**
	 * The MapsActivity (Main activity).
	 */
	private MapsActivity activity;

	/**
	 * Constructor for the UpdateThread.
	 *
	 * @param activity The MapsActivity (this should be the main activity).
	 */
	public UpdateThread(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * Constructor for the UpdateThread.
	 *
	 * @param activity        The MapsActivity (this should be the main activity).
	 * @param updateFrequency How frequency (in milliseconds) the thread should loop.
	 *                        If this is omitted, it will default to 4000 milliseconds (4 seconds).
	 */
	public UpdateThread(MapsActivity activity, long updateFrequency) {
		this(activity);
		this.updateFrequency = updateFrequency;
	}

	/**
	 * This is the thread that repeatedly queries the routematch server for data on the buses, routes, and stops.
	 * It loops with the frequency defined by the {@code updateFrequency} variable (default of 4000 milliseconds, or 4 seconds).
	 *
	 * @return The thread. Note that this dies not run the thread, that has to be called separately.
	 */
	Thread thread() {
		return new Thread(() -> {

			// For debugging purposes, let the poor developer know when the thread has started.
			Log.w("Update thread", "Starting up...");

			// Loop continuously while the run variable is true, and  the thread hasn't been interrupted for whatever reason.
			while (this.run && !Thread.interrupted()) {

				// Make a copy of the selected routes array to run iterations on (to avoid the ConcurrentModificationException of death).
				Route[] routes = this.activity.selectedRoutes.toArray(new Route[0]);

				// If there are no selected routes, loop quickly (every quarter second) rather than the set frequency.
				if (routes.length != 0) {

					// Because there is a lot of JSON parsing in the following section, be sure to catch any JSON parsing errors.
					try {

						// For each of the selected routes from the activity, retrieve one, and execute the following
						for (Route route : routes) {

							// Parse the bus data for the respective route
							this.parseBuses(route);

						}
						// Sleep for the given update frequency
						try {
							Thread.sleep(this.updateFrequency);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Thread.yield();
					} catch (org.json.JSONException e) {
						// For now, just print a stack trace if there are any errors.
						e.printStackTrace();
					}
				} else {

					// Quick sleep since there are no routes to track
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Thread.yield();
				}

				// Notify the developer that the thread is now starting over.
				Log.d("Update thread", "Looping...");
			}

			// Notify the developer that the thread has exited the while loop and will now stop.
			Log.w("Update thread", "Shutting down...");
		});
	}

	/**
	 * Parses the buses from the current route, and then calls {@code updateBusMarkers} from the activity thread.
	 *
	 * @param route The route to parse the buses from.
	 */
	private void parseBuses(Route route) throws JSONException {
		// Get the data section of the bus JSON pulled from the routematch server
		org.json.JSONArray busArray;
		try {
			busArray = this.activity.routeMatch.getRoute(route.routeName).getJSONArray("data");
		} catch (JSONException noData) {
			Log.w("parseBuses", "No data for buses!");
			return;
		}

		// In the event that there are multiple buses running on one route, the array will have a size > 1.
		// To combat this, just loop through the size of the array, and parse the JSON for the individual buses.
		int count = busArray.length();
		for (int i = 0; i < count; i++) {


			// Log the progress of parsing the buses.
			Log.d("parseBuses", String.format("Parsing bus %d/%d", i + 1, count));

			// Get the individual bus from the JSON array at the provided index
			org.json.JSONObject object = busArray.getJSONObject(i);

			// Create a new bus object form the data.
			// In order to create a bus object, we need the busID, and the route.
			// Parse the busID from the data (stored as vehicleID), and insert the route from above.
			Bus bus = new Bus(object.getString("vehicleId"), route);

			// Try to add the heading of the bus. Sometimes this is blank, so if that is the case, just default to NORTH.
			try {
				// The heading is stored in the data as headingName
				bus.heading = Heading.valueOf(object.getString("headingName"));
			} catch (IllegalArgumentException e) {
				bus.heading = Heading.NORTH;
			}

			// Get the latitude of the bus, which is stored in the data as latitude.
			bus.latitude = object.getDouble("latitude");

			// Get the longitude of the bus, which is stored in the data as longitude.
			bus.longitude = object.getDouble("longitude");

			// Set the bus color to that of the route color.
			// If the route doesn't have a color it will just assign the value of 0,
			// as its stored as an int.
			bus.color = route.color;

			// Search the current array of buses for the newly created bus ID.
			// If it exists, then update the old bus properties.
			// If it doesn't exist, add it to the array of buses to track.
			boolean found = false;
			for (Bus busInArray : this.activity.buses) {
				if (bus.busID.equals(busInArray.busID)) {
					found = true;
					// DO NOT MODIFY THE BUS MARKER
					busInArray.heading = bus.heading;
					busInArray.route = bus.route;
					busInArray.latitude = bus.latitude;
					busInArray.longitude = bus.longitude;
					busInArray.color = bus.color;
					// If the bus was found, just break early
					break;
				}
			}

			// Add the bus to the array of tracked buses if it wasn't in the array.
			if (!found) {
				Log.d("parseBuses", "Adding bus: " + bus.busID + " to the array");
				this.activity.buses.add(bus);
			}

			// Update the bus markers on the map
			this.activity.updateBusMarkers();
		}
	}
}
