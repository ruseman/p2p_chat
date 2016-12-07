package p2p.common;

import java.util.StringTokenizer;

/**
 * Container class for a remote configuration
 */
public final class Remote {

	/**
	 * The host address
	 */
	public final String		host;

	/**
	 * the hosts port
	 */
	public final Integer	port;

	/**
	 * A string of a Remote, the ip and the port, separated by a space
	 *
	 * @param string
	 */
	public Remote(String string) {
		StringTokenizer st = new StringTokenizer(string);
		host = st.nextToken();
		port = Integer.parseInt(st.nextToken());
	}

	/**
	 * Construct a host from a given host address and port
	 *
	 * @param host
	 * @param port
	 */
	public Remote(String host, Integer port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public String toString() {
		return host + " " + port;
	}
}
