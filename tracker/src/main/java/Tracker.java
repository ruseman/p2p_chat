
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/*
 * Simple logger, with 3 levels, and it prints the thread that's invoking it
 */
class ThreadLogger{
	static enum MsgType{
		ERROR, NORMAL, WARN;
	}
	
	public ThreadLogger(InputStream in, OutputStream out){
		this(in, new PrintStream(out));
	}
	
	public final InputStream in;
	public final PrintStream out;
	
	public ThreadLogger(InputStream in, PrintStream out){
		this.in = in;
		this.out = out;
	}
	
	public void println(MsgType type, String msg){
		out.println("[" + Thread.currentThread().getName() + "] [" + type.name() + "] " + msg);
	}
	
	public void log(String msg){
		println(MsgType.NORMAL, msg);
	}
	
	public void error(String msg){
		println(MsgType.ERROR, msg);
	}
	
	public void warn(String msg){
		println(MsgType.WARN, msg);
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
 * Tracker for p2p chat system. It listens on a given port (8000 by default) for
 * connections from Clients. Clients then send a JSON message with the port
 * they're listening on. The Tracker pairs clients as they join, sending them
 * the IP and port of their mate, and the Clients connect as is appropriate.
 */
public class Tracker implements AutoCloseable {
	/*
	 * Holds the state of the connected client. Each ClientConnection is run as
	 * a thread, where it listens for information from the client (the listening
	 * port) and then adds itself to the Tracker's stack of awaiting clients
	 */
	class ClientConnection implements Runnable {
		/*
		 * summary of the client
		 */
		class ClientConnectionSummary{
			@Expose
			public int port;
			@Expose
			public String host;
			
			@Override
			public String toString(){
				return gson.toJson(this, ClientConnectionSummary.class);
			}
		}
		
		@Expose
		protected ClientConnectionSummary summary;
		
		protected Socket socket;

		/*
		 * instantiate new ClientConnection, from a given socket
		 */
		public ClientConnection(Socket socket) {
			this.socket = socket;
		}

		/*
		 * called by ClientConnection threads, should wait for message from
		 * client, then set the info, then add itself to the stack of waiting
		 * clients
		 */
		@Override
		public void run() {
			// TODO
		}

		@Override
		public String toString() {
			// TODO this is probably bad, should have a ClientConnectionSummary
			// class for holding the json data
			// it only needs to hold the clients IP and the port they're listening on
			return gson.toJson(this, ClientConnection.class);
		}
	}

	/*
	 * Configuration for a tracker, typically loaded from a .json file
	 */
	public static class Config {
		/*
		 * The port to listen for
		 */
		@Expose
		public int port;
	}

	/*
	 * path to config file TODO should read from argument instead
	 */
	public static final String	CONFIG_PATH		= "tracker.json";

	/*
	 * Default configuration object
	 */
	public static final Config	DEFAULT_CONFIG;
	/*
	 * default port to connect to TODO move defaults to a .json file in the jar
	 */
	public static final int		DEFAULT_PORT	= 8000;
	/*
	 * GSON instance, used throughout the entire program GsonBuilder used, so in
	 * order for Gson to use a field, it needs the @Expose annotation
	 */
	protected static Gson		gson			= new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	static {
		DEFAULT_CONFIG = new Config();
		DEFAULT_CONFIG.port = DEFAULT_PORT;
	}

	/*
	 * Load Config from file, writing default to file if it does not exist
	 */
	protected static Config getConfig(Path configPath) {
		Config config = null;
		try {
			if (!configPath.toFile().exists()) {
				// file does not exist, write default to path
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
	protected final ThreadLogger logger = new ThreadLogger(System.in, System.out);
	
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
	 * The currently waiting clients
	 */
	@Expose
	protected Deque<ClientConnection>	clients	= new LinkedBlockingDeque<>();

	/*
	 * The trackers configuration
	 */
	@Expose
	public final Config						config;


	/*
	 * running flag, threads use this to figure out when to turn off
	 */
	@Expose
	protected boolean						running	= true;

	/*
	 * the socket we're listening on
	 */
	@Expose
	protected ServerSocket					socket	= null;

	/*
	 * Initialize new Tracker with given configuration
	 */
	public Tracker(Config config) {
		this.config = config;
		try {
			socket = new ServerSocket();
		} catch (IOException ioe) {
			throw new RuntimeException("There was an error openning the socket", ioe);
		}
	}
	
	/*
	 * Listens on the socket, waiting for a connection, passing those connections to their own ClientConnectionThreads
	 */
	protected final Thread listenThread = new Thread(()->{
		ClientConnection conn;
		ClientConnectionThread cct;
		Socket clientSocket;
		logger.log("Starting to listen...");
		while(running){
			try{
				clientSocket = socket.accept();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
				continue;
			}
			conn = new ClientConnection(clientSocket);
			cct = new ClientConnectionThread(conn);
			cct.start();
		}
	});
	
	/*
	 * Waits for there to be two or more clients connected, then pops them, and sends their connection info to each other
	 */
	protected final Thread matchThread = new Thread(()->{
		BiFunction<ClientConnection, ClientConnection, Thread> sendThread = (target, remote)->{
			return new Thread(()->{
				// TODO write the code to send the remote's data to the target
			});
		};
		ClientConnection cc1, cc2;
		while(running){
			if(clients.size() >= 2){
				// aren't thread-safe collections a magical thing!
				cc1 = clients.pop();
				cc2 = clients.pop();
				// functional interfaces ftw!
				sendThread.apply(cc1, cc2).start();
				sendThread.apply(cc2, cc1).start();
			}
		}
	});

	/*
	 * (non-Javadoc)
	 * @see java.lang.AutoCloseable#close()
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
	 * start the listener thread and the match thread, then enter an interactive
	 * prompt
	 */
	public void run() {
		// initalize threads and go to REPL
		matchThread.start();
		listenThread.start();
		System.out.println("Tracker is running, press C-c to quit");
		while (running) {
			// TODO repl should have access to commands like quit, list, status,
			// etc
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return gson.toJson(this, Tracker.class);
	}
}
