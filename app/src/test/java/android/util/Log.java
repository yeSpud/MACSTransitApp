package android.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Spud on 6/22/20 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project.
 * <p>
 * This class is used to mock the Log function in the android SDK.
 * All logs will subsequently be parsed to System.out.println().
 *
 * @version 1.0
 * @since Release 1.2.
 */
public class Log {

	private static int output(@NotNull String level, String tag, String message) {
		System.out.println(String.format("%s: %s: %s", level.toUpperCase(), tag, message));
		return 0;
	}

	public static int v(String tag, String message) {
		return Log.output("verbose", tag, message);
	}

	public static int d(String tag, String message) {
		return Log.output("debug", tag, message);
	}

	public static int i(String tag, String message) {
		return Log.output("info", tag, message);
	}

	public static int w(String tag, String message) {
		return Log.output("warn", tag, message);
	}

	public static int e(String tag, String message) {
		return Log.output("error", tag, message);
	}

	public static int e(String tag, String message, @NotNull Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		int ret = Log.output("error", tag, message + '\n' + sw);
		pw.close();
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		return ret;
	}
}
