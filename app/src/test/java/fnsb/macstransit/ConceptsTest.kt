package fnsb.macstransit

import org.junit.Assert
import org.junit.Test
import java.util.ArrayList

/**
 * Created by Spud on 9/1/21 for the project: MACS Transit.
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0.
 * @since Release 1.3.1.
 */
class ConceptsTest {

	@Test // Proof of concept for standard arrays vs array lists.
	fun ArrayTests() {

		val inputArray: Array<String> = Array(3) {
			when (it) {
				0 -> "Foo"
				1 -> "Bar"
				2 -> "Baz"
				else -> ""
			}
		}
		Assert.assertEquals("Foo", inputArray[0])
		Assert.assertEquals("Bar", inputArray[1])
		Assert.assertEquals("Baz", inputArray[2])

		val arrayList = ArrayListTest(inputArray)
		val array = StandardArraysTest(inputArray)
		Assert.assertEquals(arrayList.size, array.size)
		Assert.assertEquals(inputArray.size, arrayList.size)
		Assert.assertEquals(inputArray.size, array.size)
		Assert.assertArrayEquals(arrayList, array)
	}

	@Test // Proof of concept that zero size arrays are still valid to use for iteration (wont out of bounds)
	fun ZeroArrayTest() {

		val nullArray: Array<String?> = arrayOfNulls(10)
		val emptyArray: Array<String> = emptyArray()
		val emptyNullArray: Array<String?> = emptyArray()

		for (item in nullArray) {
			// This should run, but should not error
			Assert.assertNull(item)
		}

		for (item in emptyArray) {
			// This should not run, nor should this error...
			Assert.assertNotNull(item)
		}

		for (item in emptyNullArray.indices) {
			// This should not run, nor should this error
			Assert.assertNotNull(item)
		}
	}

	@Test
	fun forLoopsTest() {

		val array: Array<Boolean> = Array(10) {
			false // For now set the default value to false.
		}

		// Test how long it takes to switch all values with a standard for loop.
		var startTime = System.nanoTime()
		for (i in array.indices) {
			array[i] = true
			Assert.assertTrue(array[i])
		}
		var endTime = System.nanoTime()
		println("For loop set time")
		Helper.printTime(startTime, endTime)

		// TODO For entry loop.

		// Test how long it takes to print all values with a standard for loop.
		startTime = System.nanoTime()
		for (i in array.indices) {
			print(array[i])
			Assert.assertTrue(array[i])
		}
		endTime = System.nanoTime()
		println("\nFor loop print time")
		Helper.printTime(startTime, endTime)

		// TODO For entry loop.

		// Test how long it takes to switch all values with a for each loop.
		startTime = System.nanoTime()
		array.forEach {
			print(it)
			Assert.assertTrue(it)
		}
		endTime = System.nanoTime()
		println("\nFor each loop print time")
		Helper.printTime(startTime, endTime)
	}

	@Test
	fun nullArrayNonNullTest() {

		var startTime = System.nanoTime()
		val nullArray = nullArrayNonNull(arrayOfNulls(100))
		var endTime = System.nanoTime()
		print("Require Non Nulls time: ")
		val nnTime = Helper.printTime(startTime, endTime)

		startTime = System.nanoTime()
		val uncheckedCast = nullArrayUncheckedCast(arrayOfNulls(100))
		endTime = System.nanoTime()
		print("Unchecked Cast time: ")
		val ucTime = Helper.printTime(startTime, endTime)

		Assert.assertEquals(100, nullArray.size)
		Assert.assertEquals(100, uncheckedCast.size)
		Assert.assertArrayEquals(nullArray, uncheckedCast)
		Assert.assertTrue(nnTime > ucTime)

	}

	@Test
	fun hashMapSepuku() {
		val hashMap: HashMap<String, Int> = HashMap(3)
		hashMap["Foo"] = 0
		hashMap["Bar"] = 1
		hashMap["Baz"] = 2


		// Try removing a entry from the hashmap while iterating though it.
		Assert.assertThrows(ConcurrentModificationException::class.java) {
			hashMap.forEach {
				if (it.value == 1 ) {
					hashMap.remove(it.key)
				}
			}
		}
	}

	companion object {

		private fun ArrayListTest(inputArray: Array<String>): Array<Int> {
			val startTime = System.nanoTime()
			val arrayList = ArrayList<Int>()

			for (entry in inputArray) {
				arrayList.add(entry.hashCode())
			}

			val endTime = System.nanoTime()
			println("ArrayListTime")
			Helper.printTime(startTime, endTime)
			return arrayList.toTypedArray()
		}

		private fun StandardArraysTest(inputArray: Array<String>): Array<Int> {
			val startTime = System.nanoTime()
			val returnArray: Array<Int> = Array(inputArray.size) {
				inputArray[it].hashCode()
			}
			val endTime = System.nanoTime()
			println("ArrayTime")
			Helper.printTime(startTime, endTime)
			return returnArray
		}

		private fun nullArrayNonNull(nullArray: Array<Int?>): Array<Int> {
			for (i in nullArray.indices) {
				nullArray[i] = i
			}
			return nullArray.requireNoNulls()
		}

		private fun nullArrayUncheckedCast(nullArray: Array<Int?>): Array<Int> {
			for (i in nullArray.indices) {
				nullArray[i] = i
			}
			@Suppress("Unchecked_Cast")
			return nullArray as Array<Int>
		}
	}

}