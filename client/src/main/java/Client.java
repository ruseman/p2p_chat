import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/*
 * connects to a given tracker, sends the port it's listening on, then waits for a message.  After it gets the message, it parses the message to a remote client and connects, and then the clients communicate directly
 */
public class Client implements AutoCloseable, Runnable {

	protected class ConnectThread extends Thread {
		protected final Tracker tracker;

		/*
		 * Create a ConnectThread froma given tracker host and port
		 */
		public ConnectThread(Tracker tracker) {
			this.tracker = tracker;
		}

		/*
		 * return a Remote from a given JSON string
		 */
		public Remote remoteFactory(String json) {
			return gson.fromJson(json, Remote.class);
		}

		@Override
		public void run() {
			// TODO
			// wait for mode from Tracker (listen or connect)
			// if listen then we just wait, calling server.accept()
			// if connect we read a second line, with the remotes info
			// then we use that socket to construct a remoteconnection
		}
	}

	/*
	 * Class for a remote client, that we want to connect to
	 */
	protected class Remote {
		protected class RemoteConnection {
			/*
			 * Our connection to them
			 */
			public final Socket			socket;
			/*
			 * Read from them
			 */
			public final BufferedReader	in;

			/*
			 * printed to them
			 */
			public final PrintStream	out;

			private RemoteConnection() {
				try {
					socket = new Socket(host, port);
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					out = new PrintStream(new DataOutputStream(socket.getOutputStream()));
				} catch (IOException e) {
					throw new RuntimeException("Error connecting to remote");
				}
			}
		}

		@Expose
		String	host;

		@Expose
		Integer	port;

		public RemoteConnection connect() {
			return new RemoteConnection();
		}

		@Override
		public String toString() {
			return gson.toJson(this, Remote.class);
		}
	}

	/*
	 * a connection to a remote client just a socket and a few helper methods
	 */
	protected class RemoteConnection {
		public final Socket			socket;
		public final PrintStream	out;

		public final BufferedReader	in;
		@Expose
		public String				host;

		@Expose
		public int					port;

		public RemoteConnection(Socket remoteSocket) {
			socket = remoteSocket;
			host = socket.getInetAddress().getHostAddress();
			port = socket.getPort();
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintStream(new DataOutputStream(socket.getOutputStream()));
			} catch (IOException ioe) {
				throw new RuntimeException("error openning streams", ioe);
			}
		}

		@Override
		public String toString() {
			return gson.toJson(this, RemoteConnection.class);
		}
	}

	protected class Tracker implements AutoCloseable {
		public final BufferedReader		in;

		public final DataOutputStream	out;
		private Socket					sock;
		@Expose
		public final int				port;

		@Expose
		public final String				host;

		public Tracker(String host, int port) {
			this.host = host;
			this.port = port;
			try {
				sock = new Socket(host, port);
				in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new DataOutputStream(sock.getOutputStream());
			} catch (IOException ioe) {
				throw new RuntimeException("Error connecting to tracker " + toString(), ioe);
			}
		}

		@Override
		public void close() {
			try {
				sock.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return gson.toJson(this, Tracker.class);
		}
	}

	public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	public static void main(String... args) {
		try (Client client = new Client()) {
			client.run();
		}
	}

	public final ThreadLogger		logger	= new ThreadLogger(System.in, System.out);

	private boolean					running	= true;

	protected final ServerSocket	server;

	protected ConnectThread			connectThread;

	protected Tracker				tracker;

	public Client() {
		try {
			// find open port
			server = new ServerSocket(0);
		} catch (IOException ioe) {
			throw new RuntimeException("Error opening port", ioe);
		}
	}

	@Override
	public void close() {
		try {
			server.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isRunning() {
		return running;
	}

	@Override
	public void run() {
		// start the ConnectThread
		// TODO rewrite to use a config file
		tracker = new Tracker("localhost", 8000);
		connectThread = new ConnectThread(tracker);
		connectThread.start();
	}

	protected synchronized void stop() {
		if (running) {
			running = false;
		} else
			throw new RuntimeException("Client already stopped");
	}
}

/*
 * Simple logger, with 3 levels, and it prints the thread that's invoking it
 */
class ThreadLogger {
	static enum MsgType {
		ERROR, NORMAL, WARN;
	}

	public final InputStream	in;

	public final PrintStream	out;

	public ThreadLogger(InputStream in, OutputStream out) {
		this(in, new PrintStream(out));
	}

	public ThreadLogger(InputStream in, PrintStream out) {
		this.in = in;
		this.out = out;
	}

	public void error(String msg) {
		println(MsgType.ERROR, msg);
	}

	public void log(String msg) {
		println(MsgType.NORMAL, msg);
	}

	public void println(MsgType type, String msg) {
		out.println("[" + Thread.currentThread().getName() + "] [" + type.name() + "] " + msg);
	}

	public void warn(String msg) {
		println(MsgType.WARN, msg);
	}
}
