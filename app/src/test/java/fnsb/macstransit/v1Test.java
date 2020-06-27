package fnsb.macstransit;

import org.junit.Test;

import java.io.File;

import fnsb.macstransit.Settings.v1;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Spud on 6/19/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 *
 * @version 1.0
 * @since Release 1.2.
 */
@SuppressWarnings("deprecation")
public class v1Test {

	private v1 settings = new v1();

	@Test
	public void testName() {
		assertEquals("settings.txt", v1.FILENAME);
		assertNotEquals("settings.json", v1.FILENAME);
	}

	@Test
	public void testRead() {
		// First, verify the old settings file exists
		System.out.println(this.oldFile.getAbsolutePath());
		assertTrue(this.oldFile.exists());
		assertTrue(this.oldFile.canRead());

		// Now test the read function
		String[] out = settings.readFromSettingsFile(this.oldFile);
		assertNotNull(out);
		assertArrayEquals(new String[]{"Enable Traffic View:true", "Enable Dark Theme:false",
				"Show Polylines:false", "Show VR Options:false"}, out);

		// Test a bad file
		assertNull(settings.readFromSettingsFile(new File("")));
	}

	@Test
	public void testParse() {
		// First, verify the old settings file exists
		System.out.println(this.oldFile.getAbsolutePath());
		assertTrue(this.oldFile.exists());
		assertTrue(this.oldFile.canRead());

		// Not test the parse
		settings.parseSettings(settings.readFromSettingsFile(this.oldFile));
		assertTrue(v1.ENABLE_TRAFFIC_VIEW);
		assertFalse(v1.DEFAULT_NIGHT_MODE);
		assertFalse(v1.SHOW_POLYLINES);
		assertFalse(v1.ENABLE_VR_OPTIONS);
	}
}
