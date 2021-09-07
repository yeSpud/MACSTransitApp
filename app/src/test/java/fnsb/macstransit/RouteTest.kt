package fnsb.macstransit

import android.graphics.Color
import fnsb.macstransit.routematch.Route
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import org.junit.Assert
import org.junit.Test
import java.io.UnsupportedEncodingException

/**
 * Created by Spud on 7/16/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class RouteTest {

	@Test
	fun routeTest() {

		// Constructor test!
		// Basically make sure it errors when its supposed to.
		Assert.assertThrows(UnsupportedEncodingException::class.java) { Route("f o p") }
		Assert.assertThrows(UnsupportedEncodingException::class.java) { Route("multiline\nroute") }
		Assert.assertThrows(UnsupportedEncodingException::class.java) { Route("t a   b   s") }

		val fineRoute = Route("fine")
		val blue = Route("Blue", Color.BLUE)
		Assert.assertEquals("fine", fineRoute.name)
		Assert.assertEquals("Blue", blue.name)
		Assert.assertEquals(Color.BLUE.toLong(), blue.color.toLong())
		Assert.assertNotEquals(Color.GRAY.toLong(), blue.color.toLong())
	}

	/*
	@Test
	fun generateRoutesTest() {

		// Test bad arguments first.
		// TODO

		// TODO Test Master route.

	}
	*/

	@Test
	fun enableFavoriteRoutesTest() {

		// TODO All
		MapsActivity.allRoutes = arrayOf(Route("Foo"), Route("Bar"), Route("Baz"))

		// TODO Fav
		val favoriteRoutes: Array<Route> = arrayOf(Route("Foo"))

		// Enable the favoriteRoutes.
		Route.enableFavoriteRoutes(favoriteRoutes)

		// Check for expected values.
		Assert.assertTrue(MapsActivity.allRoutes[0].enabled)
		Assert.assertFalse(MapsActivity.allRoutes[1].enabled)
		Assert.assertFalse(MapsActivity.allRoutes[2].enabled)
	}
}