package fnsb.macstransit.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import fnsb.macstransit.R;

/**
 * Created by Spud on 2019-11-24 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 8
 */
public class SettingsPopupWindow extends AlertDialog {

	/**
	 * TODO Documentation
	 */
	private static final String TRAFFIC_KEY = "Enable Traffic View",
			NIGHT_MODE_KEY = "Enable Dark Theme", POLYLINES_KEY = "Show Polylines",
			VR_KEY = "Show VR Options", FILENAME = "settings.txt";

	/**
	 * TODO Documentation
	 */
	public static boolean ENABLE_TRAFFIC_VIEW, DEFAULT_NIGHT_MODE, SHOW_POLYLINES, ENABLE_VR_OPTIONS;

	/**
	 * TODO Documentation
	 */
	private Context context;

	public SettingsPopupWindow(Context context) {
		super(context);
		this.context = context;
	}

	/**
	 * TODO Documentation
	 *
	 * @param context
	 */
	public static void loadSettings(Context context) {

		// First get the settings file
		File file = new File(context.getFilesDir(), SettingsPopupWindow.FILENAME);
		Log.d("loadSettings", "Supposed file location: " + file.getAbsolutePath());

		// Then be sure to check if the file exists
		Log.d("loadSettings", "Checking of settings file exists...");
		if (file.exists()) {
			// Get the file content
			Log.d("loadSettings", "Reading content of settings");
			String[] content = SettingsPopupWindow.readFile(context);

			if (content != null) {
				// Parse the settings into the static global variables above.
				for (String string : content) {
					String[] line = string.split(":");
					// Check what the first line is (to see if its an important key).
					switch (line[0]) {
						case SettingsPopupWindow.TRAFFIC_KEY:
							Log.d("loadSettings", "Updating traffic view setting");
							SettingsPopupWindow.ENABLE_TRAFFIC_VIEW = Boolean.parseBoolean(line[1]);
							break;
						case SettingsPopupWindow.NIGHT_MODE_KEY:
							Log.d("loadSettings", "Updating dark mode setting");
							SettingsPopupWindow.DEFAULT_NIGHT_MODE = Boolean.parseBoolean(line[1]);
							break;
						case SettingsPopupWindow.POLYLINES_KEY:
							Log.d("loadSettings", "Updating polyline setting");
							SettingsPopupWindow.SHOW_POLYLINES = Boolean.parseBoolean(line[1]);
							break;
						case SettingsPopupWindow.VR_KEY:
							Log.d("loadSettings", "Updating VR setting");
							SettingsPopupWindow.ENABLE_VR_OPTIONS = Boolean.parseBoolean(line[1]);
							break;
						default:
							Log.w("loadSettings", "Line unaccounted for!\n" + string);
							break;
					}
				}
			} else {
				// Since we were unable to load the content of the file, recreate it and rerun.
				Log.w("loadSettings", "Unable to parse content!");
				SettingsPopupWindow.createSettingsFile(context);
				SettingsPopupWindow.loadSettings(context);
			}
		} else {
			// Since the file doesn't exist, create a new one and return
			Log.w("loadSettings", "File does not exist! Creating new one...");
			SettingsPopupWindow.createSettingsFile(context);
			SettingsPopupWindow.loadSettings(context);
		}
	}

	/**
	 * TODO Documentation
	 */
	public static void writeSettings(Context context) {
		// TODO
	}

	/**
	 * TODO Documentation
	 *
	 * @param context
	 */
	private static void createSettingsFile(Context context) {
		Log.d("createSettingsFile", "Creating new settings file");
		// Create the string to write to the file for the first time.
		String outputString = SettingsPopupWindow.TRAFFIC_KEY + ":true\n"
				+ SettingsPopupWindow.NIGHT_MODE_KEY + ":false\n"
				+ SettingsPopupWindow.POLYLINES_KEY + ":false\n"
				+ SettingsPopupWindow.VR_KEY + ":false";

		// Write that string to the settings file
		// https://developer.android.com/training/data-storage/app-specific#internal-store-stream
		try (FileOutputStream fos = context.openFileOutput(SettingsPopupWindow.FILENAME, Context.MODE_PRIVATE)) {
			fos.write(outputString.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO Documentation
	 * <p>
	 * https://developer.android.com/training/data-storage/app-specific#internal-access-stream
	 *
	 * @param context
	 * @return
	 */
	private static String[] readFile(Context context) {
		FileInputStream fis = null;
		try {
			fis = context.openFileInput(SettingsPopupWindow.FILENAME);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (fis != null) {
			InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
			StringBuilder stringBuilder = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
				String line = reader.readLine();
				while (line != null) {
					stringBuilder.append(line).append("\n");
					line = reader.readLine();
				}
			} catch (IOException e) {
				// Error occurred when opening raw file for reading.
				e.printStackTrace();
			}
			String contents = stringBuilder.toString();
			return contents.split("\n");
		} else {
			return null;
		}

	}

	/**
	 * TODO Documentation
	 */
	public void showSettingsPopup() {
		ViewGroup viewGroup = this.findViewById(R.id.content);
		View dialogView = LayoutInflater.from(this.context).inflate(R.layout.settings_popup, viewGroup, false);

		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setView(dialogView);
		AlertDialog alertDialog = builder.create();

		// Setup the cancel button
		dialogView.findViewById(R.id.cancel).setOnClickListener((click) -> alertDialog.cancel());

		alertDialog.show();
	}

}
