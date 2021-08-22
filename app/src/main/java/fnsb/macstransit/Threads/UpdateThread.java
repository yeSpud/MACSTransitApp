package fnsb.macstransit.threads;

import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONException;

import fnsb.macstransit.activities.mapsactivity.MapsActivity;
import fnsb.macstransit.routematch.Bus;
import fnsb.macstransit.routematch.RouteMatch;

/**
 * Created by Spud on 2019-10-13 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 4.1.
 * @since Beta 3.
 */
@Deprecated
public class UpdateThread {

	/**
	 * The default update frequency (every 10 seconds / every 10000 milliseconds).
	 */
	public static final long DEFAULT_FREQUENCY = 10 * 1000;

	/**
	 * Object used for synchronization between the maps activity and the update thread.
	 */
	public final Object LOCK = new Object();

	/**
	 * How quickly the thread should loop after its completed.
	 * Keep in mind that the smaller this number is the quicker it loops,
	 * and thus the more frequently its pulls data from the RouteMatch server,
	 * and thus the more data it will consume.
	 * <p>
	 * This number is stored as a long, as it is the time in <i>milliseconds</i>).
	 */
	private final long updateFrequency;

	/**
	 * Handler used to update bus markers on the UI Thread (without memory leaks).
	 */
	private final Handler UIHandler;

	/**
	 * The runner that updates the buses on the map. This must be run on the UI Thread.
	 */
	private final UpdateBuses updateBuses;

	/**
	 * The current state of the Update Thread. Default state is stopped.
	 */
	public STATE state = STATE.STOP;

	/**
	 * Used to determine if the thread is locked without a specified timeout (waits for forever).
	 * This is private because it should not be set outside this class.
	 * Default value is true before the thread is setup.
	 */
	private boolean isLockedForever = true;

	/**
	 * Thread that periodically fetches new buses from the RouteMatch server.
	 */
	public final Thread runner = this.getNewThread();

	private final RouteMatch routeMatch;

	/**
	 * Lazy constructor for the UpdateThread.
	 *
	 * @param handler Handler used to update buses on the UI Thread.
	 * @param map TODO
	 */
	public UpdateThread(Handler handler, RouteMatch routeMatch, GoogleMap map) {
		this(handler, UpdateThread.DEFAULT_FREQUENCY, routeMatch, map);
	}

	/**
	 * Constructor for the UpdateThread.
	 *
	 * @param handler         Handler used to update buses on the UI Thread.
	 * @param updateFrequency How frequently (in milliseconds) the thread should loop.
	 *                        If this is omitted, it will default to 4000 milliseconds (4 seconds).
	 * @param map TODO
	 */
	public UpdateThread(Handler handler, long updateFrequency, RouteMatch routeMatch, GoogleMap map) {
		this.UIHandler = handler;
		this.updateFrequency = updateFrequency;
		this.routeMatch = routeMatch;
		this.updateBuses = new UpdateBuses(map);
	}

	/**
	 * Creates a new update thread object.
	 *
	 * @return The newly created update thread object.
	 */
	@androidx.annotation.NonNull
	private Thread getNewThread() {
		Log.i("UpdateThread", "Created new update thread!");

		Thread updateThread = new Thread(() -> {

			// At this point the thread is not yet locked.
			this.isLockedForever = false;

			// For debugging purposes, let the developer know when the thread has started.
			Log.i("UpdateThread", "Starting up...");

			// Be sure to synchronize the following to the LOCK object.
			synchronized (this.LOCK) {

				// Loop continuously while the thread has not been interrupted.
				while (!Thread.interrupted() && this.state != STATE.STOP) {

					// Notify the developer that the thread is now looping.
					Log.d("UpdateThread", "Looping...");

					try {
						this.routeMatch.getNetworkQueue().cancelAll(this);
					} catch (NullPointerException npe) {
						Log.w("UpdateThread", "Nothing in queue to cancel");
					}

					try {

						switch (this.state) {
							case RUN:

								// If we are currently set to run then get the buses from the RouteMatch server.
								// If we aren't running, then wait for the thread to be notified from elsewhere.
								// Fetch the buses and update them on the map.
								this.fetchBuses();

								// Wait for the given update frequency.
								Log.v("UpdateThread", String.format("Waiting for %d milliseconds",
										this.updateFrequency));
								this.LOCK.wait(this.updateFrequency);
								break;
							case PAUSE:

								// Notify the developer that we are going to pause the thread
								// (as to reuse it when resuming later).
								Log.i("UpdateThread", "Waiting for thread to be resumed...");

								// Wait for notify to be called from the MapsActivity, and lock the thread.
								this.isLockedForever = true;
								this.LOCK.wait();

								// At this point the thread has been unlocked. Yay!
								this.isLockedForever = false;
								Log.i("UpdateThread", "Resuming...");
								break;
							case STOP:
								Log.i("UpdateThread", "Stopping thread...");
								break;
						}

					} catch (InterruptedException e) {
						Log.w("UpdateThread", "Wait interrupted! Exiting early...", e);
						break;
					} catch (IllegalMonitorStateException stateException) {
						Log.e("UpdateThread", "Illegal state", stateException);
						break;
					}
				}
			}

			// Notify the developer that the thread has exited the while loop and will now stop.
			Log.i("UpdateThread", "Shutting down...");

		});

		updateThread.setName("Update Thread");
		return updateThread;
	}

	/**
	 * Stops the update thread.
	 */
	public void stop() {
		Log.d("UpdateThread", "Stopping thread...");
		this.state = STATE.STOP;
		this.runner.interrupt();
	}

	/**
	 * Method that fetches buses from the route match server,
	 * and passes off the result to be parsed on the UI thread.
	 */
	private void fetchBuses() {

		// Get the buses from the RouteMatch server.
		this.routeMatch.callVehiclesByRoutes(response -> {
			org.json.JSONArray vehiclesJson = fnsb.macstransit.routematch.RouteMatch.parseData(response);

			// Get the array of buses. This array will include current and new buses.
			Bus[] buses;
			try {
				buses = Bus.getBuses(vehiclesJson);
			} catch (JSONException e) {
				Log.e("fetchBuses", "Could not parse bus json", e);
				return;
			}

			// Update the bus positions on the map on the UI thread.
			// This must be executed on the UI thread or else the app will crash.
			this.updateBuses.setPotentialNewBuses(buses);
			this.UIHandler.post(this.updateBuses);
		}, error -> Log.w("fetchBuses", "Unable to fetch buses", error), this, MapsActivity.Companion.getAllRoutes());
	}

	/**
	 * Gets whether or not the thread is currently locked forever (waiting without timeout).
	 *
	 * @return If the thread is locked forever (waiting without timeout).
	 */
	public boolean getIsLockedForever() {
		return this.isLockedForever;
	}

	/**
	 * Various states that the Update Thread operates in.
	 */
	public enum STATE {
		PAUSE, RUN, STOP
	}
}