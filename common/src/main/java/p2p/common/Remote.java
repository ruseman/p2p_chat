package p2p.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Wrapper class for a remote system, using a simple socket, from a given host-port configuration
 */
public class Remote implements AutoCloseable {
	private Socket				socket	= null;
	
	/**
	 * Input from the remote
	 */
	public final BufferedReader	in;
	
	/**
	 * Output to the remote
	 */
	public final PrintStream	out;

	/**
	 * Connect to a given host, opening a socket on a given Host configuration
	 * @param info the host configuration
	 * @throws UnknownHostException if the host is not found
	 * @throws IOException if there's another IO error
	 */
	public Remote(Host info) throws UnknownHostException, IOException {
		this(new Socket(info.host, info.port));
		this.host = info;
	}

	/**
	 * Connect to a socket
	 * @param socket the socket
	 * @throws IOException if there's another IO error
	 */
	public Remote(Socket socket) throws IOException {
		this.socket = socket;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintStream(new DataOutputStream(socket.getOutputStream()));
	}
	
	public Remote(String hostname, int port) throws UnknownHostException, IOException{
		this(new Host(hostname, port));
	}

	@Override
	public void close() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ioe) {
			// suppress the exception if we're already closed
			if (!socket.isClosed())
				throw new RuntimeException(ioe);
		}
	}
	
	public String getAddress() {
		return socket.getInetAddress().getHostAddress();
	}

	public int getPort() {
		return socket.getPort();
	}

	@Override
	public String toString() {
		return "{" + getAddress() + ":" + getPort() + "}";
	}
	
	private Host host = null;
	
	public Host getHost(){
		if(host != null)
			return host;
		else
			return host = new Host(getAddress(), getPort());
	}
}