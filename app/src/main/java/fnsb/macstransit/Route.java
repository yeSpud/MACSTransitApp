package fnsb.macstransit;

/**
 * Created by Spud on 2019-10-12 for the project: MACS Transit.
 * <p>
 * For the license, view the file titled LICENSE at the root of the project
 */
public class Route {

	/**
	 * TODO Documentation
	 */
	public String routeName;

	/**
	 * TODO Documentation
	 */
	private int color;

	/**
	 * TODO Documentation
	 *
	 * @param routeName
	 */
	public Route(String routeName) {
		this.routeName = routeName;
	}

	/**
	 * TODO Documentation
	 *
	 * @param routeName
	 * @param color
	 */
	public Route(String routeName, int color) {
		this(routeName);
		this.color = color;
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public int getColor() {
		return this.color;
	}

	/**
	 * TODO Documentaiton
	 *
	 * @param color
	 */
	public void setColor(int color) {
		this.color = color;
	}

}
