package fnsb.macstransit

import android.graphics.Color
import fnsb.macstransit.routematch.Route
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
		val routes: HashMap<String, Route> = HashMap(3)
		routes["Foo"] = Route("Foo")
		routes["Bar"] = Route("Bar")
		routes["Baz"] = Route("Baz")

		// TODO Fav
		val favoriteRoutes: Array<String> = arrayOf("Foo")

		// Enable the favoriteRoutes.
		Route.enableFavoriteRoutes(routes, favoriteRoutes)

		// Check for expected values.
		Assert.assertTrue(routes["Foo"]!!.enabled)
		Assert.assertFalse(routes["Bar"]!!.enabled)
		Assert.assertFalse(routes["Baz"]!!.enabled)
	}
}