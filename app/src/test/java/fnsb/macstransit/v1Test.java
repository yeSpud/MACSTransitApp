package fnsb.macstransit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collection;

import fnsb.macstransit.settings.V1;

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

	private final V1 settings = V1.INSTANCE;

	@Test
	public void testName() {
		assertEquals("settings.txt", this.settings.getFILENAME());
		assertNotEquals("settings.json", this.settings.getFILENAME());
	}

	@Test
	public void testRead() { // FIXME
		// First, verify the old settings file exists
		System.out.println(Helper.OLD_SETTINGS_TXT.getAbsolutePath());
		assertTrue(Helper.OLD_SETTINGS_TXT.exists());
		assertTrue(Helper.OLD_SETTINGS_TXT.canRead());

		// Now test the read function
		Collection<String> out = this.settings.readFromSettingsFile(Helper.OLD_SETTINGS_TXT);
		assertNotNull(out);
		/*
		assertArrayEquals(new String[]{"Enable Traffic View:true", "Enable Dark Theme:false",
				"Show Polylines:false", "Show VR Options:false"}, out.toArray());

		// Test a bad file
		assertNull(settings.readFromSettingsFile(new File("")));
		 */
	}

	@Test
	public void testParse() {
		// First, verify the old settings file exists
		System.out.println(Helper.OLD_SETTINGS_TXT.getAbsolutePath());
		assertTrue(Helper.OLD_SETTINGS_TXT.exists());
		assertTrue(Helper.OLD_SETTINGS_TXT.canRead());

		// Not test the parse
		settings.parseSettings(settings.readFromSettingsFile(Helper.OLD_SETTINGS_TXT));
		assertTrue(settings.getENABLE_TRAFFIC_VIEW());
		assertFalse(settings.getDEFAULT_NIGHT_MODE());
		assertFalse(settings.getSHOW_POLYLINES());
		assertFalse(settings.getENABLE_VR_OPTIONS());
	}
}
