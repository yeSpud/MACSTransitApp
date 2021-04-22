package fnsb.macstransit.Settings;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

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
public class v2 extends BaseSettings {

	/**
	 * Settings variables used by the app.
	 */
	private boolean traffic = false, darktheme = false, polylines = false, streetview = false;

	/**
	 * Settings variable used by the app. This variable corresponds with what map type should be used.
	 */
	private int maptype = GoogleMap.MAP_TYPE_NORMAL;

	/**
	 * Favorite routes set by the user.
	 * These routes should be enabled / selected as soon as the app has finished initialization.
	 */
	public Route[] favoriteRoutes;

	/**
	 * Constructor for v2.
	 * Because this class extends BaseSettings, the file and version need to be passed.
	 */
	public v2() {
		super("Settings.json", 2);
	}

	/**
	 * Reads the JSON object from the settings file.
	 *
	 * @param file The settings file.
	 * @return The JSON object read from the settings file.
	 */
	@Override
	public JSONObject readFromSettingsFile(File file) {

		// Load the content from the file via a call to readFile.
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
	@Override
	public JSONObject readFromSettingsFile(@NonNull Context context) {

		// Get the file from the context.
		File file = new File(context.getFilesDir(), this.FILENAME);
		Log.i("readFromSettingsFile", "Supposed file location: " + file.getAbsolutePath());

		// Return the JSON object from the file determined by the context.
		return this.readFromSettingsFile(file);
	}

	/**
	 * Formats the given arguments into a JSON object that can be written to the settings file.
	 *
	 * @param bTraffic        Whether or not to show the traffic overlay.
	 * @param bDarktheme      Whether or not to launch with the dark theme.
	 * @param bPolylines      Whether to not to show polylines.
	 * @param bStreetview     Whether or not to enable the (deprecated) streetview feature.
	 * @param iMapType        What type of map to use.
	 * @param rFavoriteRoutes An array of favorited routes defined by the user.
	 * @return The formatted JSON object.
	 * @throws JSONException Thrown if there are any issues parsing the arguments provided.
	 */
	public JSONObject formatSettingsToJsonString(boolean bTraffic, boolean bDarktheme, boolean bPolylines,
	                                             boolean bStreetview, int iMapType,
	                                             @NonNull Route... rFavoriteRoutes) throws JSONException {

		// Create a new JSON object to hold all the setting values.
		JSONObject parent = new JSONObject();

		// Add all the the simple key value pairs to the parent JSON object.
		parent.putOpt("version", this.VERSION)
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
	 * @param string  The string to be written to the settings file. This cannot be null.
	 * @param context The app context (for determining where the file is). This cannot be null.
	 */
	@Override
	public void writeSettingsToFile(@NonNull String string, @NonNull Context context) {

		// Try opening the settings file.
		try (java.io.FileOutputStream outputStream = context.openFileOutput(this.FILENAME, Context.MODE_PRIVATE)) {

			// Write the string to the file.
			Log.d("writeStringToFile", "Writing string: " + string);
			outputStream.write(string.getBytes());
			outputStream.flush();
		} catch (java.io.FileNotFoundException e) {

			// Log that the file wasn't found, and notify the user.
			Log.e("writeSettingsToFile", "File was not found", e);
			Toast.makeText(context, "Unable to find settings file", Toast.LENGTH_LONG).show();
		} catch (java.io.IOException e) {

			// Notify of any issues writing the file, and log it.
			Log.e("writeSettingsToFile", "Unable to write to file", e);
			Toast.makeText(context, "Unable write settings to file", Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Creates a new settings file with default values.
	 *
	 * @param context The app context used to determine the file location.
	 */
	@Override
	public void createSettingsFile(Context context) {
		Log.i("createSettingsFile", "Creating new settings file");

		try {
			// Create a new JSON object with all the default settings.
			JSONObject json = this.formatSettingsToJsonString(false, false,
					false, false, GoogleMap.MAP_TYPE_NORMAL);

			// Write those settings to the file.
			this.writeSettingsToFile(json.toString(), context);
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
	@Override
	public void parseSettings(@NonNull JSONObject json) throws JSONException {

		// Parse the simpler JSON objects from the settings file first.
		this.traffic = json.getBoolean("enable traffic view");
		this.darktheme = json.getBoolean("enable dark theme");
		this.polylines = json.getBoolean("enable polylines");
		this.streetview = json.getBoolean("enable streetview");
		this.maptype = json.getInt("map type");

		// Make sure all routes is not null before loading favorite routes.
		if (MapsActivity.allRoutes == null) {
			Log.w("parseSettings", "All routes is null!");
			return;
		}

		// Now try to parse the more dynamic content (favorited routes array).
		JSONArray favoritedRoutes = json.getJSONArray("favorited routes");
		int favoriteCount = favoritedRoutes.length();
		Route[] routes = new Route[favoriteCount];

		// Iterate through the JSON array and try to match the names of the routes.
		for (int i = 0; i < favoritedRoutes.length(); i++) {
			String routeName = favoritedRoutes.getString(i);
			for (Route route : MapsActivity.allRoutes) {

				// If the route names match, add it to the list of routes.
				if (routeName.equals(route.routeName)) {
					routes[i] = route;
					break;
				}
			}
		}

		// Parse the list of routes into the favorite routes array.
		this.favoriteRoutes = routes;
	}

	/**
	 * Gets the map type variable.
	 *
	 * @return The map type value.
	 */
	public int getMaptype() {
		return this.maptype;
	}

	/**
	 * Gets the traffic variable.
	 *
	 * @return The traffic boolean (if the traffic view should be shown).
	 */
	public boolean getTraffic() {
		return this.traffic;
	}

	/**
	 * Gets the dark theme variable.
	 *
	 * @return The dark theme boolean (if dark theme should be set on launch).
	 */
	public boolean getDarktheme() {
		return this.darktheme;
	}

	/**
	 * Gets the polyline variable.
	 *
	 * @return The polyline boolean (if they should be shown or not).
	 */
	public boolean getPolylines() {
		return this.polylines;
	}

	/**
	 * Gets the streetview variable.
	 *
	 * @return The streetview boolean.
	 */
	public boolean getStreetView() {
		return this.streetview;
	}

	/**
	 * Gets the favorited routes variable.
	 *
	 * @return The favorite routes defined by the user.
	 */
	public Route[] getRoutes() {
		return this.favoriteRoutes;
	}

}
