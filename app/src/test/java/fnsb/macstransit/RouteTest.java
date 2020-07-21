package fnsb.macstransit;

import org.junit.Test;

import fnsb.macstransit.RouteMatch.Route;

import static org.junit.Assert.assertFalse;
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

	Route[] allRoutes, favoriteRoutes;

	public RouteTest() {
		// Initialize dummy allRoutes
		try {
			this.allRoutes = new Route[]{new Route("Foo"), new Route("Bar"), new Route("Baz")};
		} catch (Route.RouteException e) {
			e.printStackTrace();
			fail();
			return;
		}

		// Initialize dummy favorite route
		try {
			this.favoriteRoutes = new Route[]{new Route("Foo")};
		} catch (Route.RouteException e) {
			e.printStackTrace();
			fail();
		}
	}

	@SuppressWarnings("ResultOfObjectAllocationIgnored")
	@Test
	public void routeTest() {
		// Constructor test!
		// Basically make sure it errors when its supposed to.
		assertThrows(Route.RouteException.class, () -> new Route("f o p"));
		assertThrows(Route.RouteException.class, () -> new Route("multiline\nroute"));
		assertThrows(Route.RouteException.class, () -> new Route("t a   b   s"));
	}

	@Test
	public void enableFavoriteRoutesTest() {
		// Enable the favoriteRoutes
		Route.enableFavoriteRoutes(this.allRoutes, this.favoriteRoutes);

		// Check for expected values
		assertTrue(this.allRoutes[0].enabled);
		assertFalse(this.allRoutes[1].enabled);
		assertFalse(this.allRoutes[2].enabled);
	}

}
