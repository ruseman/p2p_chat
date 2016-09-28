
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/*
 * Thread for a given ClientConnection
 */
final class ClientConnectionThread extends Thread{
	private static int count = 0;
	private Tracker.ClientConnection connection;
	
	public ClientConnectionThread(Tracker.ClientConnection conn){
		super(conn);
		this.connection = conn;
		setName("ClientConnectionThread#" + count);
		count++;
	}
	
	/*
	 * Explicit override of run
	 */
	@Override
	public final void run(){
		super.run();
	}
	
	/*
	 * print the thread name and print the connection details
	 */
	public String toString(){
		return this.getName() + ":" + connection.toString();
	}
}

/*
 * Tracker for p2p chat system. It listens on a given port (8000 by default) for
 * connections from Clients. Clients then send a JSON message with the port
 * they're listening on. The Tracker pairs clients as they join, sending them
 * the IP and port of their mate, and the Clients connect as is appropriate.
 */
public class Tracker implements AutoCloseable{
	/*
	 * Holds the state of the connected client. Each ClientConnection is run as
	 * a thread, where it listens for information from the client (the listening
	 * port) and then adds itself to the Tracker's stack of awaiting clients
	 */
	class ClientConnection implements Runnable {
		protected Socket socket;
		/*
		 * instantiate new ClientConnection, from a given socket
		 */
		public ClientConnection(Socket socket) {
			this.socket = socket;
		}

		/*
		 * called by ClientConnection threads, should wait for message from client, then set the info, then add itself to the stack of waiting clients
		 */
		@Override
		public void run() {
			
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

	public static final String	CONFIG_PATH		= "tracker.json";
	public static final Config	DEFAULT_CONFIG;
	public static final int		DEFAULT_PORT	= 8000;
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
	public static void main(String... args) {
		try(Tracker tracker = new Tracker(getConfig(Paths.get(CONFIG_PATH)))){
			tracker.run();
		}
		catch(RuntimeException re){
			re.printStackTrace();
			System.exit(-1);
		}
	}

	public final Config		config;

	protected boolean running	= true;

	protected ServerSocket	socket	= null;

	/*
	 * Stack of client connections
	 * Using LinkedList instead of Stack or generic list so we can use both size and pop/push
	 */
	protected LinkedList<ClientConnection> clients = new LinkedList<>();
	
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
		// The ListenThread, waits for connections from the socket, then runs them as their own threads
		listenThread = new Thread(()->{
			ClientConnection conn;
			ClientConnectionThread cct;
			Socket clientSocket;
			log("Starting to listen");
			while(running){				
				try {
					clientSocket = socket.accept();
				} catch (IOException e) {
					throw new RuntimeException("Error accepting connection", e);
				}
				conn = new ClientConnection(clientSocket);
				cct = new ClientConnectionThread(conn);
				cct.start();
			}
		});
	}
	
	/*
	 * Log a String along with the current thread
	 */
	public static void log(String msg){
		System.out.println("[" + Thread.currentThread().getName() + "]:" + msg);
	}
	
	protected final Thread listenThread;

	/*
	 * Start the ListenThread, and then enter an interactive mode
	 */
	public void run() {
		listenThread.start();
		System.out.println("Tracker is running, press C-c to quit");
		while (running) {
			
		}
	}

	@Override
	public void close()  {
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error shutting down", e);
		}
	}
}
