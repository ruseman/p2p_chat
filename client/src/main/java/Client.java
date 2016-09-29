import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

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

/*
 * connects to a given tracker, sends the port it's listening on, then waits for a message.  After it gets the message, it parses the message to a remote client and connects, and then the clients communicate directly
 */
public class Client implements AutoCloseable, Runnable {
	
	public final ThreadLogger logger = new ThreadLogger(System.in, System.out);
	
	private boolean running = true;
	public boolean isRunning(){
		return running;
	}
	
	protected synchronized void stop(){
		if(running)
			running = false;
		else
			throw new RuntimeException("Client already stopped");
	}
	protected final ServerSocket	server;
	
	public class ConnectThread extends Thread {
		protected final Tracker			tracker;

		public ConnectThread(String host, int port) {
			tracker = new Tracker(host, port);
		}

		@Override
		public void run() {
			// send port we're listening to to tracker
			try {
				logger.log("Starting to send the localport");
				tracker.out.writeBytes(server.getLocalPort() + "\n");
				logger.log("Sent localport");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
		
		public void close(){
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
			this.server.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected ConnectThread connectThread;
	
	@Override
	public void run() {
		// start the ConnectThread
		// TODO rewrite to use a config file
		connectThread = new ConnectThread("localhost", 8000);
		connectThread.start();
	}
}
