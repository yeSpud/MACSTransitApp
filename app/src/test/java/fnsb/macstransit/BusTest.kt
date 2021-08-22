package fnsb.macstransit

import fnsb.macstransit.routematch.Bus
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.activities.mapsactivity.MapsActivity
import fnsb.macstransit.routematch.Route.RouteException
import fnsb.macstransit.routematch.Route
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.lang.NullPointerException
import java.util.*
import kotlin.Throws

/**
 * Created by Spud on 6/27/20 for the project: MACS Transit.
 *
 *
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class BusTest {

	@Test
	fun busesTest() {

		// Test the individual buses.
		// First test bad arguments,
		val empty: Array<Bus> = emptyArray()
		Assert.assertArrayEquals(empty, Bus.getBuses(JSONArray()))

		// Now test valid buses.
		try {
			Assert.assertArrayEquals(empty, Bus.getBuses(RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_EMPTY_JSON))))
			val buses: Array<Bus> = Bus.getBuses(RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_JSON)))
			Assert.assertEquals(3, buses.size.toLong())

			// Test the individual buses.
			val ids: Array<String> = arrayOf("Bus 142", "Bus 131", "Bus 71")
			val lat: DoubleArray = doubleArrayOf(64.85543060302734, 64.81417083740234, 64.84135437011719)
			val lon: DoubleArray = doubleArrayOf(-147.7141876220703, -147.61318969726562, -147.71914672851562)
			for (i in buses.indices) {
				val bus: Bus = buses[i]
				Assert.assertEquals(ids[i], bus.name)
				Assert.assertSame(Objects.requireNonNull(MapsActivity.allRoutes)[i], bus.route)
				Assert.assertEquals(lat[i], bus.latitude, 0.0)
				Assert.assertEquals(lon[i], bus.longitude, 0.0)
			}
		} catch (e: JSONException) {
			e.printStackTrace()
			Assert.fail()
		} catch (e: NullPointerException) {
			e.printStackTrace()
			Assert.fail()
		}
	}

	@Test
	fun createNewBusTest() {

		// Test against bad arguments.
		Assert.assertThrows(JSONException::class.java) { Bus(JSONObject()) }

		// Test with valid arguments.
		val ids: Array<String> = arrayOf("Bus 142", "Bus 131", "Bus 71")
		val lat: DoubleArray = doubleArrayOf(64.85543060302734, 64.81417083740234, 64.84135437011719)
		val lon: DoubleArray = doubleArrayOf(-147.7141876220703, -147.61318969726562, -147.71914672851562)
		try {
			val busArray: JSONArray = RouteMatch.parseData(Helper.getJSON(Helper.ALL_VEHICLES_JSON))
			for (i in 0 until busArray.length()) {
				val bus = Bus(busArray.getJSONObject(i))
				Assert.assertEquals(ids[i], bus.name)
				Assert.assertSame(MapsActivity.allRoutes[i], bus.route)
				Assert.assertEquals(lat[i], bus.latitude, 0.0)
				Assert.assertEquals(lon[i], bus.longitude, 0.0)
			}
		} catch (e: JSONException) {
			e.printStackTrace()
			Assert.fail()
		} catch (e: RouteException) {
			e.printStackTrace()
			Assert.fail()
		} catch (e: NullPointerException) {
			e.printStackTrace()
			Assert.fail()
		}
	}

	@Test
	fun noBusMatchTest() {

		// Create various dummy test routes.
		val greenRoute = Route("Green")
		val blueRoute = Route("Blue")
		val otherGreen = Route("Green")

		// Create various dummy buses.
		val bus0 = Bus("0", greenRoute, 0.0, 0.0)
		val bus1 = Bus("1", greenRoute, 0.0, 0.0)
		val bus2 = Bus("2", greenRoute, 0.0, 0.0)
		val bus3 = Bus("3", greenRoute, 0.0, 0.0)
		val bus2Blue = Bus("2", blueRoute, 0.0, 0.0)
		val bus0OG = Bus("0", otherGreen, 0.0, 0.0)

		// Load most of the dummy buses into a test array.
		val testBusArray = arrayOf(bus0, bus1, bus2)

		// Test the noBusMatch method.
		Assert.assertFalse(bus0.isBusNotInArray(testBusArray))
		Assert.assertFalse(bus1.isBusNotInArray(testBusArray))
		Assert.assertFalse(bus2.isBusNotInArray(testBusArray))
		Assert.assertTrue(bus3.isBusNotInArray(testBusArray))
		Assert.assertTrue(bus2Blue.isBusNotInArray(testBusArray))
		Assert.assertFalse(bus0OG.isBusNotInArray(testBusArray))
	}

	@Test // Proof of concept for standard arrays vs array lists.
	fun ArrayTests() {

		val testData: JSONObject = try {
			Helper.getJSON(Helper.ALL_VEHICLES_JSON)
		} catch (e: JSONException) {
			e.printStackTrace()
			Assert.fail()
			return
		}

		val array: JSONArray = RouteMatch.parseData(testData)
		try {
			val b1 = ArrayListTest(array)
			val b2 = StandardArraysTest(array)
			Assert.assertEquals(b1.size.toLong(), b2.size.toLong())
		} catch (e: RouteException) {
			Assert.fail()
		}
	}

	companion object {

		@Throws(RouteException::class)
		private fun ArrayListTest(vehicles: JSONArray): Array<Bus> {
			val startTime = System.nanoTime()
			val buses = ArrayList<Bus>()
			for (i in 0 until vehicles.length()) {
				try {
					val bus = Bus(vehicles.getJSONObject(i))
					buses.add(bus)
				} catch (e: JSONException) {
					e.printStackTrace()
				}
			}
			val endTime = System.nanoTime()
			Helper.printTime(startTime, endTime)
			return buses.toTypedArray()
		}

		@Throws(RouteException::class)
		private fun StandardArraysTest(vehicles: JSONArray): Array<Bus> {
			val startTime = System.nanoTime()
			val potentialBuses: Array<Bus> = Array(vehicles.length()) {
				Bus(vehicles.getJSONObject(it))
			}
			val endTime = System.nanoTime()
			Helper.printTime(startTime, endTime)
			return potentialBuses
		}
	}

	init {
		MapsActivity.allRoutes = arrayOf(Route("Brown"), Route("Green"), Route("Red"))
	}
}