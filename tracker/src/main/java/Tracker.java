
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/*
 * Tracker for p2p chat system. It listens on a given port (8000 by default) for
 * connections from Clients. Clients then send a JSON message with the port
 * they're listening on. The Tracker pairs clients as they join, sending them
 * the IP and port of their mate, and the Clients connect as is appropriate.
 */
public class Tracker implements AutoCloseable {
	/*
	 * Configuration for a tracker, typically loaded from a .json file
	 */
	public static class Config {
		public static Config DEFAULT = new Config();
		static {
			DEFAULT.port = 8000;
		}

		/*
		 * The port to listen for
		 */
		@Expose
		public int port;
	}

	protected class ListenThread extends Thread {
		public ListenThread() {
			setName("ListenThread");
		}

		@Override
		public void run() {
			Socket clientSocket;
			logger.log("Starting to listen...");
			while (isRunning()) {
				try {
					clientSocket = socket.accept();
					logger.log("Got connection!");
				} catch (SocketException se) {
					if (se.getMessage().equals("Socket closed")) {
						if (isRunning())
							throw new RuntimeException(se);
						else {
							break;
						}
					} else {
						continue;
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
					continue;
				}
				Thread thread = new ClientConnectionThread(new ClientConnection(clientSocket));
				thread.start();
			}
		}
	}
	protected class MatchThread extends Thread {
		int pairs = 0;

		public MatchThread() {
			setName("MatchThread");
		}

		public Thread pairThread(ClientConnection conn1, ClientConnection conn2) {
			
			return new Thread(() -> {
				logger.log("Starting to pair...");
				// TODO send the data for the two clients to each other
				// NOTE this single thread sends conn1 to conn2 and visa versa
			}, "PairThread#" + (pairs++));
		}

		@Override
		public void run() {
			logger.log("Starting to match...");
			ClientConnection cc1, cc2;
			while (isRunning()) {
				if (clients.size() >= 2) {
					cc1 = clients.pop();
					cc2 = clients.pop();
					pairThread(cc1, cc2).start();
				}
			}
		}
	}

	/*
	 * Class for a Task that can be run from the REPL. These are help in a
	 * Map<String, Task>. It has the runnable (i.e. the actual function) and the
	 * docstring, which if used provides a brief description of the command
	 */
	protected class Task {
		public final String				docstring;
		private final Supplier<String>	supplier;

		public Task(String doc, Supplier<String> fn) {
			if (doc == null) {
				doc = "No doc";
			}
			docstring = doc;
			supplier = fn;
		}

		public Task(Supplier<String> fn) {
			this(null, fn);
		}

		public Supplier<String> call() {
			return supplier;
		}
	}

	/*
	 * Holds the state of the connected client. Each ClientConnection is run as
	 * a thread, where it listens for information from the client (the listening
	 * port) and then adds itself to the Tracker's stack of awaiting clients
	 */
	class ClientConnection implements Runnable {

		/*
		 * The socket the client is talking to us through
		 */
		protected final Socket				socket;

		/*
		 * in for the socket
		 */
		protected final BufferedReader		in;

		/*
		 * out for the socket
		 */
		protected final DataOutputStream	out;

		/*
		 * The port the client is listening on
		 */
		@Expose
		private Integer						port		= null;

		/*
		 * the hostname
		 */
		@Expose
		private String						hostname	= null;

		/*
		 * instantiate new ClientConnection, from a given socket
		 */
		public ClientConnection(Socket socket) {
			this.socket = socket;
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new DataOutputStream(socket.getOutputStream());
				hostname = socket.getInetAddress().getHostAddress();
			} catch (IOException ioe) {
				throw new RuntimeException("Failed to open client connection", ioe);
			}
		}

		/*
		 * return the host the client is on
		 */
		public String hostname() {
			if (hostname == null)
				throw new NullPointerException();
			return hostname;
		}

		/*
		 * return the port the Client is listening on
		 */
		public int port() {
			if (port == null)
				throw new NullPointerException();
			return port;
		}

		/*
		 * called by ClientConnection threads, should wait for message from
		 * client, then set the info, then add itself to the stack of waiting
		 * clients
		 */
		@Override
		public void run() {
			int port;
			String msg;
			try {
				// It's listening for a single string, an int, the port that the
				// client is listening on
				msg = in.readLine();
				port = Integer.parseInt(msg);
			} catch (IOException ioe) {
				throw new RuntimeException("Error reading client message", ioe);
			} catch (RuntimeException re) {
				throw new RuntimeException("Error parsing message from client", re);
			}
			this.port = port;
			Tracker.this.clients.push(this);
		}

		@Override
		public String toString() {
			return gson.toJson(this, ClientConnection.class);
		}
	}

	/*
	 * path to config file
	 */
	public static final String	CONFIG_PATH	= "tracker.json";

	/*
	 * GSON instance, used throughout the entire program GsonBuilder used, so in
	 * order for Gson to use a field, it needs the @Expose annotation
	 */
	protected static Gson		gson		= new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	/*
	 * main bruh
	 */
	public static void main(String... args) {
		try (Tracker tracker = new Tracker(getConfig(Paths.get(CONFIG_PATH)))) {
			tracker.run();
		} catch (RuntimeException re) {
			re.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 * Load Config from file, writing default to file if it does not exist
	 */
	protected static Config getConfig(Path configPath) {
		Config config = null;
		try {
			if (!configPath.toFile().exists()) {
				// file does not exist, write default to path
				config = Config.DEFAULT;
				Files.write(configPath, gson.toJson(config).getBytes());
			}
			config = gson.fromJson(new String(Files.readAllBytes(configPath)), Config.class);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		if (config == null)
			throw new RuntimeException("Error parsing file");
		return config;
	}

	/*
	 * logger object, printing to System.in and System.out
	 */
	protected final ThreadLogger		logger	= new ThreadLogger(System.in, System.out);

	/*
	 * running flag, threads use this to figure out when to turn off
	 */
	@Expose
	private boolean						running	= true;

	/*
	 * The currently waiting clients
	 */
	@Expose
	protected Deque<ClientConnection>	clients	= new LinkedBlockingDeque<>();

	/*
	 * The trackers configuration
	 */
	@Expose
	public final Config					config;
	/*
	 * the socket we're listening on
	 */
	@Expose
	protected ServerSocket				socket	= null;

	/*
	 * Listens on the socket, waiting for a connection, passing those
	 * connections to their own ClientConnectionThreads
	 */
	private final Thread				listenThread;

	/*
	 * Waits for there to be two or more clients connected, then pops them, and
	 * sends their connection info to each other
	 */
	private final Thread				matchThread;

	/*
	 * Initialize new Tracker with given configuration
	 */
	public Tracker(Config config) {
		this.config = config;
		try {
			socket = new ServerSocket(config.port);
		} catch (IOException ioe) {
			throw new RuntimeException("There was an error openning the socket", ioe);
		}
		// initialize local threads
		matchThread = new MatchThread();
		listenThread = new ListenThread();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.AutoCloseable#close() Close the Tracker, free it's
	 * resources gracefully
	 */
	@Override
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error shutting down", e);
		}
	}

	/*
	 * whether the Tracker is still running
	 */
	public boolean isRunning() {
		return running;
	}

	/*
	 * start the listener thread and the match thread, then enter an interactive
	 * prompt
	 */
	public void run() {
		// initalize threads and go to REPL
		matchThread.start();
		listenThread.start();
		System.out.println("Tracker is running, enter 'help' for a list of tasks");
		try (Scanner scan = new Scanner(System.in)) {
			scan.useDelimiter(System.lineSeparator());
			// map of all the tasks, from String to the tasks. It could've been
			// an immutable map, but that'd have meant taking on a Guava
			// dependency, and that seems silly for just one collection
			Map<String, Task> tasks = new HashMap<>();
			tasks.put("help",
					new Task("Display this helpscreen",
							() -> tasks.entrySet().stream()
									.map((entry) -> entry.getKey() + "\t\t" + entry.getValue().docstring)
									.reduce((String s1, String s2) -> s1 + "\n" + s2).get()));
			tasks.put("exit", new Task("Kill the tracker gracefully", () -> {
				stop();
				return "Tracker stopped gracefully";
			}));
			tasks.put("list", new Task("List the current open connections", () -> clients.stream()
					.map(
							(client) -> client.toString()).reduce(
									(String s1, String s2) -> s1 + "\n" + s2).orElse("No connections")));
			while (isRunning()) {
				String line;
				try{
					line = scan.nextLine();
				}
				catch(NoSuchElementException nsee){
					System.out.println("Got EOF");
					line = "exit";
				}
				if(line.equals(""))
					continue;
				Task task = tasks.get(line);
				System.out.println(task == null ? "Sorry I don't know what that means" : task.call().get());
			}
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return gson.toJson(this, Tracker.class);
	}

	protected synchronized void stop() {
		if (running) {
			running = false;
		} else
			throw new RuntimeException("Tracker already stopped!!");
	}
}

/*
 * Thread for a given ClientConnection
 */
final class ClientConnectionThread extends Thread {
	/*
	 * Count used to give each connection a unique name, for logging purposes
	 */
	private static int	count	= 0;

	/*
	 * The threads connection summary, used for logging purposes
	 */
	private String		connection;

	/*
	 * Create a ClientConnectionThread from a ClientConnection
	 */
	public ClientConnectionThread(Tracker.ClientConnection conn) {
		super(conn);
		connection = conn.toString();
		setName("ClientConnectionThread#" + count);
		count++;
	}

	/*
	 * print the thread name and print the connection details
	 */
	@Override
	public String toString() {
		return getName() + ":" + connection;
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

@FunctionalInterface
interface TriFunction<A, B, C, R> {
	public R apply(A a, B b, C c);
}

/*
 * static methods that are useful
 */
class Util {
	public static String concat(List<String> strings) {
		return concat(strings, "");
	}

	public static String concat(List<String> strings, String delimeter) {
		return strings.stream().reduce((s1, s2) -> s1 + delimeter + s2).get();
	}

	private Util() {
		// we don't ever want to instantiate Util!
		throw new RuntimeException();
	}
}
