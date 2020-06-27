package fnsb.macstransit.Threads;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Bus;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

/**
 * Created by Spud on 2019-10-13 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 4.0
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
	 * and thus the more frequently its pulls data from the routematch server,
	 * and thus the more data it will consume.
	 * <p>
	 * This number is stored as a long, as it is the time in <i>milliseconds</i>,
	 * with the default being 4000 (4 seconds).
	 */
	private long updateFrequency = 4000;

	/**
	 * TODO Documents
	 */
	public UpdateThread() {
	}

	/**
	 * Constructor for the UpdateThread.
	 * TODO Documentation
	 * @param updateFrequency How frequency (in milliseconds) the thread should loop.
	 *                        If this is omitted, it will default to 4000 milliseconds (4 seconds).
	 */
	public UpdateThread(long updateFrequency) {
		this.updateFrequency = updateFrequency;
	}

	/**
	 * This is the thread that repeatedly queries the routematch server for data on the buses,
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

			// Loop continuously while the run variable is true, and the thread hasn't been interrupted.
			while (this.run && !Thread.interrupted()) {

				// TODO Comments
				JSONObject returnedVehicles = MapsActivity.routeMatch.getVehiclesByRoutes(MapsActivity.allRoutes);
				JSONArray vehiclesJson = RouteMatch.parseData(returnedVehicles);

				Bus[] newBuses;
				try {
					newBuses = Bus.getBuses(vehiclesJson);
				} catch (Route.RouteException e) {
					Log.e("UpdateThread", "MapsActivity.allRoutes is empty!", e);
					this.run = false;
					break;
				}

				ArrayList<Bus> buses = new ArrayList<>(Arrays.asList(Bus.addNewBuses(MapsActivity.buses, newBuses)));
				buses.addAll(Arrays.asList(Bus.updateCurrentBuses(MapsActivity.buses, newBuses)));
				Bus.removeOldBuses(MapsActivity.buses, newBuses);

				MapsActivity.buses = buses.toArray(new Bus[0]);

				// Sleep for the given update frequency
				try {
					Thread.sleep(this.updateFrequency);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Thread.yield();

				// Notify the developer that the thread is now starting over.
				Log.d("Update thread", "Looping...");
			}
			// Notify the developer that the thread has exited the while loop and will now stop.
			Log.i("Update thread", "Shutting down...");
		});
	}
}