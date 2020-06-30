package fnsb.macstransit.Activities.ActivityListeners;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;

import org.jetbrains.annotations.NotNull;
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
public class ApplySettings implements View.OnClickListener {

	/**
	 * The activity that corresponds to this listener.
	 */
	private final SettingsActivity activity;

	/**
	 * Constructor for the on click listener.
	 * @param activity The activity that corresponds to this listener.
	 */
	public ApplySettings(SettingsActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onClick(View v) {
		// Get the favorited routes from the activity.
		Route[] favoritedRoutes = this.getFavoritedRoutes();

		// Determine the map type
		int mapId;
		switch (this.activity.mapType.getCheckedRadioButtonId()) {
			case R.id.satellite_map:
				mapId = GoogleMap.MAP_TYPE_SATELLITE;
				break;
			case R.id.terrain_map:
				mapId = GoogleMap.MAP_TYPE_TERRAIN;
				break;
			default:
				mapId = GoogleMap.MAP_TYPE_NORMAL;
				break;
		}

		// Format the options into a Json string
		org.json.JSONObject json;
		try {
			json = CurrentSettings.settings.formatSettingsToJsonString(this.activity.trafficBox.isChecked(),
					this.activity.darkthemeBox.isChecked(), this.activity.polyBox.isChecked(),
					this.activity.streetviewBox.isChecked(), mapId, favoritedRoutes);
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(v.getContext(), "An exception occurred while applying settings",
					Toast.LENGTH_LONG).show();
			return;
		}

		// Write that string to the file
		CurrentSettings.settings.writeStringToFile(json.toString(), this.activity);

		// Reload the settings
		try {
			CurrentSettings.settings.parseSettings(json);
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(v.getContext(), "An exception occurred while reloading settings",
					Toast.LENGTH_LONG).show();
		}

		// Close the activity
		this.activity.finish();
	}

	/**
	 * Gets the favorited routes from the favorited routes container.
	 * @return The array of selected favorited routes.
	 */
	@NotNull
	private Route[] getFavoritedRoutes() {
		// Get the number of potential favorite routes
		int potentialRoutesCount = this.activity.favoriteContainer.getChildCount();
		Log.d("getFavoritedRoutes", "Potential count: " + potentialRoutesCount);

		// Determine the selected routes
		Route[] potentialRoutes = new Route[potentialRoutesCount];
		int routesPosition = 0;
		for (int i = 0; i < potentialRoutesCount; i++) {
			CheckBox box = (CheckBox) this.activity.favoriteContainer.getChildAt(i);
			if (box.isChecked()) {
				potentialRoutes[routesPosition] = (Route) box.getTag();
				Log.d("getFavoritedRoutes", "Adding route " + potentialRoutes[routesPosition].routeName);
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
