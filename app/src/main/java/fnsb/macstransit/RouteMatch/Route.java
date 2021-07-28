package fnsb.macstransit.RouteMatch;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.6.
 * @since Beta 3.
 */
public class Route {

	/**
	 * Fallback empty route array used as a way to circumvent potential exceptions.
	 */
	public static final Route[] EMPTY_ROUTE = new Route[0];

	/**
	 * The name of the route.
	 */
	public final String routeName;

	/**
	 * The name of the route formatted to be parsed as a URL.
	 */
	public final String urlFormattedName; // TODO Add regex check

	/**
	 * The color of the route.
	 * This is optional, as there is a high chance that the route does not have one.
	 * <p>
	 * This is an int instead of a Color object as android stores its colors as an integer.
	 */
	public int color;

	/**
	 * The array of stops for this route.
	 * This may be empty / null if the route has not been initialized,
	 * and the stops haven't been loaded.
	 */
	@Nullable
	public Stop[] stops;

	/**
	 * Whether or not the route is enabled or disabled (to be shown or hidden).
	 * Default is false (disabled).
	 */
	public boolean enabled = false;

	/**
	 * The array of LatLng coordinates that will be used to create the polyline (if enabled).
	 * This is private as we don't want this variable to be set outside the class.
	 */
	private LatLng[] polyLineCoordinates;

	/**
	 * The array of shared stops for this route.
	 * This may be empty/ null if the route has not been initialized,
	 * and the the shared stops haven't been loaded, or if there are no shared stops for the route.
	 * <p>
	 * The reason why this is private access is because there is a special way this should be set within the route class.
	 */
	private SharedStop[] sharedStops;

	/**
	 * The polyline that corresponds to this route.
	 * The reason why this is private access is because there is a special way this should be set within the route class.
	 */
	private Polyline polyline;

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the route. Be sure this does <b>NOT</b>
	 *                  contain any whitespace characters!
	 * @throws UnsupportedEncodingException Thrown if the route name cannot be formatted to a URL.
	 */
	public Route(@NonNull String routeName) throws UnsupportedEncodingException {
		this(routeName, 0);
	}

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the route. Be sure this does <b>NOT</b>
	 *                  contain any whitespace characters!
	 * @param color     The route's color. This is optional,
	 *                  and of the color is non-existent simply use the
	 *                  {@code Route(String routeName)} constructor.
	 * @throws UnsupportedEncodingException Thrown if the route name cannot be formatted to a URL.
	 */
	public Route(String routeName, int color) throws UnsupportedEncodingException {

		// Set the route name.
		this.routeName = routeName;

		// Parse and set the url from the name.
		// The URL encoding that the RouteMatch API uses is slightly different than that provided by the URL encoder
		this.urlFormattedName = java.util.regex.Pattern.compile("\\+").matcher(java.net.URLEncoder.
				encode(routeName, "UTF-8")).replaceAll("%20");

		// Set the route color.
		this.color = color;
	}

	/**
	 * Generates the array of routes from the master schedule json array.
	 * If a null array is provided then an empty array will be returned.
	 *
	 * @param masterSchedule The json array containing the master schedule containing all the routes.
	 * @return The routes derived from the master schedule.
	 */
	@NonNull
	public static Route[] generateRoutes(JSONArray masterSchedule) {

		// Make sure the master schedule is not null. If it is, return an empty route array.
		if (masterSchedule == null) {
			Log.w("generateRoutes", "Master schedule is null!");
			return Route.EMPTY_ROUTE;
		}

		// Create an array to store all the generated routes.
		int count = masterSchedule.length();
		Route[] potentialRoutes = new Route[count];
		int routeCount = 0;

		// Iterate though each route in the master schedule.
		for (int index = 0; index < count; index++) {
			Log.d("generateRoutes", String.format("Parsing route %d/%d", index + 1, count));

			// Try to get the route data from the array.
			// If there's an issue parsing the data simply continue to the next iteration of the loop.
			JSONObject routeData;
			try {
				routeData = masterSchedule.getJSONObject(index);
			} catch (JSONException e) {
				Log.w("generateRoutes", "Issue retrieving the route data", e);
				continue;
			}

			// Try to create the route using the route data obtained above.
			// If there was a route exception thrown simply log it.
			try {
				Route route = Route.generateRoute(routeData);
				potentialRoutes[routeCount] = route;
				routeCount++;
			} catch (RouteException | UnsupportedEncodingException e) {
				Log.w("generateRoutes", "Issue creating route from route data", e);
			}
		}

		// Down size our potential routes array to fit the actual number of routes.
		Route[] routes = new Route[routeCount];
		System.arraycopy(potentialRoutes, 0, routes, 0, routeCount);
		return routes;
	}

	/**
	 * Creates a new route object from the provided json object.
	 * If the json object is null then a RouteException will be thrown.
	 *
	 * @param jsonObject The json object contain the data to create a new route object.
	 * @return The newly created route object.
	 * @throws RouteException               Thrown if the json object is null, or if the route name is unable to be parsed.
	 * @throws UnsupportedEncodingException Thrown if the route name cannot be formatted to a URL.
	 */
	@NonNull
	private static Route generateRoute(JSONObject jsonObject) throws RouteException, UnsupportedEncodingException {

		// Make sure the provided json object is not null.
		if (jsonObject == null) {
			throw new RouteException("Json object cannot be null!");
		}

		// First, parse the name.
		String name;
		try {
			name = jsonObject.getString("routeId");
		} catch (JSONException e) {
			throw new RouteException("Unable to get route name from JSON", e.getCause());
		}

		// Now try to parse the route and route color.
		Route route;
		try {
			String colorName = jsonObject.getString("routeColor");
			int color = android.graphics.Color.parseColor(colorName);

			route = new Route(name, color);

		} catch (JSONException | IllegalArgumentException e) {
			Log.w("generateRoute", "Unable to parse color");

			// Since there was an issue parsing the color, and we have the name at this point...
			// Simply create the route without a color.
			route = new Route(name);
		}

		// Return the newly created route.
		return route;
	}

	/**
	 * Iterates though all the routes in MapsActivity.allRoutes
	 * and enables those that have been favorited (as determined by being in the favoritedRoutes array).
	 * <p>
	 * This should only be run once.
	 *
	 * @param favoritedRoutes The selected routes to be enabled from MapsActivity.allRoutes.
	 */
	public static void enableFavoriteRoutes(Route[] favoritedRoutes) {

		// Make sure there are routes to iterate over.
		if (MapsActivity.allRoutes == null || favoritedRoutes == null) {
			return;
		}

		// Iterate through all the routes that will be used in the activity.
		for (Route allRoute : MapsActivity.allRoutes) {

			// Iterate though the favorite routes.
			for (Route favoritedRoute : favoritedRoutes) {

				// Make sure the favorited route and the comparison route aren't null.
				if (favoritedRoute != null && allRoute != null) {

					// If the route name matches the favorited route name, enable it.
					if (allRoute.routeName.equals(favoritedRoute.routeName)) {
						allRoute.enabled = true;
						break;
					}
				} else {

					// If either of the routes were null, log it.
					Log.w("enableFavoriteRoutes", "Route entry in array is null");
				}
			}
		}

		// Set the selectedFavorites variable to be true as to not run again.
		MapsActivity.selectedFavorites = true;
	}

	/**
	 * Starts an asynchronous process to fetch all the stops for the route from the RouteMatch API.
	 */
	public void loadStops() {

		// Get all the stops for the route from the RouteMatch object.
		MapsActivity.routeMatch.callAllStops(this, result -> {

			// Get the data from all the stops and store it in a JSONArray.
			JSONArray data = RouteMatch.parseData(result);

			// Load in all the potential stops for the route.
			// The reason why this is considered potential stops is because at this stage duplicate
			// stops have not yet been handled.
			Stop[] potentialStops = Stop.generateStops(data, this);

			// At this point duplicate stops have now been handled and removed.
			this.stops = Stop.validateGeneratedStops(potentialStops);

		}, error -> Log.w("loadStops", "Unable to get stops from RouteMatch server", error), this);
	}

	/**
	 * Loads the polyline coordinates for the route object by retrieving the array from the RouteMatch server.
	 * This method will either set the polyline coordinates for the route,
	 * or will return early if the route match object is null.
	 */
	public void loadPolyLineCoordinates() {

		// Make sure the RouteMatch object exists.
		if (MapsActivity.routeMatch == null) {
			Log.w("loadPolyLineCoordinates", "RouteMatch object is null!");
			return;
		}

		// Get the land route from the routematch API using an asynchronous process.
		MapsActivity.routeMatch.callLandRoute(this, response -> {

			try {
				// Get the land route data array from the land route object.
				JSONArray landRouteData = RouteMatch.parseData(response);

				// Get the land route points object from the land route data array.
				JSONObject landRoutePoints = landRouteData.getJSONObject(0);

				// Get the land route points array from the land route points object.
				JSONArray landRoutePointsArray = landRoutePoints.getJSONArray("points");

				// Get the number of points in the array.
				int count = landRoutePointsArray.length();

				// Create a new LatLng array to store all the coordinates.
				LatLng[] coordinates = new LatLng[count];

				// Initialize the array of coordinates by iterating through the land route points array.
				for (int i = 0; i < count; i++) {

					// Get the land route point object from the land route points array.
					JSONObject landRoutePoint = landRoutePointsArray.getJSONObject(i);

					// Get the latitude and longitude from the land route point.
					double latitude = landRoutePoint.getDouble("latitude"),
							longitude = landRoutePoint.getDouble("longitude");

					// Create a new LatLng object using the latitude and longitude.
					LatLng latLng = new LatLng(latitude, longitude);

					// Add the newly created LatLng object to the LatLng array.
					coordinates[i] = latLng;
				}

				// Set the polyline coordinates array to the finished LatLng array.
				this.polyLineCoordinates = coordinates;

			} catch (JSONException exception) {
				Log.e("loadPolyLineCoordinates", "Error parsing json", exception);
			}
		}, error -> Log.w("loadPolyLineCoordinates",
				"Unable to get polyline from routematch server", error), this);

	}

	/**
	 * Returns the polyline that corresponds to the route.
	 *
	 * @return The polyline that corresponds to the route.
	 */
	public Polyline getPolyline() {
		return this.polyline;
	}

	/**
	 * Creates and sets the polyline for the route.
	 * If there are no polyline coordinates for the route then this simply returns early and does not create the polyline.
	 */
	@UiThread
	public void createPolyline() {
		Log.v("createPolyline", "Creating route polyline");

		// Make sure the polyline coordinates is not null or 0. If it is then return early.
		if (this.polyLineCoordinates == null || this.polyLineCoordinates.length == 0) {
			Log.w("createPolyline", "There are no polyline coordinates to work with!");
			return;
		}

		// Make sure the map isn't null.
		if (MapsActivity.map == null) {
			Log.w("createPolyline", "Map is not yet ready!");
		}

		// Create new polyline options from the array of polyline coordinates stored for the route.
		PolylineOptions options = new PolylineOptions().add(this.polyLineCoordinates);

		// Make sure its not clickable.
		options.clickable(false);

		// Set the color of the polylines based on the route color.
		options.color(this.color);

		// Make sure the polyline starts out invisible.
		options.visible(false);

		// Add the polyline to the map, and set it for the object.
		this.polyline = MapsActivity.map.addPolyline(options);
	}

	/**
	 * Adds the shared to the routes shared stop array.
	 *
	 * @param sharedStop The shared stop to add to the route.
	 */
	public void addSharedStop(SharedStop sharedStop) {

		// Create a new shared stop array that will contain our current shared stop array + the new shared stop.
		SharedStop[] newSharedStops;

		if (this.sharedStops == null) {

			// If there was no shared stop array before then simply set the array to just contain our shared stop.
			newSharedStops = new SharedStop[]{sharedStop};
		} else {

			// Since our current array of shared stops has content
			// simply insert our shared stop to the array and copy the rest using System.arraycopy.
			newSharedStops = new SharedStop[this.sharedStops.length + 1];
			newSharedStops[0] = sharedStop;
			System.arraycopy(this.sharedStops, 0, newSharedStops, 1, this.sharedStops.length);
		}

		// Set the routes shared stop array to the newly created shared stop array.
		this.sharedStops = newSharedStops;
	}

	/**
	 * Gets the shared stops for the route. This may be null if there are none.
	 *
	 * @return The shared stops for the route.
	 */
	@Nullable
	public SharedStop[] getSharedStops() {
		return this.sharedStops;
	}

	/**
	 * Gets the LatLng object array for the polyline coordinates for the route.
	 *
	 * @return The polyline coordinates for the route as a LatLng array.
	 */
	@Nullable
	public LatLng[] getPolyLineCoordinates() {
		return this.polyLineCoordinates;
	}

	/**
	 * Removes the routes polyline from the map, and sets it to null.
	 * This must be run on the UI thread.
	 */
	@UiThread
	public void removePolyline() {
		if (this.polyline != null) {
			this.polyline.remove();
			this.polyline = null;
		}
	}

	/**
	 * Exception class used for throwing any exception relating to Routes.
	 */
	public static class RouteException extends Exception {

		/**
		 * Constructor for a new exception with a (hopefully detailed) message.
		 *
		 * @param message The (ideally detailed) message for why this was thrown.
		 */
		public RouteException(String message) {
			super(message);
		}

		/**
		 * Constructor for a new exception with a (hopefully detailed) message, and a cause.
		 *
		 * @param message The (ideally detailed) message.
		 * @param cause   The cause for the exception. This may be null if the cause is undetermined.
		 */
		public RouteException(String message, @Nullable Throwable cause) {
			super(message, cause);
		}

		/**
		 * Constructor for a new exception with a cause.
		 *
		 * @param cause The cause for the exception. This may be null if the cause is undetermined.
		 */
		public RouteException(@Nullable Throwable cause) {
			super(cause);
		}
	}
}
