package fnsb.macstransit;

import org.junit.Test;

import fnsb.macstransit.Activities.ActivityListeners.StopClicked;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
	@Test
	public void addition_isCorrect() {
		assertEquals(4, 2 + 2);
	}

	@Test
	public void timeFormat() {
		assertEquals("12:00 pm", StopClicked.formatTime("12:00").toLowerCase());
		assertEquals("12:30 pm", StopClicked.formatTime("12:30").toLowerCase());
		assertEquals("11:30 am", StopClicked.formatTime("11:30").toLowerCase());
		assertEquals("1:00 pm", StopClicked.formatTime("13:00").toLowerCase());
		assertEquals("11:59 am", StopClicked.formatTime("11:59").toLowerCase());
		assertEquals("12:01 pm", StopClicked.formatTime("12:01").toLowerCase());
		assertEquals("12:59 am", StopClicked.formatTime("00:59").toLowerCase());
		assertEquals("12:00 am", StopClicked.formatTime("00:00").toLowerCase());
		assertEquals("11:59 pm", StopClicked.formatTime("23:59").toLowerCase());
	}
}