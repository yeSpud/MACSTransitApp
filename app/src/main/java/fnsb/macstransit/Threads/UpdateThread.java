package fnsb.macstransit.Threads;

import android.util.Log;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Bus;

/**
 * Created by Spud on 2019-10-13 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 4.0.
 * @since Beta 3.
 */
public class UpdateThread {

	/**
	 * Create a boolean that will be used to determine if the update thread should be running or not.
	 */
	public boolean run = false;

	/**
	 * How quickly the thread should loop after its completed.
	 * Keep in mind that the smaller this number is the quicker it loops,
	 * and thus the more frequently its pulls data from the RouteMatch server,
	 * and thus the more data it will consume.
	 * <p>
	 * This number is stored as a long, as it is the time in <i>milliseconds</i>,
	 * with the default being 4000 (4 seconds).
	 */
	private final long updateFrequency;

	/**
	 * The default update frequency (every 70 seconds / every 70000 milliseconds).
	 */
	public static final long DEFAULT_FREQUENCY = 70 * 1000;

	/**
	 * The application context this is being called from.
	 */
	private final MapsActivity context;

	/**
	 * Lazy constructor for the UpdateThread.
	 *
	 * @param context The application context this is being run from.
	 *                This is needed to run methods on the UI Thread.
	 */
	public UpdateThread(MapsActivity context) {
		this(context, UpdateThread.DEFAULT_FREQUENCY);
	}

	/**
	 * Constructor for the UpdateThread.
	 *
	 * @param context         The application context this is being run from.
	 *                        This is needed to run methods on the UI Thread.
	 * @param updateFrequency How frequently (in milliseconds) the thread should loop.
	 *                        If this is omitted, it will default to 4000 milliseconds (4 seconds).
	 */
	public UpdateThread(MapsActivity context, long updateFrequency) {
		this.context = context;
		this.updateFrequency = updateFrequency;
	}

	/**
	 * This is the thread that repeatedly queries the RouteMatch server for data on the buses,
	 * childRoutes, and stops.
	 * It loops with the frequency defined by the {@code updateFrequency} variable
	 * (default of 4000 milliseconds, or 4 seconds).
	 *
	 * @return The thread. Note that this dies not run the thread, that has to be called separately.
	 */
	public Thread thread() {
		return new Thread(() -> {

			// For debugging purposes, let the poor developer know when the thread has started.
			Log.i("Update thread", "Starting up...");

			// Check to make sure allRoutes isn't null. If it is, just set run to false.
			this.run = (MapsActivity.allRoutes != null);

			// Loop continuously while the run variable is true, and the thread hasn't been interrupted.
			while (this.run && !Thread.interrupted()) {

				// Get the buses from the RouteMatch server.
				org.json.JSONObject returnedVehicles = MapsActivity.routeMatch.getVehiclesByRoutes(MapsActivity.allRoutes);
				org.json.JSONArray vehiclesJson = fnsb.macstransit.RouteMatch.RouteMatch.parseData(returnedVehicles);

				// Get the array of buses.
				// This array will include current and new buses.
				Bus[] potentialNewBuses;
				try {
					potentialNewBuses = Bus.getBuses(vehiclesJson);
				} catch (fnsb.macstransit.RouteMatch.Route.RouteException e) {

					// If there was a route exception thrown, stop the loop early.
					Log.e("UpdateThread", "MapsActivity.allRoutes is empty!", e);
					this.run = false;
					break;
				}

				// Update the bus positions on the map on the UI thread.
				// This must be executed on the UI thread or else the app will crash.
				this.context.runOnUiThread(() -> {

					// Get the array of new buses.
					// These buses are buses that were not previously on the map until now.
					Bus[] newBuses = Bus.addNewBuses(MapsActivity.buses, potentialNewBuses);

					// Update the current position of our current buses.
					// This also removes old buses from the array, but they still have markers on the map.
					Bus[] currentBuses = Bus.updateCurrentBuses(MapsActivity.buses, potentialNewBuses);

					// Remove the markers of the old buses that are no longer on the map.
					Bus.removeOldBuses(MapsActivity.buses, potentialNewBuses);

					// Create a new bus array that will store our new and updated buses.
					Bus[] buses = new Bus[newBuses.length + currentBuses.length];

					// Populate our bus array.
					System.arraycopy(newBuses, 0, buses, 0, newBuses.length);
					System.arraycopy(currentBuses, 0, buses, newBuses.length, currentBuses.length);

					// Make sure our entire array was filled.
					if (buses.length != 0 && buses[buses.length - 1] == null) {
						Log.w("UpdateThread", "Bus array was populated incorrectly!");
					}

					// Set our bus array.
					MapsActivity.buses = buses;
				});

				// Wait for the given update frequency.
				try {
					Thread.sleep(this.updateFrequency);
				} catch (InterruptedException e) {
					Log.e("UpdateThread", "Wait interrupted", e);
				}

				// Notify the developer that the thread is now starting over.
				Log.d("Update thread", "Looping...");
			}

			// Notify the developer that the thread has exited the while loop and will now stop.
			Log.i("Update thread", "Shutting down...");
		});
	}
}