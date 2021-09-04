package fnsb.macstransit

import fnsb.macstransit.activities.mapsactivity.StopClicked
import fnsb.macstransit.Helper.getJSON
import fnsb.macstransit.routematch.RouteMatch.Companion.parseData
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.util.*

/**
 * Created by Spud on 6/25/20 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 2.0.
 * @since Release 1.2.
 */
class StopClickedTest {

	@Test
	fun timeFormatTest() {
		Assert.assertEquals("12:00 pm",
		                    StopClicked.formatTime("12:00").lowercase(Locale.getDefault()))
		Assert.assertEquals("12:30 pm",
		                    StopClicked.formatTime("12:30").lowercase(Locale.getDefault()))
		Assert.assertEquals("11:30 am",
		                    StopClicked.formatTime("11:30").lowercase(Locale.getDefault()))
		Assert.assertEquals("1:00 pm",
		                    StopClicked.formatTime("13:00").lowercase(Locale.getDefault()))
		Assert.assertEquals("11:59 am",
		                    StopClicked.formatTime("11:59").lowercase(Locale.getDefault()))
		Assert.assertEquals("12:01 pm",
		                    StopClicked.formatTime("12:01").lowercase(Locale.getDefault()))
		Assert.assertEquals("12:59 am",
		                    StopClicked.formatTime("00:59").lowercase(Locale.getDefault()))
		Assert.assertEquals("12:00 am",
		                    StopClicked.formatTime("00:00").lowercase(Locale.getDefault()))
		Assert.assertEquals("11:59 pm",
		                    StopClicked.formatTime("23:59").lowercase(Locale.getDefault()))
		Assert.assertEquals("", StopClicked.formatTime(""))
	}

	@Test
	fun newLineOccurrenceTest() {
		Assert.assertEquals(2, StopClicked.getNewlineOccurrence("Foo\nBar\nBaz"))
		Assert.assertEquals(2, StopClicked.getNewlineOccurrence("Foo\n\nBar"))
		Assert.assertEquals(0, StopClicked.getNewlineOccurrence("Foo"))
		Assert.assertEquals(0, StopClicked.getNewlineOccurrence("String with spaces."))
		Assert.assertEquals(1, StopClicked.getNewlineOccurrence("\n"))
	}


	@Test
	fun timeTest() {

			// Test the raw function with bad parameters.
			Assert.assertEquals("", StopClicked.getTime(JSONObject(), ""))

			try {

				// Load test json array from files.
				val geist = parseData(getJSON(Helper.GEIST_MCDS))
				val woodCenter = parseData(getJSON(Helper.WOOD_CENTER))
				val transitCenter = parseData(getJSON(Helper.TRANSIT_CENTER))

				// Iterate though each of the arrays individually.
				// Start with geist.
				val geistArrivalTime = arrayOf("17:51", "18:20", "19:04")
				val geistDepartureTime = arrayOf("17:51", "18:20", "19:04")
				for (g in 0 until geist.length()) {
					val stopObject = geist.getJSONObject(g)
					bulkGetTimeTest(stopObject, geistArrivalTime[g], geistDepartureTime[g])
				}

				// Move onto wood center.
				val woodArrivalTime =
						arrayOf("17:47", "18:10", "18:15", "18:22", "18:32", "18:45", "18:50",
						        "19:00", "19:30")
				val woodDepartureTime =
						arrayOf("17:47", "18:15", "18:15", "18:22", "18:32", "18:45", "18:50",
						        "19:00", "19:30")
				for (w in 0 until woodCenter.length()) {
					val stopObject = woodCenter.getJSONObject(w)
					bulkGetTimeTest(stopObject, woodArrivalTime[w], woodDepartureTime[w])
				}

				// Finish with transit center.
				val transitArrivalTime =
						arrayOf("17:08", "17:39", "17:44", "17:56", "18:02", "18:14", "18:22",
						        "18:52", "18:54", "19:09", "19:22", "02:17", "02:40", "03:05")
				val transitDepartureTime =
						arrayOf("18:00", "17:45", "17:45", "18:00", "18:02", "18:20", "18:30",
						        "20:00", "18:54", "19:10", "19:30", "02:17", "02:40", "03:05")
				for (t in 0 until transitCenter.length()) {
					val stopObject = transitCenter.getJSONObject(t)
					bulkGetTimeTest(stopObject, transitArrivalTime[t], transitDepartureTime[t])
				}
			} catch (e: org.json.JSONException) {
				e.printStackTrace()
				Assert.fail()
			}
		}

	companion object {
		fun bulkGetTimeTest(stopObject: JSONObject, arrivalTime: String, departureTime: String) {

			// Test valid keys against the object.
			// Start with those that should not equal the arrival or departure times.
			val failKeys =
					arrayOf("arriveComplete", "cancelled", "dailyTimetableSameAsArrivedTimeTable",
					        "departComplete", "departTime", "destination", "distanceOffRoute",
					        "etaEnabled", "firstStop", "lastETACalcDateTime",
					        "lastTimePointCrossedDate", "lastTimePointCrossedId",
					        "masterRouteLongName", "masterRouteShortName", "predictedReal",
					        "routeId", "routeLongName", "routeShortName", "routeStatus",
					        "scheduleAdherence", "scheduleAdherenceEnabled", "status", "stopId",
					        "stopTypeDefinitionBitset", "subRouteLongName", "subRouteShortName",
					        "templates", "timePoint", "tripDirection", "tripId", "vehicleId")
			for (failKey in failKeys) {
				Assert.assertNotEquals(arrivalTime, StopClicked.getTime(stopObject, failKey))
				Assert.assertNotEquals(departureTime, StopClicked.getTime(stopObject, failKey))
			}

			// Test keys that either should contain the actual arrival and departure time,
			// or has the possibility of containing the actual arrival / departure time.
			Assert.assertEquals(arrivalTime, StopClicked.getTime(stopObject, "predictedArrivalTime"))
			Assert.assertEquals(departureTime, StopClicked.getTime(stopObject, "predictedDepartureTime"))

			// FIXME Sometimes these match the predicted times, sometimes they don't.
			//Assert.assertEquals("", StopClicked.getTime(stopObject, "arriveTime"))
			//Assert.assertEquals(arrivalTime, StopClicked.getTime(stopObject, "realETA"))
			//Assert.assertEquals(arrivalTime, StopClicked.getTime(stopObject, "scheduledArrivalTime"))
			//Assert.assertEquals(departureTime, StopClicked.getTime(stopObject, "scheduledDepartureTime"))

			// Test invalid keys against the object.
			Assert.assertEquals("", StopClicked.getTime(stopObject, "fadskjhbgfdsajhk"))
			Assert.assertEquals("", StopClicked.getTime(stopObject, ""))
		}
	}
}