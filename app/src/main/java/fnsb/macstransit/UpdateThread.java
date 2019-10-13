package fnsb.macstransit;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Spud on 2019-10-13 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class UpdateThread {

	/**
	 * Create a boolean that will be used to determine if the update thread should be running or not.
	 */
	public boolean run = false;

	/**
	 * TODO Documentation
	 */
	private long updateFrequency = 4000;

	/**
	 * TODO Documentation
	 */
	private MapsActivity activity;

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 */
	public UpdateThread(MapsActivity activity) {
		this.activity = activity;
	}

	/**
	 * TODO Documentation
	 *
	 * @param activity
	 * @param updateFrequency
	 */
	public UpdateThread(MapsActivity activity, long updateFrequency) {
		this(activity);
		this.updateFrequency = updateFrequency;
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public Thread thread() { // TODO Better comments
		Thread t = new Thread(() -> {

			Log.w("Update thread", "Starting up...");

			while (this.run && !Thread.interrupted()) {

				try {
					for (Route route : this.activity.routes) {
						org.json.JSONArray array = this.activity.routeMatch.getRoute(route.routeName).getJSONArray("data");

						Log.i("Full data", array.toString());
						for (int i = 0; i < array.length(); i++) {
							JSONObject object = array.getJSONObject(i);

							// Create a bus object form the data
							Bus bus = new Bus(object.getString("vehicleId"), route);
							try {
								bus.heading = Heading.valueOf(object.getString("headingName"));
							} catch (IllegalArgumentException e) {
								bus.heading = Heading.NORTH;
							}
							bus.latitude = object.getDouble("latitude");
							bus.longitude = object.getDouble("longitude");
							bus.color = route.getColor();


							// Search the current array of buses for that bus ID
							// If it exists, update its lat and long, and heading.
							// If it doesn't exist, add it.
							boolean found = false;
							for (Bus busCheck : this.activity.buses) {
								if (bus.busID.equals(busCheck.busID)) {
									found = true;
									// DO NOT MODIFY THE BUS MARKER
									busCheck.heading = bus.heading;
									busCheck.route = bus.route;
									busCheck.latitude = bus.latitude;
									busCheck.longitude = bus.longitude;
									busCheck.color = bus.color;
									break;
								}
							}

							if (!found) {
								this.activity.buses.add(bus);
							}

							// Update the bus markers
							this.activity.updateBusMarkers();
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

				// Sleep for the given update frequency
				try {
					Thread.sleep(this.updateFrequency);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Thread.yield();
				Log.d("Update thread", "Looping...");

			}

			Log.w("Update thread", "Shutting down...");
		});

		return t;

	}

}
