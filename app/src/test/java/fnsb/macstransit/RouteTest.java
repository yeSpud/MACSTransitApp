package fnsb.macstransit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.graphics.Color;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import fnsb.macstransit.Activities.MapsActivity;
import fnsb.macstransit.RouteMatch.Route;

/**
 * Created by Spud on 7/16/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class RouteTest {

	@Test
	public void routeTest() {

		// Constructor test!
		// Basically make sure it errors when its supposed to. FIXME
		//assertThrows(UnsupportedEncodingException.class, () -> new Route("f o p"));
		//assertThrows(Route.RouteException.class, () -> new Route("multiline\nroute"));
		//assertThrows(Route.RouteException.class, () -> new Route("t a   b   s"));

		Route fineRoute = new Route("fine"), blue = new Route("Blue", Color.BLUE);
		assertEquals("fine", fineRoute.getRouteName());
		assertEquals("Blue", blue.getRouteName());
		assertEquals(Color.BLUE, blue.getColor());
		assertNotEquals(Color.GRAY, blue.getColor());
	}

	/*
	@Test
	public void generateRoutesTest() {

		// Test bad arguments first.
		// TODO

		// TODO Test Master route.

	}
	*/

	@Test
	public void enableFavoriteRoutesTest() {

		// TODO All
		MapsActivity.allRoutes = new Route[]{new Route("Foo"), new Route("Bar"),
				new Route("Baz")};

		// TODO Fav
		Route[] favoriteRoutes;
		favoriteRoutes = new Route[]{new Route("Foo")};

		// Enable the favoriteRoutes.
		Route.enableFavoriteRoutes(Arrays.asList(favoriteRoutes));

		// Check for expected values.
		assertTrue(MapsActivity.allRoutes[0].getEnabled());
		assertFalse(MapsActivity.allRoutes[1].getEnabled());
		assertFalse(MapsActivity.allRoutes[2].getEnabled());
		MapsActivity.allRoutes = null;

	}

}
