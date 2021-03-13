package fnsb.macstransit.RouteMatch;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import fnsb.macstransit.Activities.MapsActivity;

/**
 * Created by Spud on 2019-11-01 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Beta 7.
 */
public class SharedStop extends MarkedObject {

	/**
	 * Array of routes that share this one shared stop.
	 */
	public final Route[] routes;

	/**
	 * Array of circle options for each circle that represents a route.
	 */
	public final CircleOptions[] circleOptions;

	/**
	 * Array of circles that represent a route that shares this one stop.
	 */
	public final Circle[] circles;

	/**
	 * Location (as a LatLng object) of the shared stop.
	 */
	public final LatLng location;

	/**
	 * The initial size to set the largest circle to.
	 * Each of the subsequent circles are set to a smaller size dependent on this constant.
	 */
	public static final double INITIAL_CIRCLE_SIZE = 12.0d;

	/**
	 * Constructor for SharedStop. This not only sets the location, route, and name,
	 * but also initializes the circle options for the shared stop.
	 * <p>
	 * It should be noted that while the circle options are set the circles are not initialized at this point.
	 *
	 * @param latLng   The location of the shared stop.
	 * @param stopName The name of the shared stop.
	 * @param routes   The routes that share this one stop (as an array). This cannot be null.
	 */
	public SharedStop(LatLng latLng, String stopName, @NonNull Route[] routes) {
		super(stopName);

		// Set the location of the stop.
		this.location = latLng;

		// Set the routes that share this stop.
		this.routes = routes;

		// Using the routes set the size of the circle options and circles.
		this.circleOptions = new CircleOptions[routes.length];
		this.circles = new Circle[routes.length];

		// Populate the circle options.
		for (int i = 0; i < routes.length; i++) {

			// Set the center of each circle.
			this.circleOptions[i] = new CircleOptions().center(this.location);

			// If the route has a color, set circle color.
			Route route = routes[i];
			if (route.color != 0) {
				this.circleOptions[i].fillColor(route.color);
				this.circleOptions[i].strokeColor(route.color);
			}
		}

		// Set the initial circle size.
		this.setCircleSizes(SharedStop.INITIAL_CIRCLE_SIZE);
	}

	/**
	 * Gets the routes that share the provided stop by iterating through all routes,
	 * and comparing each route's stop to the provided stop to see if they match.
	 * <p>
	 * If there are no matches then the size of the returned route array will be 1.
	 * This is because its the only route to have that stop in its stop array.
	 *
	 * @param route      The route to compare against all other routes.
	 * @param routeIndex The index of the route that we are comparing in all routes.
	 * @param stop       The stop to compare against all stops in all other routes.
	 * @return Array of routes that share the stop. This always return an array of at least 1+.
	 */
	@NonNull
	public static Route[] getSharedRoutes(Route route, int routeIndex, Stop stop) {

		// Check if all routes is null.
		// If it is then simply return the single route provided as an array of 1.
		if (MapsActivity.allRoutes == null) {
			return new Route[]{route};
		}

		// Create an array of potential routes that could share a same stop
		// (the stop that we are iterating over).
		// Set the array size to that of all the routes minus the current index as to make it decrease every iteration.
		Route[] potentialRoutes = new Route[MapsActivity.allRoutes.length - routeIndex];

		// Add the current route to the potential routes, and update the potential route index.
		potentialRoutes[0] = route;
		int potentialRouteIndex = 1;

		// In order to iterate though all the routes remaining in the allRoutes array we need to get the 2nd route index.
		// This is equal to the first route index + 1 as to not hopefully not compare the same route against itself,
		// but also not compare against previous routes in the array.
		for (int route2Index = routeIndex + 1; route2Index < MapsActivity.allRoutes.length; route2Index++) {

			// Get the route at the 2nd index for comparison.
			Route route2 = MapsActivity.allRoutes[route2Index];

			// If the routes are the same then continue to the next iteration of the loop.
			if (route == route2) {
				continue;
			}

			// Iterate though each stop in the second route and compare them to the provided stop.
			for (Stop stop2 : route2.stops) {
				try {

					// If the stops match, add the route to the potential routes array.
					if (Stop.stopMatches(stop, stop2)) {
						potentialRoutes[potentialRouteIndex] = route2;
						potentialRouteIndex++;
					}
				} catch (NullPointerException e) {
					Log.e("mapSharedStops", "Stop may not have circle options set!", e);
				}
			}
		}

		// Create a new array of routes with the actual size of shared routes between the one shared stop.
		Route[] actualRoutes = new Route[potentialRouteIndex];

		// Copy the content from the potential routes into the actual route, and return the actual route.
		System.arraycopy(potentialRoutes, 0, actualRoutes, 0, potentialRouteIndex);
		return actualRoutes;
	}

	/**
	 * Compares stops against shared stops and only returns the stops that are not shared stops.
	 *
	 * @param stops       The original stops for the route that may be shared with shared stops.
	 * @param sharedStops The shared stops for the route.
	 * @return Returns an array of stops that are unique to the route (not shared by any other routes or shared stops).
	 */
	public static Stop[] removeStopsWithSharedStops(Stop[] stops, SharedStop[] sharedStops) {

		// Check if either the stops or shared stops array are null.
		// If they are just return the original stops array (which may be null - which is fun).
		if (stops == null || sharedStops == null) {
			Log.w("remvStpsWthShredStps", "Arguments are null!");
			return stops;
		}

		// Create an of potential stops with a maximum size of the original stop array.
		Stop[] potentialStops = new Stop[stops.length];
		int finalIndex = 0;

		// Iterate though each stop in the provided stop array.
		for (Stop stop : stops) {

			// Check if the stop matches the shared stop (same name, location).
			boolean noMatch = true;
			for (SharedStop sharedStop : sharedStops) {
				if (SharedStop.areEqual(sharedStop, stop)) {
					noMatch = false;
					break;
				}
			}

			// If the stop doesn't match add it to the potential stops array since its not shared.
			if (noMatch) {
				try {
					potentialStops[finalIndex] = stop;
					finalIndex++;
				} catch (ArrayIndexOutOfBoundsException e) {

					// If the array was out of bounds then log it (catastrophic if left unchecked).
					Log.e("remvStpsWthShredStps",
							String.format("Failed to add stop %s from route %s to array\n" +
											"Final stops array is too small!", stop.name,
									stop.route.routeName), e);
				}
			}
		}


		// If the final index does match the stop length minus shared stop length log how much it was off by.
		// This is left over from debugging, but is still useful to know.
		if (finalIndex != (stops.length - sharedStops.length)) {
			Log.i("remvStpsWthShredStps",
					String.format("Final index differs from standard number! (%d vs %d)",
							stops.length - sharedStops.length, finalIndex));
		}

		final Stop[] finalStops = new Stop[finalIndex];
		System.arraycopy(potentialStops, 0, finalStops, 0, finalIndex);
		return finalStops;
	}

	/**
	 * Compares the stop and shared stop name and location and returns whether or not both are the same.
	 *
	 * @param sharedStop The shared stop to compare.
	 * @param stop       The stop to compare.
	 * @return Whether or not they share the same name and location.
	 */
	public static boolean areEqual(@NonNull SharedStop sharedStop, @NonNull Stop stop) {

		// Compare names.
		boolean nameMatch = sharedStop.name.equals(stop.name),

				// Compare latitude.
				latitudeMatch = sharedStop.circleOptions[0].getCenter().latitude == stop.circleOptions.getCenter().latitude,

				// Compare longitude.
				longitudeMatch = sharedStop.circleOptions[0].getCenter().longitude == stop.circleOptions.getCenter().longitude;

		// Return whether all three checks match.
		return nameMatch && latitudeMatch && longitudeMatch;
	}

	/**
	 * Sets the shared stop circles to be visible.
	 * Circles will be created at this point if they were non-existent before (null).
	 * <p>
	 * This should be run on the UI thread.
	 *
	 * @param map The map to put create the circles on.
	 */
	@UiThread
	public void showSharedStop(GoogleMap map) {

		// Iterate though each of the circles.
		for (int i = 0; i < circles.length; i++) {
			Circle circle = this.circles[i];

			// If the circle is null, create a new shared stop circle.
			if (circle == null) {

				// Only set the newly created circles to clickable if its the 0th index circle (the biggest one).
				this.circles[i] = SharedStop.createSharedStopCircle(map, this.circleOptions[i],
						this, i == 0);
			} else {

				// Only set the circle to be clickable if its the 0th index circle (the biggest one).
				circle.setClickable(i == 0);

				// Set the circle to be visible.
				circle.setVisible(true);
			}
		}
	}

	/**
	 * Hides the shared stop.
	 * <p>
	 * If there are any routes that are still enabled that belong to this shared stop then the stop will not be hidden.
	 * <p>
	 * This must be run on the UI thread.
	 */
	@UiThread
	public void hideStop() {

		// Iterate though each route in the shared stop.
		for (Route route : this.routes) {

			// If any route is still enabled, return early.
			if (route.enabled) {
				return;
			}
		}

		// Iterate though each circle in the shared stop.
		for (Circle circle : this.circles) {

			// If the circle is not null set it to not be clickable, and hide it.
			if (circle != null) {
				circle.setClickable(false);
				circle.setVisible(false);
			}
		}
	}

	/**
	 * Sets the circles to the specified size.
	 * Each subsequent circle is set to a smaller size than the initial circle.
	 * <p>
	 * This should be run on the UI thread.
	 *
	 * @param size The size to set the circles to.
	 */
	@UiThread
	public void setCircleSizes(double size) {

		// Log the size that is being set for the initial circle (index 0).
		Log.d("setCircleSizes", "Setting initial size to: " + (size * (1.0d / (1))));

		// Iterate though each circle option (and circle if its not null) and reset its radius.
		for (int i = 0; i < this.circles.length; i++) {
			this.circleOptions[i].radius(size * (1.0d / (i + 1)));
			if (this.circles[i] != null) {
				this.circles[i].setRadius(size * (1.0d / (i + 1)));
			}
		}
	}

	/**
	 * Creates a new circle with the specified circle options that is immediately visible.
	 * <p>
	 * This should be run on the UI thread.
	 *
	 * @param map        The map to add the circle to.
	 * @param options    The specified circle options to apply to the circle.
	 * @param sharedStop The shared stop this circle belongs to. This will be set as the circle's tag.
	 * @param clickable  Whether or not the circle should be clickable.
	 * @return The newly created circle.
	 */
	@NonNull
	@UiThread
	private static Circle createSharedStopCircle(@NonNull GoogleMap map, CircleOptions options,
	                                             SharedStop sharedStop, boolean clickable) {

		// Get the circle that was added to the map with the provided circle options.
		Circle circle = map.addCircle(options);

		// Set the tag of the circle to the provided shared stop object.
		circle.setTag(sharedStop);

		// Set the circle to be clickable depending on the clickable argument.
		circle.setClickable(clickable);

		// At this point set the circle to be visible.
		circle.setVisible(true);

		// Return our newly created circle.
		return circle;
	}
}
