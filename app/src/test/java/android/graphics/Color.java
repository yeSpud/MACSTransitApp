package android.graphics;

/**
 * Created by Spud on 3/12/21 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 * <p>
 * This class is used to mock the Color function in the android SDK.
 *
 * @version 1.0.
 * @since Release 1.2.
 */
public class Color {

	public static final int BLUE = 0xFF0000FF;
	public static final int GRAY = 0xFF888888;

	public static int parseColor(String color) {
		System.out.println(String.format("Requested color: %s (returning 0 instead)", color));
		return 0;
	}
}
