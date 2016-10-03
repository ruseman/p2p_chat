package p2p.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Remote implements AutoCloseable {
	private Socket				socket	= null;
	public final BufferedReader	in;
	public final PrintStream	out;

	public Remote(ServerInfo info) throws UnknownHostException, IOException {
		this(new Socket(info.host, info.port));
	}

	public Remote(Socket socket) throws IOException {
		this.socket = socket;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintStream(new DataOutputStream(socket.getOutputStream()));
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
}