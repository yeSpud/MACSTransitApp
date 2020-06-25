package fnsb.macstransit.Activities;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 7.
 */
public class SplashActivity extends androidx.appcompat.app.AppCompatActivity {

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

		this.progressBar.setVisibility(View.VISIBLE);
		this.setProgress(0);


		this.button.setVisibility(View.INVISIBLE);

		// Run the initialization on a new thread as to not hang the app.
		this.initializeApp().start();
	}


	/**
	 * TODO Documentation
	 * @return
	 */
	private Thread initializeApp() {
		// TODO Comments
		Thread thread = new Thread(() -> {
			int maxProgress = 6;

			this.setMessage("Checking internet connection");

			// First, make sure the user has internet.
			if (this.hasInternet()) {

				this.setProgress(1/maxProgress);

				// Then create the routematch object.
				this.setMessage("Creating bus schedule");
				try {
					MapsActivity.routeMatch = new RouteMatch("https://fnsb.routematch.com/feed/");
					this.setProgress(2/maxProgress);
				} catch (java.net.MalformedURLException e) {
					// If the parentRoute match url is malformed (hence an error was thrown) simply log it,
					// and then return early. Don't start loading data.
					Log.e("onResume", "The RouteMatch URL is malformed!");
					return;
				}

				// Then retrieve the routes that are being used.
				this.setMessage("Finding bus routes");
				Log.v("onResume", "Loading routes from master schedule");
				JSONObject masterSchedule = MapsActivity.routeMatch.getMasterSchedule();
				this.setProgress(3/maxProgress);

				// Check if there is no schedule for the day.
				if (masterSchedule.length() == 0) {
					Log.v("onResume", "Schedule is empty");
					this.setMessage("There are no buses scheduled to run at this time");

					// Also add a chance for the user to retry
					this.showRetryButton();
					return;
				}

				// Using the schedule load in all the routes.
				this.setMessage("Mapping bus routes");
				Route[] routes = Route.generateRoutes(masterSchedule);
				this.setProgress(4/maxProgress);

				// Using the routes generate the stops and polylines.
				this.setMessage("Mapping bus stops");
				for (Route route : routes) {
					// TODO Progress
					route.stops = route.loadStops(MapsActivity.routeMatch);
					route.polyLineCoordinates = route.loadPolyLineCoordinates(MapsActivity.routeMatch);
				}

				MapsActivity.allRoutes = routes;

				this.launchMapsActivity();

			} else {
				// Since the user doesn't have internet, let them know, and add an option to open internet settings via clicking the button.
				// First, hide the progress bar.
				this.progressBar.setVisibility(View.INVISIBLE);

				// Then, set the message of the text view to notify the user that there is no internet connection.
				this.setMessage("Cannot connect to the internet");

				// Then setup the button to open the internet settings when clicked on, and make it visible.
				this.setMessage("Open network settings");
				this.button.setOnClickListener((click) -> {
					this.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS), 0);
					// Also, close this application when clicked
					this.finish();
				});
				this.button.setVisibility(View.VISIBLE);

				// Since technically everything (which is nothing) has been loaded, set the variable as so
				SplashActivity.loaded = true;
			}
		});

		thread.setName("Initialization thread");

		return thread;
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
	 * Checks if the device has a current internet connection.
	 *
	 * @return Whether or not the device has an internet connection.
	 */
	private boolean hasInternet() {
		// TODO Comments
		ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext()
				.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
		if (connectivityManager != null) {
			android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
			return activeNetwork != null && activeNetwork.isConnected();
		} else {
			return false;
		}
	}

	/**
	 * TODO Documentation
	 */
	private void launchMapsActivity() {
		SplashActivity.loaded = true;

		// Start the MapsActivity, and close this splash activity.
		this.startActivity(new Intent(this, MapsActivity.class));
		this.finish();
	}

	/**
	 * TODO Documentation
	 * @param message
	 */
	private void setMessage(String message) {
		this.runOnUiThread(() -> this.textView.setText(message));
	}

	/**
	 * Sets the percent progress on the progress bar from a scale of 0 to 1 (as a double).
	 * If the progress is larger than 1, it will be converted to 1. If the progress is less than 0,
	 * it will be converted to 0.
	 *
	 * @param progress The progress to be set on the progress bar as a percent (between 0 and 1).
	 */
	private void setProgress(double progress) {
		this.runOnUiThread(() -> {
			// Convert the progress to be an int out of 100.
			int p = (int) Math.round(progress * 100);

			// Validate that that the progress is between 0 and 100.
			p = (p > 100) ? 100 : Math.max(p, 0);
			Log.d("setProgress", "Progress: " + p);

			// Apply the progress to the progress bar, and animate it if its supported in the SDK.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				this.progressBar.setProgress(p, true);
			} else {
				this.progressBar.setProgress(p);
			}
		});
	}

	/**
	 * Shows the retry button by setting the view to visible, hiding the progress bar,
	 * and by setting the click action of the button to launch the onResume() method once again.
	 */
	private void showRetryButton() {
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