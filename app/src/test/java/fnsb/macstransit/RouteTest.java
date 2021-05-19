package fnsb.macstransit;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Route;
import fnsb.macstransit.RouteMatch.RouteMatch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by Spud on 7/16/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class RouteTest {

	/* TODO Revamp test
	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	@Test
	public void routeTest() {

		// Constructor test!
		// Basically make sure it errors when its supposed to.
		assertThrows(Route.RouteException.class, () -> new Route("f o p"));
		assertThrows(Route.RouteException.class, () -> new Route("multiline\nroute"));
		assertThrows(Route.RouteException.class, () -> new Route("t a   b   s"));

		try {
			Route fineRoute = new Route("fine"), blue = new Route("Blue", Color.BLUE);
			assertEquals("fine", fineRoute.routeName);
			assertEquals("Blue", blue.routeName);
			assertEquals(Color.BLUE, blue.color);
			assertNotEquals(Color.GRAY, blue.color);
		} catch (Route.RouteException | UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
		}
	}
	 */

	@Test
	public void generateRoutesTest() {

		// Test bad arguments first.
		assertArrayEquals(Route.EMPTY_ROUTE, Route.generateRoutes(null));
		assertArrayEquals(Route.EMPTY_ROUTE, Route.generateRoutes(new JSONArray()));

		// Now test valid arguments.
		try {
			Route[] routes = Route.generateRoutes(RouteMatch.parseData(Helper.getJSON(Helper.MASTERROUTE_JSON)));
			assertEquals(8, routes.length);

			String[] names = new String[]{"Yellow", "Grey", "Brown", "Blue", "Red", "Green", "Purple", "Orange"};
			int index = 0;
			for (Route route : routes) {
				assertEquals(names[index], route.routeName);
				index++;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			fail();
		}

	}

	@Test
	public void enableFavoriteRoutesTest() {

		// TODO All
		try {
			MapsActivity.allRoutes = new Route[]{new Route("Foo"),
					new Route("Bar"), new Route("Baz")};
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
			return;
		}

		// TODO Fav
		Route[] favoriteRoutes;
		try {
			favoriteRoutes = new Route[]{new Route("Foo")};
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail();
			return;
		}

		// Enable the favoriteRoutes.
		Route.enableFavoriteRoutes(favoriteRoutes);

		// Check for expected values.
		assertTrue(MapsActivity.allRoutes[0].enabled);
		assertFalse(MapsActivity.allRoutes[1].enabled);
		assertFalse(MapsActivity.allRoutes[2].enabled);

		try {
			Route.enableFavoriteRoutes(null);
			MapsActivity.allRoutes = null;
			Route.enableFavoriteRoutes(null);
		} catch (NullPointerException e) {
			e.printStackTrace();
			fail();
		}
	}

}
