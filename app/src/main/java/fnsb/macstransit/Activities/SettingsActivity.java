package fnsb.macstransit.Activities;

import android.os.Build;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;

import fnsb.macstransit.Activities.ActivityListeners.ApplySettings;
import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.Settings.CurrentSettings;

/**
 * Created by Spud on 2019-11-24 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 2.0
 * @since Beta 8.
 */
public class SettingsActivity extends AppCompatActivity {

	/**
	 * TODO Documentation
	 */
	public CheckBox trafficBox, darkthemeBox, polyBox, streetviewBox;

	/**
	 * TODO Documentation
	 */
	public RadioGroup mapType;

	/**
	 * TODO Documentation
	 */
	public LinearLayout favoriteContainer;

	/**
	 * TODO Documentation
	 *
	 * @param savedInstanceState
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the layout view to the settings view
		this.setContentView(R.layout.settings);

		// Setup the fixed checkboxes
		this.trafficBox = this.findViewById(R.id.traffic);
		this.trafficBox.setChecked(CurrentSettings.settings.getTraffic());

		this.darkthemeBox = this.findViewById(R.id.night_mode);
		this.darkthemeBox.setChecked(CurrentSettings.settings.getDarktheme());

		this.polyBox = this.findViewById(R.id.polylines);
		this.polyBox.setChecked(CurrentSettings.settings.getPolylines());

		this.streetviewBox = this.findViewById(R.id.VR);
		this.streetviewBox.setChecked(CurrentSettings.settings.getStreetView());

		// Setup the radio buttons
		this.mapType = this.findViewById(R.id.map_group);
		switch (CurrentSettings.settings.getMaptype()) {
			case GoogleMap.MAP_TYPE_SATELLITE:
				this.mapType.check(R.id.satellite_map);
				break;
			case GoogleMap.MAP_TYPE_TERRAIN:
				this.mapType.check(R.id.terrain_map);
				break;
			default:
				this.mapType.check(R.id.normal_map);
				break;
		}

		// Setup the favorites container
		this.favoriteContainer = this.findViewById(R.id.favorite_route_container);
		this.addToFavoritesContainer();

		// Setup the buttons
		this.findViewById(R.id.apply).setOnClickListener(new ApplySettings(this));
		this.findViewById(R.id.cancel).setOnClickListener((v) -> this.finish());
	}

	/**
	 * TODO Documentation
	 */
	private void addToFavoritesContainer() {
		// Iterate through all the routes (if there are any).
		if (MapsActivity.allRoutes != null) {
			for (Route route : MapsActivity.allRoutes) {
				CheckBox checkBox = new CheckBox(this);
				checkBox.setText(route.routeName);
				checkBox.setTextSize(15);
				checkBox.setTextColor(this.getResources().getColor(R.color.white));

				// Add button tint if the sdk supports it.
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					checkBox.setButtonTintList(this.getResources().getColorStateList(R.color.white));
				}

				checkBox.setTag(route);

				// Determine if the box should be checked
				for (Route savedRoute : CurrentSettings.settings.getRoutes()) {
					if (savedRoute.routeName.equals(route.routeName)) {
						checkBox.setChecked(true);
						break;
					}
				}

				// Add the box to the favorites container
				this.favoriteContainer.addView(checkBox);
			}
		}
	}
}
