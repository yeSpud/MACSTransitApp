package fnsb.macstransit.Activities;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;
import fnsb.macstransit.RouteMatch.SharedStop;
import fnsb.macstransit.RouteMatch.Stop;

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 7.
 */
@SuppressWarnings("MagicNumber")
public class SplashActivity extends androidx.appcompat.app.AppCompatActivity {

	/**
	 * The max progress for the progress bar.
	 * The progress is determined the following checks:
	 * <ul>
	 * <li>Internet check (1)</li>
	 * <li>Creating a RouteMatch object (1)</li>
	 * <li>Downloading the master schedule (1)</li>
	 * <li>Load bus routes (Route) (8) - average number of routes</li>
	 * <li>Map the bus routes (Polyline) (8)</li>
	 * <li>Map the bus stops (8)</li>
	 * <li>Map the shared stops (8) + (52 * 8) - average number of stops per route</li>
	 * <li>Validate the stops (8)</li>
	 * </ul>
	 */
	private static final double maxProgress = 1 + 1 + 1 + 8 + 8 + 8 + 8 + (52 * 8) + 8;

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
			System.exit(0);
		}
	}

	/**
	 * Creates a thread that will run the initialization methods.
	 * The reason why this needs to be run on a new thread is if it was run on the UI thread
	 * it would cause the app to hang until all the methods are completed.
	 *
	 * @return The thread that will run all the necessary initialization methods.
	 */
	@SuppressWarnings("OverlyLongMethod")
	@org.jetbrains.annotations.NotNull
	private Thread initializeApp() {
		@SuppressWarnings("OverlyLongLambda") Thread thread = new Thread(() -> {

			// Check if the user has internet before continuing.
			this.setMessage("Checking internet connection");
			if (this.isMissingInternet()) {
				this.noInternet();
				return;
			}
			this.setProgressBar(1);

			// Create the RouteMatch object.
			this.setMessage("Creating RouteMatch object");
			try {
				MapsActivity.routeMatch = new fnsb.macstransit.RouteMatch.
						RouteMatch("https://fnsb.routematch.com/feed/");
			} catch (MalformedURLException e) {
				Log.e("initializeApp", "", e);
				this.setMessage("An incorrect URL has been provided for the RouteMatch server");
				this.progressBar.setVisibility(View.INVISIBLE);
				return;
			}
			this.setProgressBar(2);

			// Get the master schedule from the RouteMatch server
			this.setMessage("Downloading master schedule");
			JSONObject masterSchedule = MapsActivity.routeMatch.getMasterSchedule();
			this.setProgressBar(3);

			// Load the bus routes from the master schedule.
			this.setMessage("Loading bus routes");
			JSONArray routes = RouteMatch.parseData(masterSchedule);

			// Check if there are no routes for the day.
			if (routes.length() == 0) {
				this.setMessage("There are no buses scheduled to run at this time");

				// Also add a chance for the user to retry.
				this.showRetryButton();
				SplashActivity.loaded = true;
				return;
			}
			MapsActivity.allRoutes = Route.generateRoutes(routes);
			this.setProgressBar(3 + 8);

			// Map bus routes (map polyline coordinates).
			this.setMessage("Mapping bus routes");
			this.mapBusRoutes();
			this.setProgressBar(3 + (8 * 2));

			// Map bus stops.
			this.setMessage("Mapping bus stops");
			this.mapBusStops();
			this.setProgressBar(3 + (8 * 3));

			// Map shared stops.
			this.setMessage("Checking for shared bus stops");
			this.mapSharedStops();
			this.setProgressBar(1 + 1 + 1 + 8 + 8 + 8 + 8 + (52 * 8));

			// Validate stops.
			this.setMessage("Validating stops");
			this.validateStops();
			this.setProgressBar(1 + 1 + 1 + 8 + 8 + 8 + 8 + (52 * 8) + 8);


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
	private void noInternet() {
		// First, hide the progress bar.
		this.progressBar.setVisibility(View.INVISIBLE);

		// Then, set the message of the text view to notify the user that there is no internet connection.
		this.setMessage("Cannot connect to the internet");

		// Then setup the button to open the internet settings when clicked on, and make it visible.
		this.button.setText(R.string.open_network_settings);
		this.button.setOnClickListener((click) -> {
			this.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
					0);

			// Also, close this application when clicked
			this.finish();
		});
		this.button.setVisibility(View.VISIBLE);

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
	private void mapBusRoutes() {

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapBusRoutes", "All routes is null!");
			return;
		}

		// Determine the progress step.
		double step = 8.0d / MapsActivity.allRoutes.length, currentProgress = (3.0d + 8.0d);

		// Iterate though each route, and try to load the polyline in each of them.
		for (Route route : MapsActivity.allRoutes) {
			try {
				route.loadPolyLineCoordinates();
			} catch (JSONException e) {

				// If there is a JSONException while loading the polyline coordinates, just log it.
				Log.e("mapBusRoutes", String.format("Unable to map route %s", route.routeName), e);
			}

			// Update the progress.
			currentProgress += step;
			this.setProgressBar(currentProgress);
		}
	}

	/**
	 * Loads the bus stops for every route. At this point shared stops are not implemented,
	 * so stops for separate routes will overlap.
	 */
	private void mapBusStops() {

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapBusStops", "All routes is null!");
			return;
		}

		// Determine the progress step.
		double step = 8.0d / MapsActivity.allRoutes.length, currentProgress = (3.0d + (8.0d * 2.0d));

		// Iterate thorough all the routes to load each stop.
		for (Route route : MapsActivity.allRoutes) {
			route.stops = route.loadStops();

			// Update the progress.
			currentProgress += step;
			this.setProgressBar(currentProgress);
		}
	}

	/**
	 * TODO Documentation
	 */
	private void mapSharedStops() {

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("mapSharedStops", "All routes is null!");
			return;
		}

		// TODO
		double routeStep = 8.0d / MapsActivity.allRoutes.length, currentProgress = (3.0d + (8.0d * 3.0d));

		// Iterate though all the routes.
		for (int routeIndex = 0; routeIndex < MapsActivity.allRoutes.length; routeIndex++) {

			// Get a first comparison route.
			Route route = MapsActivity.allRoutes[routeIndex];

			// Get the stop for each stop (52 is the average number of stops per route).
			double stopStep = 52.0d / route.stops.length;

			// Iterate through all the stops in our first comparison route.
			for (Stop stop : route.stops) {

				Route[] sharedRoutes = SharedStop.getSharedRoutes(route, routeIndex, stop);

				if (sharedRoutes.length > 1) {
					SharedStop sharedStop = new SharedStop(stop.circleOptions.getCenter(),
							stop.stopName, sharedRoutes);

					for (Route sharedRoute : sharedRoutes) {
						sharedRoute.addSharedStop(sharedStop);
					}
				}

				currentProgress += stopStep;
				this.setProgressBar(currentProgress);
			}

			currentProgress += routeStep;
			this.setProgressBar(currentProgress);
		}

	}

	/**
	 * TODO Documentation
	 */
	private void validateStops() {

		// Verify that allRoutes is not null. If it is then log and return early.
		if (MapsActivity.allRoutes == null) {
			Log.w("validateStops", "All routes is null!");
			return;
		}

		// Determine the progress step.
		double step = 8.0d / MapsActivity.allRoutes.length, currentProgress = 1 + 1 + 1 + 8 + 8 + 8 + 8 + (52 * 8);

		for (Route route : MapsActivity.allRoutes) {
			final Stop[] finalStops = SharedStop.recreateStops(route.stops, route.sharedStops);
			Log.d("validateStops", String.format("Final stop count: %d", finalStops.length));
			route.stops = finalStops;

			currentProgress += step;
			this.setProgressBar(currentProgress);
		}

	}

	/**
	 * Launches the maps activity.
	 */
	private void launchMapsActivity() {
		// Set the loaded state to true as everything was loaded (or should have been loaded).
		SplashActivity.loaded = true;

		// Start the MapsActivity, and close this splash activity.
		this.startActivity(new Intent(this, MapsActivity.class));
		this.finish();
	}

	/**
	 * Sets the text of the TextView to be that of the provided message argument.
	 *
	 * @param message The message to be displayed in the text area.
	 */
	private void setMessage(CharSequence message) {

		// Since we are changing a TextView element, the following needs to be run on the UI thread.
		this.runOnUiThread(() -> {

			// Make sure the text view is not null.
			if (this.textView != null) {

				// Set the TextView text to that of the message.
				this.textView.setText(message);
			} else {

				// Since the TextView is null, log that it hasn't been initialized yet.
				Log.w("setMessage", "TextView has not been initialized yet");
			}
		});
	}

	/**
	 * TODO Documentation
	 *
	 * @param progress
	 */
	private void setProgressBar(double progress) {
		this.runOnUiThread(() -> {
			// Convert the progress to be an int out of 100.
			int p = (int) Math.round((progress / SplashActivity.maxProgress) * 100);

			// Validate that that the progress is between 0 and 100.
			p = (p > 100) ? 100 : Math.max(p, 0);

			// Make sure the progress bar is not null
			if (this.progressBar != null) {
				// Apply the progress to the progress bar, and animate it if its supported in the SDK.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					this.progressBar.setProgress(p, true);
				} else {
					this.progressBar.setProgress(p);
				}
			} else {
				Log.w("setProgressBar", "Progressbar has not been initialized yet");
			}
		});
	}

	/**
	 * Shows the retry button by setting the view to visible, hiding the progress bar,
	 * and by setting the click action of the button to launch the onResume() method once again.
	 */
	private void showRetryButton() {
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