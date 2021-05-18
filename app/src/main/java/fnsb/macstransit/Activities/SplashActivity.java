package fnsb.macstransit.Activities;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.AnyThread;

import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.1.
 * @since Beta 7.
 */
@SuppressWarnings("MagicNumber")
public class SplashActivity extends androidx.appcompat.app.AppCompatActivity {

	/**
	 * The max progress for the progress bar.
	 * The progress is determined the following checks:
	 * <ul>
	 * <li>Downloading the master schedule (1)</li>
	 * <li>Load bus routes (Route) (8) - average number of routes</li>
	 * <li>Map the bus routes (Polyline) (8)</li>
	 * <li>Map the bus stops (8)</li>
	 * <li>Map the shared stops (1)</li>
	 * <li>Validate the stops (1)</li>
	 * </ul>
	 */
	private static final double maxProgress = 1 + 8 + 8 + 8 + 1 + 1;

	/**
	 * Create a variable to check if the map activity has already been loaded
	 * (as to determine if the app needs to close when the back button is clicked,
	 * or just needs to refresh the activity)
	 */
	public static boolean loaded = false;

	/**
	 * The TextView widget in the activity.
	 */
	private android.widget.TextView textView;

	/**
	 * The ProgressBar widget in the activity.
	 */
	private android.widget.ProgressBar progressBar;

	/**
	 * TODO Documentation
	 */
	public static final Object LOCK = new Object();

	/**
	 * The Button widget in the activity.
	 */
	private android.widget.Button button;

	/**
	 * Called when the activity is starting.
	 * This is where most initialization should go: calling setContentView(int) to inflate the activity's UI,
	 * using findViewById(int) to programmatically interact with widgets in the UI,
	 * calling managedQuery(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String) to retrieve cursors for data being displayed, etc.
	 * <p>
	 * You can call finish() from within this function,
	 * in which case onDestroy() will be immediately called after onCreate(Bundle)
	 * without any of the rest of the activity lifecycle (onStart(), onResume(), onPause(), etc) executing.
	 *
	 * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
	 *                           Note: Otherwise it is null. This value may be null.
	 */
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the view to that of the splash screen.
		this.setContentView(R.layout.splashscreen);

		// Find the widgets of use in the splash screen, and assign them to their own private variables.
		this.textView = this.findViewById(R.id.textView);
		this.progressBar = this.findViewById(R.id.progressBar);
		this.button = this.findViewById(R.id.button);

		// Psst. Hey. Wanna know a secret?
		// In the debug build you can click on the logo to launch right into the maps activity.
		// This is mainly for a bypass on Sundays. :D
		if (fnsb.macstransit.BuildConfig.DEBUG) {
			this.findViewById(R.id.logo).setOnClickListener((click) -> this.launchMapsActivity());
		}

		// Set the button widget to have no current onClickListener, and set it to be invisible for now.
		this.button.setOnClickListener(null);
		this.button.setVisibility(View.INVISIBLE);

		// Setup the progress bar by defining its max, and if the SDK supports it, assign its min as well.
		this.progressBar.setMax(100);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.progressBar.setMin(0);
		}

		// Make sure the progress bar is visible to the user.
		this.progressBar.setVisibility(View.VISIBLE);
	}

	/**
	 * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
	 * for your activity to start interacting with the user.
	 * This is an indicator that the activity became active and ready to receive input.
	 * It is on top of an activity stack and visible to user.
	 * <p>
	 * On platform versions prior to Build.VERSION_CODES.Q this is also a good place to try to open exclusive-access devices or to get access to singleton resources.
	 * Starting with Build.VERSION_CODES.Q there can be multiple resumed activities in the system simultaneously,
	 * so onTopResumedActivityChanged(boolean) should be used for that purpose instead.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// Initialize the progress bar to 0.
		this.progressBar.setVisibility(View.VISIBLE);
		this.setProgress(0);

		// Make sure the dynamic button is invisible.
		this.button.setVisibility(View.INVISIBLE);

		// Run the initialization on a new thread as to not hang the app.
		this.initializeApp().start();
	}

	/**
	 * Called as part of the activity lifecycle when the user no longer actively interacts with the activity,
	 * but it is still visible on screen. The counterpart to onResume().
	 * <p>
	 * When activity B is launched in front of activity A, this callback will be invoked on A.
	 * B will not be created until A's onPause() returns, so be sure to not do anything lengthy here.
	 * <p>
	 * This callback is mostly used for saving any persistent state the activity is editing,
	 * to present a "edit in place" model to the user and making sure nothing is lost if there are
	 * not enough resources to start the new activity without first killing this one.
	 * This is also a good place to stop things that consume a noticeable amount of CPU in order to
	 * make the switch to the next activity as fast as possible.
	 * <p>
	 * On platform versions prior to Build.VERSION_CODES.Q this is also a good place to try to close
	 * exclusive-access devices or to release access to singleton resources.
	 * Starting with Build.VERSION_CODES.Q there can be multiple resumed activities in the system
	 * at the same time, so onTopResumedActivityChanged(boolean) should be used for that purpose instead.
	 * <p>
	 * If an activity is launched on top,
	 * after receiving this call you will usually receive a following call to onStop()
	 * (after the next activity has been resumed and displayed above).
	 * However in some cases there will be a direct call back to onResume()
	 * without going through the stopped state.
	 * An activity can also rest in paused state in some cases when in multi-window mode,
	 * still visible to user.
	 * <p>
	 * Derived classes must call through to the super class's implementation of this method.
	 * If they do not, an exception will be thrown.
	 */
	@Override
	protected void onPause() {
		super.onPause();

		// Simply close the application, since it hasn't finished loading.
		if (!SplashActivity.loaded) {
			this.finishAffinity();
		}
	}

	/**
	 * Creates a thread that will run the initialization methods.
	 * The reason why this needs to be run on a new thread is if it was run on the UI thread
	 * it would cause the app to hang until all the methods are completed.
	 *
	 * @return The thread that will run all the necessary initialization methods.
	 */
	@androidx.annotation.NonNull
	private Thread initializeApp() {
		Thread thread = new Thread(() -> {

			// Check if the user has internet before continuing.
			this.setMessage(R.string.internet_check);
			if (this.isMissingInternet()) {
				this.noInternet();
				return;
			}

			// Create the RouteMatch object.
			this.setMessage(R.string.routematch_creation);
			try {
				MapsActivity.routeMatch = new fnsb.macstransit.RouteMatch.
						RouteMatch("https://fortsmith.routematch.com/feed/", this.getApplicationContext());
			} catch (java.net.MalformedURLException e) {
				Log.e("initializeApp", "", e);
				this.setMessage(R.string.routematch_creation_fail);
				this.progressBar.setVisibility(View.INVISIBLE);
				return;
			}

			// Get the master schedule from the RouteMatch server
			this.setProgressBar(-1);
			this.setMessage(R.string.downloading_master_schedule);
			MapsActivity.routeMatch.callMasterSchedule(
					new fnsb.macstransit.Threads.MasterScheduleCallback(this),
					error -> {
						Log.w("initializeApp", "MasterSchedule callback error", error);
						this.setMessage(R.string.routematch_timeout);
						this.showRetryButton();
					});

			// Wait for the callback to finish.
			synchronized (SplashActivity.LOCK) {
				try {
					SplashActivity.LOCK.wait();
				} catch (InterruptedException e) {
					Log.e("initializeApp", "Interrupted!", e);
					return;
				}
			}
			this.setProgressBar(1 + 8);

			// Map bus routes (map polyline coordinates).
			this.mapBusRoutes();

			// Map bus stops.
			this.mapBusStops();

			// Map shared stops.
			this.mapSharedStops();

			// Validate stops.
			this.validateStops();

			// Finally, launch the maps activity.
			this.launchMapsActivity();
		});

		// Set the name of the thread, and finally return it.
		thread.setName("Initialization thread");
		return thread;
	}

	/**
	 * Changes the splash screen display when there is no internet.
	 * This method involves making the progress bar invisible,
	 * and setting the button to launch the wireless settings.
	 * It will also close the application when the button is clicked (as to force a restart of the app).
	 */
	@AnyThread
	private void noInternet() {

		// The following needs to run on the UI thread.
		this.runOnUiThread(() -> {

			// First, hide the progress bar.
			this.progressBar.setVisibility(View.INVISIBLE);

			// Then, set the message of the text view to notify the user that there is no internet connection.

			//this.setMessage(R.string.cannot_connect_internet);
			this.textView.setText(R.string.cannot_connect_internet);

			// Then setup the button to open the internet settings when clicked on, and make it visible.
			this.button.setText(R.string.open_network_settings);
			this.button.setOnClickListener((click) -> {
				this.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
						0);

				// Also, close this application when clicked
				this.finish();
			});

			// Set the button to invisible.
			this.button.setVisibility(View.VISIBLE);
		});

		// Since technically everything (which is nothing) has been loaded, set the variable as so
		SplashActivity.loaded = true;
	}

	/**
	 * Checks if the device has a current internet connection.
	 *
	 * @return Whether or not the device has an internet connection.
	 */
	private boolean isMissingInternet() {

		// Get the connectivity manager for the device.
		ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext()
				.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);

		// Check if the connectivity manager is null.
		if (connectivityManager != null) {

			// Get the network info
			android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
			return activeNetwork == null || !activeNetwork.isConnected();
		} else {

			// Since the connectivity manager is null return true.
			return true;
		}
	}

	/**
	 * Loads the polylines for each route. Also known as mapping the bus routes to the map.
	 */
	public void mapBusRoutes() {

		// Display that we are mapping bus routes to the user.
		this.setMessage(R.string.mapping_bus_routes);

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapBusRoutes", "All routes is null!");
			return;
		}

		// Determine the progress step.
		double step = 8.0d / MapsActivity.allRoutes.length, currentProgress = 8;

		// Iterate though each route, and try to load the polyline in each of them.
		for (Route route : MapsActivity.allRoutes) {
			try {
				route.loadPolyLineCoordinates();
			} catch (org.json.JSONException e) {

				// If there is a JSONException while loading the polyline coordinates, just log it.
				Log.e("mapBusRoutes", String.format("Unable to map route %s", route.routeName), e);
			}

			// Update the progress.
			currentProgress += step;
			this.setProgressBar(currentProgress);
		}

		// Update the progress bar one final time for this method.
		this.setProgressBar(1 + 8 + 8);
	}

	/**
	 * Loads the bus stops for every route. At this point shared stops are not implemented,
	 * so stops for separate routes will overlap.
	 */
	public void mapBusStops() {

		// Update the user that we are now mapping (calculating) bus stops.
		this.setMessage(R.string.mapping_bus_stops);

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapBusStops", "All routes is null!");
			return;
		}

		// Determine the progress step.
		double step = 8.0d / MapsActivity.allRoutes.length, currentProgress = 1 + 8 + 8;

		// Iterate thorough all the routes to load each stop.
		for (Route route : MapsActivity.allRoutes) {
			route.stops = route.loadStops();

			// Update the progress.
			currentProgress += step;
			this.setProgressBar(currentProgress);
		}

		// Update the progress one last time for this method.
		this.setProgressBar(1 + 8 + 8 + 8);
	}

	/**
	 * Adds the shared stops to the map.
	 * This is done by iterating through all the stops in each route and checking for duplicates.
	 * If there are any found they will be added to all the routes the stop belongs to as a shared stop.
	 * At this point the original stop is still present in the route.
	 */
	public void mapSharedStops() {

		// Let the user know that we are checking for shared bus stops at this point.
		this.setMessage(R.string.shared_bus_stop_check);

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapSharedStops", "All routes is null!");
			return;
		}

		// Set the current progress.
		double step = 1.0d / MapsActivity.allRoutes.length, currentProgress = 1 + 8 + 8 + 8;

		// Iterate though all the routes.
		for (int routeIndex = 0; routeIndex < MapsActivity.allRoutes.length; routeIndex++) {

			// Get a first comparison route.
			Route route = MapsActivity.allRoutes[routeIndex];

			// Iterate through all the stops in our first comparison route.
			for (Stop stop : route.stops) {

				// Make sure our stop is not already in our shared stop.
				SharedStop[] sharedStops = route.getSharedStops();
				if (sharedStops != null) {
					boolean found = false;

					// Iterate though the shared stops in the route.
					for (SharedStop sharedStop : sharedStops) {

						// If the route was found, continue.
						if (SharedStop.areEqual(sharedStop, stop)) {
							found = true;
							break;
						}
					}
					if (found) {
						continue;
					}
				}

				// Get an array of shared routes.
				Route[] sharedRoutes = SharedStop.getSharedRoutes(route, routeIndex, stop);

				// If the shared routes array has more than one entry, create a new shared stop object.
				if (sharedRoutes.length > 1) {
					SharedStop sharedStop = new SharedStop(stop.circleOptions.getCenter(), stop.name,
							sharedRoutes);

					// Iterate though all the routes in the shared route, and add our newly created shared stop.
					for (Route sharedRoute : sharedRoutes) {
						sharedRoute.addSharedStop(sharedStop);
					}
				}
			}

			// Update the progress.
			currentProgress += step;
			this.setProgressBar(currentProgress);
		}

		// Update the progress bar one last time for this method.
		this.setProgressBar(1 + 8 + 8 + 8 + 1);
	}

	/**
	 * Validates the stops and shared stops.
	 * Meaning this method removes the stops that are shared stops as to not duplicate the stop.
	 */
	public void validateStops() {

		// Let the user know that we are validating the stops (and shared stop) for each route.
		this.setMessage(R.string.stop_validation);

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("validateStops", "All routes is null!");
			return;
		}

		// Determine the progress step.
		double step = 1.0d / MapsActivity.allRoutes.length, currentProgress = 1 + 8 + 8 + 8 + 1;

		// Iterate though all the routes and recreate the stops for each route.
		for (Route route : MapsActivity.allRoutes) {

			// Get the final stop count for each route by removing stops that are taken care of by the shared route object.
			final Stop[] finalStops = SharedStop.removeStopsWithSharedStops(route.stops, route.getSharedStops());
			Log.d("validateStops", String.format("Final stop count: %d", finalStops.length));

			// Set the stops array for the route to the final determined stop array.
			// This array no longer contains the stops that are shared stops.
			route.stops = finalStops;

			// Update the progress.
			currentProgress += step;
			this.setProgressBar(currentProgress);
		}

		// Update the progress bar one last time for this method.
		this.setProgressBar(1 + 8 + 8 + 8 + 1 + 1);
	}

	/**
	 * Launches the maps activity.
	 */
	public void launchMapsActivity() {

		// Set the loaded state to true as everything was loaded (or should have been loaded).
		SplashActivity.loaded = true;

		// Set the selected favorites routes to be false for the maps activity.
		MapsActivity.selectedFavorites = false;

		/*
		Suggest some garbage collection since we are done with a lot of heavy processing.
		While this is normally discouraged as it implies poor processing practices,
		it's used here since we have finished a gauntlet of processing steps that created and discarded
		many different arrays, which can now be returned to the OS.
		*/
		// Runtime.getRuntime().gc();
		/*
		UPDATE:
		After reading through many articles it has been decided to disable the garbage collection call
		as in most cases it seems to hurt performance. Its being left in the code as a comment in the event
		that it should be re-enabled for testing, but that's about it - a testing use case.
		*/

		// Start the MapsActivity, and close this splash activity.
		this.startActivity(new Intent(this, MapsActivity.class));
		this.finishAfterTransition();
	}

	/**
	 * Sets the message content to be displayed to the user on the splash screen.
	 *
	 * @param resID The string ID of the message. This can be retrieved by calling R.string.STRING_ID.
	 */
	@AnyThread
	public void setMessage(@androidx.annotation.StringRes final int resID) {

		// Since we are changing a TextView element, the following needs to be run on the UI thread.
		this.runOnUiThread(() -> {

			// Make sure the text view is not null.
			if (this.textView != null) {

				// Set the TextView text to that of the message.
				this.textView.setText(resID);
			} else {

				// Since the TextView is null, log that it hasn't been initialized yet.
				Log.w("setMessage", "TextView has not been initialized yet");
			}
		});
	}

	/**
	 * Update the progress bar to the current progress.
	 *
	 * @param progress The current progress out of SplashActivity.maxProgress.
	 */
	@AnyThread
	public void setProgressBar(final double progress) {
		Log.v("setProgressBar", String.format("Provided progress: %f", progress));

		// Because we are updating UI elements we need to run the following on the UI thread.
		this.runOnUiThread(() -> {

			// Convert the progress to be an int out of 100.
			int p = (int) Math.round((progress / SplashActivity.maxProgress) * 100);

			/* Validate that that the progress is between 0 and 100.
			This is the equivalent of:
			if (p > 100) {
				p = 100;
			} else {
				p = Math.max(p,0);
			}
			 */
			p = (p > 100) ? 100 : Math.max(p, 0);

			// Make sure the progress bar is not null.
			if (this.progressBar != null) {

				// Set the progress to indeterminate if its less than 1.
				this.progressBar.setIndeterminate(progress < 0.0d);

				// Apply the progress to the progress bar, and animate it if its supported in the SDK.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					this.progressBar.setProgress(p, true);
				} else {
					this.progressBar.setProgress(p);
				}
			} else {

				// Log that the progress bar has not been set up yet.
				Log.w("setProgressBar", "Progressbar has not been initialized yet");
			}
		});
	}

	/**
	 * Shows the retry button by setting the view to visible, hiding the progress bar,
	 * and by setting the click action of the button to launch the onResume() method once again.
	 */
	@AnyThread
	public void showRetryButton() {

		// Since we are updating UI elements, run the following on the UI thread.
		this.runOnUiThread(() -> {

			// First hide the progress bar since it is no longer of use.
			this.progressBar.setVisibility(View.INVISIBLE);

			// Then setup the button to relaunch the activity, and make it visible.
			this.button.setText(R.string.retry);
			this.button.setOnClickListener((click) -> this.onResume());
			this.button.setVisibility(View.VISIBLE);
		});
	}

}