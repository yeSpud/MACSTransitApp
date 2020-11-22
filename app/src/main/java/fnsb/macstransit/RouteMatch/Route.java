package fnsb.macstransit.RouteMatch;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.5.
 * @since Beta 3.
 */
public class Route {

	/**
	 * The name of the route.
	 * Note: This cannot contain whitespace characters (ie spaces, tabs, or new lines),
	 * as its used in a url.
	 */
	public String routeName;

	/**
	 * The color of the route.
	 * This is optional, as there is a high chance that the parentRoute does not have one.
	 * <p>
	 * This is an int instead of a Color object as android stores its colors as an integer.
	 */
	public int color;

	/**
	 * The array of stops for this route.
	 * This may be empty / null if the route has not been initialized, and the stops haven't been loaded.
	 */
	public Stop[] stops;

	/**
	 * TODO Documentation
	 */
	public SharedStop[] sharedStops;

	/**
	 * The array of LatLng coordinates that will be used to create the polyline (if enabled).
	 */
	public LatLng[] polyLineCoordinates;

	/**
	 * TODO Documentation
	 */
	public boolean enabled = false;

	/**
	 * The polyline that corresponds to this route.
	 */
	private Polyline polyline;

	/**
	 * Constructor for the route. The name of the route is the only thing that is required.
	 * Be sure that the provided route name does <b>NOT</b> contain any whitespace characters!
	 *
	 * @param routeName The name of the route. Be sure this does <b>NOT</b>
	 *                  contain any whitespace characters!
	 * @throws RouteException Thrown if the route name contains white space characters.
	 */
	public Route(@NotNull String routeName) throws RouteException {
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
	 * @throws RouteException Thrown if the route name contains white space characters.
	 */
	public Route(String routeName, int color) throws RouteException {
		Pattern whitespace = Pattern.compile("\\s");
		if (whitespace.matcher(routeName).find()) {
			throw new RouteException("Route name cannot contain white space!");
		} else {
			this.routeName = routeName;
		}
		this.color = color;
	}

	/**
	 * TODO Documentation
	 * @param masterSchedule
	 * @return
	 */
	@NotNull
	public static Route[] generateRoutes(@NotNull JSONArray masterSchedule) {
		// Create an array to store all the generated routes.
		Collection<Route> routes = new ArrayList<>(0);

		// Iterate through the data array to begin parsing the childRoutes
		int count = masterSchedule.length();
		for (int index = 0; index < count; index++) {
			// Get the current progress for parsing the routes
			Log.d("generateRoutes", String.format("Parsing route %d/%d", index + 1, count));

			// Try to get the route data from the array.
			// If there's an issue parsing the data, simply go to the next iteration of the loop (continue).
			JSONObject routeData;
			try {
				routeData = masterSchedule.getJSONObject(index);
			} catch (JSONException e) {
				Log.w("generateRoutes", "Issue retrieving the route data");
				continue;
			}

			// Try to create the route using the route data obtained above.
			// If there was a route exception thrown, simply go to the next iteration of the loop (continue).
			try {
				Route route = Route.generateRoute(routeData);
				routes.add(route);
			} catch (RouteException e) {
				Log.w("generateRoutes", "Issue creating route from route data");
				// Since this is the end of the loop, there isn't a need for a continue statement.
			}
		}

		// Return the parentRoute array list as a new array of childRoutes. Yes, they are different.
		Route[] returnRoutes = new Route[routes.size()];
		routes.toArray(returnRoutes);
		return returnRoutes;
	}

	/**
	 * TODO Documentation
	 *
	 * @param jsonObject
	 * @return
	 * @throws RouteException
	 */
	@NotNull
	private static Route generateRoute(@NotNull JSONObject jsonObject) throws RouteException {
		String name;

		try {
			// First, parse the name.
			name = jsonObject.getString("routeId");
		} catch (JSONException e) {
			throw new RouteException("Unable to get route name from JSON");
		}

		Route route;

		try {
			// Now try to parse the color.
			String colorName = jsonObject.getString("routeColor");
			int color = android.graphics.Color.parseColor(colorName);

			route = new Route(name, color);

		} catch (JSONException e) {
			// TODO
			route = new Route(name);
		}

		return route;
	}

	/**
	 * TODO Documentation
	 *
	 * @param allRoutes
	 * @param favoritedRoutes
	 */
	public static void enableFavoriteRoutes(Route[] allRoutes, Route[] favoritedRoutes) {

		// Make sure there are routes to iterate over.
		if (allRoutes == null || favoritedRoutes == null) {
			return;
		}

		// Iterate through all the routes that will be used in the activity.
		for (Route allRoute : allRoutes) {

			// Iterate though the favorite routes
			for (Route favoritedRoute : favoritedRoutes) {

				// If the route name matches the favorited route name, enable it.
				if (allRoute.routeName.equals(favoritedRoute.routeName)) {
					allRoute.enabled = true;
					break;
				}
			}
		}

	}

	/**
	 * Retrieves and parses the stops for the given route.
	 * This function does not apply the stops the route.
	 * @return The array stops corresponding the route.
	 */
	public Stop[] loadStops() {

		// Get all the stops for the route from the RouteMatch object.
		JSONObject allStopsObject = MapsActivity.routeMatch.getAllStops(this);

		// Get the data from all the stops and store it in a JSONArray.
		JSONArray data = RouteMatch.parseData(allStopsObject);

		// Load in all the potential stops for the route.
		// The reason why this is considered potential stops is because at this stage duplicate
		// stops have not yet been handled.
		Stop[] potentialStops = Stop.generateStops(data, this);

		// Return the validated version of the generated stops.
		// At this point duplicate stops have now been handled and removed.
		return Stop.validateGeneratedStops(potentialStops);
	}

	/**
	 * TODO Documentation
	 */
	public void loadPolyLineCoordinates() throws JSONException {
		// Make sure the RouteMatch object exists.
		if (MapsActivity.routeMatch == null) {
			return;
		}

		// Get the land route json object from the RouteMatch server.
		JSONObject landRouteObject = MapsActivity.routeMatch.getLandRoute(this);

		// Get the land route data array from the land route object.
		JSONArray landRouteData = RouteMatch.parseData(landRouteObject);

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
	 * TODO Documentation
	 */
	public void createPolyline() {
		Log.v("createPolyline", "Creating route polyline");
		// Add the polyline based off the polyline coordinates within the parentRoute.
		PolylineOptions options = new PolylineOptions().add(this.polyLineCoordinates);

		// Make sure its not clickable.
		options.clickable(false);

		// Set the color of the polylines based on the parentRoute color.
		options.color(this.color);

		// Make sure the polyline starts out invisible.
		options.visible(false);

		// Add the polyline to the map, and return it.
		this.polyline = MapsActivity.map.addPolyline(options);
	}

	/**
	 * TODO Documentation
	 * @param sharedStop
	 */
	public void addSharedStop(SharedStop sharedStop) {
		SharedStop[] newSharedStops;

		if (this.sharedStops == null) {
			newSharedStops = new SharedStop[]{sharedStop};
		} else {
			newSharedStops = new SharedStop[this.sharedStops.length+1];
			newSharedStops[0] = sharedStop;
			System.arraycopy(this.sharedStops, 0, newSharedStops, 1, this.sharedStops.length);
		}

		this.sharedStops = newSharedStops;
	}

	/**
	 * TODO Documentation
	 */
	public static class RouteException extends Exception {

		/**
		 * TODO Documentation
		 *
		 * @param message
		 */
		public RouteException(String message) {
			super(message);
		}

		/**
		 * TODO Documentation
		 *
		 * @param message
		 * @param cause
		 */
		public RouteException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * TODO Documentation
		 *
		 * @param cause
		 */
		public RouteException(Throwable cause) {
			super(cause);
		}

	}
}
