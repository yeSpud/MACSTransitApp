package fnsb.macstransit.Activities;

import android.os.Build;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;

import fnsb.macstransit.R;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.Settings.CurrentSettings;
import fnsb.macstransit.Settings.v2;

/**
 * Created by Spud on 2019-11-24 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.1.
 * @since Beta 8.
 */
public class SettingsActivity extends androidx.appcompat.app.AppCompatActivity {

	/**
	 * Constant used to set the initial size of the text for the favorite routes check box.
	 */
	private static final int CHECKBOX_TEXT_SIZE = 15;

	/**
	 * Checkboxes that have already been manually defined in the settings layout.
	 * These boxes will have their functionality setup in the onCreate method.
	 */
	public CheckBox trafficBox, darkthemeBox, polyBox, streetviewBox;

	/**
	 * Radio group used to identify the map type to be used in the maps activity (standard, satellite, other...).
	 */
	public android.widget.RadioGroup mapType;

	/**
	 * Layout container that will contain the checkboxes for enabling, disabling,
	 * and generating favorite route entries.
	 */
	public android.widget.LinearLayout favoriteContainer;

	/**
	 * Iterates though the provided route (favorited routes),
	 * and returns if the provided route name matches any of them.
	 *
	 * @param routes    The favorited routes. This cannot be null.
	 * @param routeName The route name. This cannot be null.
	 * @return Whether the route name was found in the favorited routes.
	 */
	private static boolean isFavorited(@NonNull Route[] routes, @NonNull String routeName) {

		// Iterate though all the routes provided.
		for (Route savedRoute : routes) {

			// If the route isn't null, and the name matches then return true.
			// If not then keep iterating.
			if (savedRoute != null) {
				if (savedRoute.routeName.equals(routeName)) {
					return true;
				}
			}
		}

		// Since no names match return false.
		return false;
	}

	/**
	 * Called when the activity is starting.
	 * This is where most initialization should go:
	 * calling setContentView(int) to inflate the activity's UI,
	 * using findViewById(int) to programmatically interact with widgets in the UI,
	 * calling managedQuery(Uri, String[], String, String[], String)
	 * to retrieve cursors for data being displayed, etc.
	 * <p>
	 * You can call finish() from within this function,
	 * in which case onDestroy() will be immediately called after onCreate(Bundle)
	 * without any of the rest of the activity lifecycle (onStart(), onResume(), onPause(), etc)
	 * executing.
	 * <p>
	 * Derived classes must call through to the super class's implementation of this method.
	 * If they do not, an exception will be thrown.
	 * <p>
	 * This method must be called from the main thread of your app.
	 * If you override this method you must call through to the superclass implementation.
	 *
	 * @param savedInstanceState Bundle:
	 *                           If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
	 *                           Note: Otherwise it is null. This value may be null.
	 */
	@Override
	protected void onCreate(@androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the layout view to the settings view.
		this.setContentView(R.layout.settings);

		// Get the settings object.
		v2 settings = (v2) CurrentSettings.settingsImplementation;

		// Setup the fixed checkboxes.
		// Traffic box is used to determine whether or not to show the traffic overlay.
		this.trafficBox = this.findViewById(R.id.traffic);
		this.trafficBox.setChecked(settings.getTraffic());

		// Dark theme box is used to determine whether or not to start the ap with a dark themed map.
		this.darkthemeBox = this.findViewById(R.id.night_mode);
		this.darkthemeBox.setChecked(settings.getDarktheme());

		// Polybox is used to determine whether or not to show polylines for routes.
		this.polyBox = this.findViewById(R.id.polylines);
		this.polyBox.setChecked(settings.getPolylines());

		// Streetview box would be used to activate the streetview easter egg if it were not deprecated.
		this.streetviewBox = this.findViewById(R.id.VR);
		this.streetviewBox.setChecked(settings.getStreetView());

		// Setup the radio buttons.
		this.mapType = this.findViewById(R.id.map_group);
		switch (settings.getMaptype()) {
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

		// Setup the favorites container.
		this.favoriteContainer = this.findViewById(R.id.favorite_route_container);
		this.addToFavoritesContainer(settings.getRoutes());

		// Setup the buttons.
		// The apply settings button should run the apply settings listener.
		this.findViewById(R.id.apply).setOnClickListener(new fnsb.macstransit.Activities.
				ActivityListeners.ApplySettings(this));

		// The cancel button should just finish the class and return.
		this.findViewById(R.id.cancel).setOnClickListener((v) -> this.finish());
	}

	/**
	 * Creates new favorite route checkboxes for all the routes that can be tracked,
	 * and adds them to the favorite routes container.
	 *
	 * @param favoritedRoutes The array of favorited routes to enable.
	 */
	private void addToFavoritesContainer(Route[] favoritedRoutes) {

		// Make sure there are routes to iterate though.
		if (MapsActivity.allRoutes == null) {
			return;
		}

		// Iterate though all the routes.
		for (Route route : MapsActivity.allRoutes) {

			// Create a new checkbox.
			CheckBox checkBox = new CheckBox(this);

			// Set the checkbox's text to the route name.
			checkBox.setText(route.routeName);

			// Set the color and size of the text to constants.
			checkBox.setTextSize(SettingsActivity.CHECKBOX_TEXT_SIZE);
			checkBox.setTextColor(this.getResources().getColor(R.color.white));

			// Add button tint if the sdk supports it.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				checkBox.setButtonTintList(androidx.appcompat.content.res.AppCompatResources.
						getColorStateList(this, R.color.white));
			}

			// Set the checkbox tag to the route object.
			checkBox.setTag(route);

			// If the favorited route object is not null, set the checkbox to its enabled value.
			if (favoritedRoutes != null) {
				checkBox.setChecked(isFavorited(favoritedRoutes, route.routeName));
			}

			// Add the box to the favorites container.
			this.favoriteContainer.addView(checkBox);
		}
	}
}
