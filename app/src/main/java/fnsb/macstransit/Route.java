package fnsb.macstransit;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Route {

	public String routeName;

	private int color;

	public Route(String routeName) {
		this.routeName = routeName;
	}

	public Route(String routeName, int color) {
		this.color = color;
		this.routeName = routeName;
	}

	public int getColor() {
		return this.color;
	}

	public void setColor(int color) {
		this.color = color;
	}

}
