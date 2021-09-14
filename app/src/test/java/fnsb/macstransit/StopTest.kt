package fnsb.macstransit

import fnsb.macstransit.routematch.Route
import fnsb.macstransit.routematch.RouteMatch
import fnsb.macstransit.routematch.SharedStop
import fnsb.macstransit.routematch.Stop
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.util.*

/**
 * Created by Spud on 10/26/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class StopTest {

	@Test
	fun StopCreationTest() {

		val blueStopJson: JSONObject = try {
			 Helper.getJSON(Helper.BLUE_STOPS)
		} catch (e: JSONException) {
			e.printStackTrace()
			Assert.fail()
			return
		}

		val blueStopJsonArray: JSONArray = RouteMatch.parseData(blueStopJson)
		val blueRoute = Route("Blue")
		var startTime: Long = System.nanoTime()
		var blueStops: Array<Stop> = Array(blueStopJsonArray.length()) {
			Stop(blueStopJsonArray.getJSONObject(it), blueRoute)
		}
		var endTime: Long = System.nanoTime()
		Helper.printTime(startTime, endTime)

		// Before duplication checking this should be 232 in length
		Assert.assertNotNull(blueStops)
		Assert.assertEquals(233, blueStops.size.toLong())
		startTime = System.nanoTime()
		blueStops = Stop.validateGeneratedStops(blueStops)
		endTime = System.nanoTime()
		Helper.printTime(startTime, endTime)

		// After duplication checking
		Assert.assertNotNull(blueStops)
		Assert.assertEquals(66, blueStops.size.toLong())
	}

	/*
	@Test
	fun isDuplicateCheck() {
			// TODO
	}
	 */

	@Test
	fun averageStops() {
		val files = arrayOf(Helper.BLUE_STOPS, Helper.BROWN_STOPS, Helper.GOLD_STOPS, Helper.GREEN_STOPS,
		                    Helper.PURPLE_STOPS, Helper.RED_STOPS, Helper.YELLOW_STOPS)
		var count = 0
		for (file in files) {
			try {
				val jsonObject: JSONObject = Helper.getJSON(file)
				val jsonArray: JSONArray = RouteMatch.parseData(jsonObject)
				val pStops: Array<Stop> = arrayOf(Stop(jsonArray.getJSONObject(0), Route("Foo")))
				val vStops: Array<Stop> = Stop.validateGeneratedStops(pStops)
				count += vStops.size
			} catch (e: Exception) {
				e.printStackTrace()
				Assert.fail()
			}
		}
		println(count / files.size)
	}

	@Test
	/*
	 * This test should test the loading of multiple stops, apply a duplication check, apply shared stops,
	 * and finally remove overlapping shared stops and regular stops.
	 */
	fun stopGauntlet() {
		try {
			// Start by getting the json files to use for testing.
			// TODO Add Gray, Orange lines.
			val loadedFiles = 6
			val files = arrayOf(Helper.BLUE_STOPS, Helper.BROWN_STOPS, Helper.GREEN_STOPS, Helper.PURPLE_STOPS,
			                    Helper.RED_STOPS, Helper.YELLOW_STOPS)
			val routes: HashMap<String, Route> = HashMap(loadedFiles)
			routes["Blue"] = Route("Blue")
			routes["Brown"] = Route("Brown")
			routes["Green"] = Route("Green")
			routes["Purple"] = Route("Purple")
			routes["Red"] = Route("Red")
			routes["Yellow"] = Route("Yellow")

			// Check the stops that will have duplicates.
			val stopsWithDuplicates = ArrayList<Array<Stop>>(loadedFiles)

			// Start by loading the stops from each file.
			for (stopJsonFile in files) {
				val jsonObject: JSONObject = Helper.getJSON(stopJsonFile)
				val dataArray: JSONArray = RouteMatch.parseData(jsonObject)

				val routeName = when(stopJsonFile) {

					Helper.BLUE_STOPS -> "Blue"
					Helper.BROWN_STOPS -> "Brown"
					Helper.GREEN_STOPS -> "Green"
					Helper.PURPLE_STOPS -> "Purple"
					Helper.RED_STOPS -> "Red"
					Helper.YELLOW_STOPS -> "Yellow"
					else -> "Gold"

				}

				val stops: Array<Stop> = Array(dataArray.length()) {
					Stop(dataArray.getJSONObject(it), routes[routeName]!!)
				}
				stopsWithDuplicates.add(stops)
			}

			// Now iterate though each stop that has duplicates and verify the number of stops.
			// This number should be large as we haven not removed the duplicate stops at this point.
			val validDuplicateStopCounts = intArrayOf(233, 24, 144, 78, 176, 145)
			for (i in 0 until loadedFiles) {
				val stops: Array<Stop> = stopsWithDuplicates[i]
				println("Number of stops for ${stops[0].routeName} (with potential duplicates): ${stops.size}\n")
				Assert.assertEquals(validDuplicateStopCounts[i], stops.size)
			}

			// Now test the removal of duplicate stops.
			val validateStopCounts = intArrayOf(66, 24, 104, 39, 58, 56)
			for (i in 0 until loadedFiles) {
				val stops: Array<Stop> = stopsWithDuplicates[i]
				val vStops: Array<Stop> = Stop.validateGeneratedStops(stops)
				println("Number of stops for ${vStops[0].routeName}: ${vStops.size}\n")
				Assert.assertEquals(validateStopCounts[i], vStops.size)
				vStops.forEach { routes[it.routeName]!!.stops[it.name] = it }
			}

			// Now test the creation of shared stops.
			for (stopJsonFile in files) {

				val routeName = when(stopJsonFile) {

					Helper.BLUE_STOPS -> "Blue"
					Helper.BROWN_STOPS -> "Brown"
					Helper.GREEN_STOPS -> "Green"
					Helper.PURPLE_STOPS -> "Purple"
					Helper.RED_STOPS -> "Red"
					Helper.YELLOW_STOPS -> "Yellow"
					else -> "Gold"

				}

				// Get a first comparison route.
				val route: Route = routes[routeName]!!

				// Iterate through all the stops in our first comparison route.
				for ((name, stop) in route.stops) {

					// Make sure our stop is not already in our shared stop.
					val sharedStops = route.sharedStops
					if (sharedStops.isNotEmpty()) {
						var found = false

						// Iterate though the shared stops in the route.
						for (ssCheck in sharedStops) {

							// If the route was found, continue.
							if (ssCheck.equals(stop)) {
								found = true
								break
							}
						}
						if (found) {
							continue
						}
					}

					// Get an array of shared routes.
					val sharedRoutes = SharedStop.getSharedRoutes(route, stop, routes)

					// If the shared routes array has more than one entry, create a new shared stop object.
					if (sharedRoutes.size > 1) {
						val sharedStop = SharedStop(name, stop.location, sharedRoutes)

						// Iterate though all the routes in the shared route, and add our newly created shared stop.
						sharedRoutes.forEach { it.sharedStops[name] = sharedStop }
					}
				}
			}

			// Test the number of shared stops.
			val sharedStopsCount = intArrayOf(14, 3, 10, 10, 12, 17)
			for ((i, stopJsonFile) in files.withIndex()) {

				val routeName = when(stopJsonFile) {

					Helper.BLUE_STOPS -> "Blue"
					Helper.BROWN_STOPS -> "Brown"
					Helper.GREEN_STOPS -> "Green"
					Helper.PURPLE_STOPS -> "Purple"
					Helper.RED_STOPS -> "Red"
					Helper.YELLOW_STOPS -> "Yellow"
					else -> "Gold"

				}

				val route: Route = routes[routeName]!!
				println("${route.name} route stops: ${route.stops.size}\n")
				println("${route.name} route shared stops: ${route.sharedStops.size}\n")
				Assert.assertEquals(sharedStopsCount[i], route.sharedStops.size)
			}

			// Test removal of stops that have shared stops.
			val finalStopsSize: Array<Int?> = arrayOfNulls(loadedFiles)
			for (i in 0 until routes.size) {

				val routeName = when(i) {

					0 -> "Blue"
					1 -> "Brown"
					2 -> "Green"
					3 -> "Purple"
					4 -> "Red"
					5 -> "Yellow"
					else -> "Gold"

				}
				val route: Route = routes[routeName]!!

				val initialStopSize = route.stops.size
				route.purgeStops()
				println("Went from $initialStopSize stops to ${route.stops.size} stops for route ${route.name}\n")
				finalStopsSize[i] = route.stops.size
			}
			val finalStopCount = intArrayOf(66 - 14, 24 - 3, 104 - 10, 39 - 10, 58 - 12, 56 - 17)
			for (i in 0 until loadedFiles) {
				val stopSize: Int = finalStopsSize[i]!!
				Assert.assertEquals(finalStopCount[i], stopSize)
			}
		} catch (e: JSONException) {

			// If anything goes wrong, print and then fail.
			e.printStackTrace()
			Assert.fail()
		} catch (e: RuntimeException) {
			e.printStackTrace()
			Assert.fail()
		}
	}
}