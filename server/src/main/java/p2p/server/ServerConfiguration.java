package p2p.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Configuration options for a server
 */
public class ServerConfiguration {

	/**
	 * A default server configuration
	 */
	public static final ServerConfiguration	DEFAULT	= new ServerConfiguration(8000);

	public int port;

	private ServerConfiguration(int port) {
		this.port = port;
	}

}
