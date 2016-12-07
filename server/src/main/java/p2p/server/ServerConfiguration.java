package p2p.server;

/**
 * Configuration options for a server
 */
public class ServerConfiguration {

	/**
	 * A default server configuration
	 */
	public static final ServerConfiguration	DEFAULT	= new ServerConfiguration(8000);

	public int								port;

	private ServerConfiguration(int port) {
		this.port = port;
	}

}
