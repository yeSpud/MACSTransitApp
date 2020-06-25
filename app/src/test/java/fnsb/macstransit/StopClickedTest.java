package fnsb.macstransit;

import org.junit.Test;

import fnsb.macstransit.Activities.ActivityListeners.StopClicked;

import static org.junit.Assert.assertEquals;

/**
 * Created by Spud on 6/25/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class StopClickedTest {

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
