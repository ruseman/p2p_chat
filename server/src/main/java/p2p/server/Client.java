package p2p.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

/*
 * A client, waiting to be paired
 */
public class Client implements AutoCloseable {
	private Socket				socket;
	public final PrintStream	out;
	
	public InetAddress getInetAddress(){
		return socket.getInetAddress();
	}

	public final BufferedReader	in;

	public Client(Socket socket) {
		this.socket = socket;
		try {
			out = new PrintStream(new DataOutputStream(socket.getOutputStream()));
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException ioe) {
			throw new RuntimeException("Failed to open ports from Client " + toString(), ioe);
		}
	}

	@Override
	public void close() {
		try {
			socket.close();
		} catch (IOException ioe) {
			throw new RuntimeException("Failed to close Client", ioe);
		}
	}

	@Override
	public String toString() {
		return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
	}
}
