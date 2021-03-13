package fnsb.macstransit.Settings;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class v2 {

	/**
	 * The filename of the settings file.
	 */
	public static final String FILENAME = "Settings.json";

	/**
	 * The version of the settings file.
	 */
	public static final int VERSION = 2;

	/**
	 * Static variables used by the app.
	 */
	public static boolean traffic = false, darktheme = false, polylines = false, streetview = false;

	/**
	 * Static variable used by the app. This variable corresponds with what map type should be used.
	 */
	public static int maptype = GoogleMap.MAP_TYPE_NORMAL;

	/**
	 * Favorite routes set by the user.
	 * These routes should be selected as soon as the app has finished initialization.
	 */
	public static Route[] favoriteRoutes;

	/**
	 * Reads the JSON object from the settings file.
	 *
	 * @param file The settings file.
	 * @return The JSON object read from the settings file.
	 */
	public JSONObject readFromSettingsFile(File file) {
		String content = CurrentSettings.readFile(file);
		Log.d("readFromSettingsFile", "Content: " + content);

		// If the content isn't null, create a new JSON object from the content.
		if (content != null) {
			try {
				return new JSONObject(content);
			} catch (JSONException e) {
				// If there was an issue with parsing the JSON, return an empty object.
				Log.e("readFromSettingsFile", "Cannot parse JSON");
				return new JSONObject();
			}
		} else {
			// Since the content was null, return an empty JSON object.
			return new JSONObject();
		}
	}

	/**
	 * Reads the JSON object from the settings file.
	 *
	 * @param context The context to get the settings file by.
	 * @return The JSON object read from the settings file.
	 */
	public JSONObject readFromSettingsFile(@NotNull Context context) {
		// Get the file from the context.
		File file = new File(context.getFilesDir(), v2.FILENAME);
		Log.i("readFromSettingsFile", "Supposed file location: " + file.getAbsolutePath());

		// Return the JSON object from the file determined by the context.
		return this.readFromSettingsFile(file);
	}

	/**
	 * Formats the given arguments into a JSON object that can be written to the settings file.
	 *
	 * @param bTraffic Whether or not to show the traffic overlay.
	 * @param bDarktheme Whether or not to launch with the dark theme.
	 * @param bPolylines Whether to not to show polylines.
	 * @param bStreetview Whether or not to enable the (deprecated) streetview feature.
	 * @param iMapType What type of map to use.
	 * @param rFavoriteRoutes An array of favorited routes defined by the user.
	 * @return The formatted JSON object.
	 * @throws JSONException Thrown if there are any issues parsing the arguments provided.
	 */
	public JSONObject formatSettingsToJsonString(boolean bTraffic, boolean bDarktheme, boolean bPolylines,
	                                             boolean bStreetview, int iMapType, Route... rFavoriteRoutes)
			throws JSONException {

		// Create a new JSON object to hold all the setting values.
		JSONObject parent = new JSONObject();

		// Add all the the simple key value pairs to the parent JSON object.
		parent.putOpt("version", v2.VERSION)
				.putOpt("enable traffic view", bTraffic)
				.putOpt("enable dark theme", bDarktheme)
				.putOpt("enable polylines", bPolylines)
				.putOpt("enable streetview", bStreetview)
				.putOpt("map type", iMapType);

		// Create a JSON array for all the favorite routes.
		JSONArray favoritedRoutes = new JSONArray();
		for (Route route : rFavoriteRoutes) {
			favoritedRoutes.put(route.routeName);
		}

		// Add the favorite routes JSON array to the parent JSON object.
		parent.putOpt("favorited routes", favoritedRoutes);

		Log.d("formatSettingsJsnString", "Formatted Json: " + parent);
		return parent;
	}

	/**
	 * Writes the provided string to the settings file.
	 *
	 * @param string  The string to be written to the settings file.
	 * @param context The app context (for determining where the file is).
	 */
	public void writeStringToFile(String string, Context context) {
		// Try opening the settings file.
		try (java.io.FileOutputStream outputStream = context.openFileOutput(v2.FILENAME, Context.MODE_PRIVATE)) {
			// Write the string to the file.
			Log.d("writeStringToFile", "Writing string: " + string);
			outputStream.write(string.getBytes());
			outputStream.flush();
		} catch (java.io.IOException e) {
			// Notify of any issues writing the file.
			Toast.makeText(context, "Unable write settings to file", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new settings file with default values.
	 *
	 * @param context The app context used to determine the file location.
	 */
	public void createSettingsFile(Context context) {
		Log.v("createSettingsFile", "Creating new settings file");
		try {
			// Create a new JSON object with all the default settings.
			JSONObject json = CurrentSettings.settingsImplementation.formatSettingsToJsonString(false,
					false, false, false, GoogleMap.MAP_TYPE_NORMAL);

			// Write those settings to the file.
			CurrentSettings.settingsImplementation.writeStringToFile(json.toString(), context);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parses settings to be applied to the static variables from the provided JSON object.
	 *
	 * @param json The JSON object containing the values to be parsed into settings.
	 * @throws JSONException Thrown if there was an issue with parsing any values.
	 */
	public void parseSettings(@NotNull JSONObject json) throws JSONException {
		// Parse the simpler JSON objects from the settings file first.
		v2.traffic = json.getBoolean("enable traffic view");
		v2.darktheme = json.getBoolean("enable dark theme");
		v2.polylines = json.getBoolean("enable polylines");
		v2.streetview = json.getBoolean("enable streetview");
		v2.maptype = json.getInt("map type");

		// Now try to parse the more dynamic content (favorited routes array).
		ArrayList<Route> routes = new ArrayList<>();
		JSONArray favoritedRoutes = json.getJSONArray("favorited routes");

		// Iterate through the JSON array and try to match the names of the routes.
		for (int i = 0; i < favoritedRoutes.length(); i++) {
			String routeName = favoritedRoutes.getString(i);
			if (MapsActivity.allRoutes != null) {
				for (Route route : MapsActivity.allRoutes) {
					// If the route names match, add it to the list of routes.
					if (routeName.equals(route.routeName)) {
						routes.add(route);
						break;
					}
				}
			}
		}

		// Parse the list of routes into the favorite routes array.
		v2.favoriteRoutes = routes.toArray(new Route[0]);
	}

	/**
	 * Gets the map type static variable.
	 *
	 * @return The map type value.
	 */
	public int getMaptype() {
		return v2.maptype;
	}

	/**
	 * Gets the traffic static variable.
	 *
	 * @return The traffic boolean (if the traffic view should be shown).
	 */
	public boolean getTraffic() {
		return v2.traffic;
	}

	/**
	 * Gets the dark theme static variable.
	 *
	 * @return The dark theme boolean (if dark theme should be set on launch).
	 */
	public boolean getDarktheme() {
		return v2.darktheme;
	}

	/**
	 * Gets the polyline static variable.
	 *
	 * @return The polyline boolean (if they should be shown or not).
	 */
	public boolean getPolylines() {
		return v2.polylines;
	}

	/**
	 * Gets the streetview static variable.
	 *
	 * @return The streetview boolean.
	 */
	public boolean getStreetView() {
		return v2.streetview;
	}

	/**
	 * Gets the favorited routes static variable.
	 *
	 * @return The favorite routes defined by the user.
	 */
	public Route[] getRoutes() {
		return v2.favoriteRoutes;
	}

}
