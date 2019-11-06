package fnsb.macstransit;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.MalformedURLException;

import fnsb.macstransit.RouteMatch.RouteMatch;

/**
 * Created by Spud on 2019-11-04 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 *
 * @version 1.0
 * @since Beta 7
 */
public class Init extends AppCompatActivity {

	/**
	 * TODO Documentation
	 */
	private TextView console;

	/**
	 * TODO Documentation
	 */
	private ProgressBar progressBar;

	/**
	 * TODO Documentation
	 */
	private Button button;

	/**
	 * TODO Documentation
	 *
	 * @param savedInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.init);

		this.console = this.findViewById(R.id.textView);
		this.progressBar = this.findViewById(R.id.progressBar);
		this.button = this.findViewById(R.id.button);
		this.button.setVisibility(View.INVISIBLE);

		this.progressBar.setMax(100);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			this.progressBar.setMin(0);
		}
	}

	/**
	 * TODO Documentation
	 */
	@Override
	protected void onResume() {
		super.onResume();
		this.setProgress(0);

		// First, check if the user has internet
		if (this.hasInternet()) {
			// ...
		} else {
			// Since the user doesn't have internet, let them know, and add an option to open internet settings via clicking the button
			this.progressBar.setVisibility(View.INVISIBLE);
			this.console.setText(R.string.nointernet);
			this.button.setText(R.string.open_internet_settins);
			this.button.setOnClickListener((click) -> {
				this.startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS), 0);
				// Also, close this application
				this.finish();
			});
			this.button.setVisibility(View.VISIBLE);
		}

	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	private boolean hasInternet() {
		NetworkInfo activeNetwork = ((ConnectivityManager) this.getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}

	/**
	 * TODO Documentation
	 *
	 * @param progress
	 */
	private void setProgress(double progress) {
		this.runOnUiThread(() -> {
			int p = (int) Math.round(progress * 100);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				this.progressBar.setProgress(p, true);
			} else {
				this.progressBar.setProgress(p);
			}
		});
	}

}
