package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONException;

import fnsb.macstransit.Activities.SettingsActivity;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.Settings.CurrentSettings;

/**
 * Created by Spud on 6/23/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
@androidx.annotation.UiThread
public class ApplySettings implements View.OnClickListener {

	/**
	 * The activity that corresponds to this listener.
	 */
	private final SettingsActivity activity;

	/**
	 * Constructor for the on click listener.
	 *
	 * @param activity The activity that corresponds to this listener.
	 */
	public ApplySettings(SettingsActivity activity) {
		this.activity = activity;
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick(View v) {

		// Get the favorite routes from the activity.
		Route[] favoritedRoutes = this.getFavoritedRoutes();

		// Determine the map type.
		int mapId;
		int radioId = this.activity.mapType.getCheckedRadioButtonId();
		if (radioId == R.id.satellite_map) {
			mapId = GoogleMap.MAP_TYPE_SATELLITE;
		} else if (radioId == R.id.terrain_map) {
			mapId = GoogleMap.MAP_TYPE_TERRAIN;
		} else {
			mapId = GoogleMap.MAP_TYPE_NORMAL;
		}

		// Format the options into a Json string.
		org.json.JSONObject json;
		try {
			json = ((fnsb.macstransit.Settings.V2) CurrentSettings.settingsImplementation).
					formatSettingsToJsonString(this.activity.trafficBox.isChecked(),
							this.activity.darkthemeBox.isChecked(), this.activity.polyBox.isChecked(),
							this.activity.streetviewBox.isChecked(), mapId, favoritedRoutes);
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(v.getContext(), "An exception occurred while applying settings",
					Toast.LENGTH_LONG).show();
			return;
		}

		// Write that string to the file
		CurrentSettings.settingsImplementation.writeSettingsToFile(json.toString(), this.activity);

		// Reload the settings.
		try {
			CurrentSettings.settingsImplementation.parseSettings(json);
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(v.getContext(), "An exception occurred while reloading settings",
					Toast.LENGTH_LONG).show();
		}

		// Close the activity.
		this.activity.finish();
	}

	/**
	 * Gets the favorited routes from the favorited routes container.
	 *
	 * @return The array of selected favorited routes.
	 */
	@androidx.annotation.NonNull
	private Route[] getFavoritedRoutes() {

		// Get the number of potential favorite routes.
		int potentialRoutesCount = this.activity.favoriteContainer.getChildCount();
		Log.d("getFavoritedRoutes", "Potential count: " + potentialRoutesCount);

		// Create an array of potential routes.
		// Since we know the maximum just use that as its starting size.
		Route[] potentialRoutes = new Route[potentialRoutesCount];
		int routesPosition = 0;

		// Iterate though each radio button in the favorites container.
		for (int i = 0; i < potentialRoutesCount; i++) {

			// Get a specific checkbox from the favorites container.
			CheckBox box = (CheckBox) this.activity.favoriteContainer.getChildAt(i);

			// Add the route to the array if its checked.
			if (box.isChecked()) {
				potentialRoutes[routesPosition] = (Route) box.getTag();
				//noinspection ObjectAllocationInLoop
				Log.d("getFavoritedRoutes", "Adding route " + potentialRoutes[routesPosition].routeName);

				// Add one to a tally of verified favorite routes.
				routesPosition++;
			}
		}

		// Create a new route array, and be sure to fit it to its actual size.
		Route[] routes = new Route[routesPosition];
		System.arraycopy(potentialRoutes, 0, routes, 0, routesPosition);

		// Return the newly created array.
		return routes;
	}
}
