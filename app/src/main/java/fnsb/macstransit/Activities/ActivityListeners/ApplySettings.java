package fnsb.macstransit.Activities.ActivityListeners;

import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONException;

import java.util.ArrayList;

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
	 * TODO Documentation
	 */
	private SettingsActivity activity;

	/**
	 * TODO Documentation
	 * @param activity
	 */
	public ApplySettings(SettingsActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onClick(View v) {
		// TODO Comments

		// Determine the selected routes
		ArrayList<Route> favoritedRoutes = new ArrayList<>();
		for (int i = 0; i < this.activity.favoriteContainer.getChildCount(); i++) {
			CheckBox box = (CheckBox) this.activity.favoriteContainer.getChildAt(i);
			if (box.isChecked()) {
				favoritedRoutes.add((Route) box.getTag());
			}
		}

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
					this.activity.streetviewBox.isChecked(), mapId, favoritedRoutes.toArray(new Route[0]));
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
}
