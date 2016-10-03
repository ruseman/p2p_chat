package p2p.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import p2p.common.Host;
import p2p.common.Logger;
import p2p.common.Remote;
import p2p.server.task.Task;

public class Server implements AutoCloseable {

	private class ListenRunnable implements Runnable {
		@Override
		public void run() {
			Remote client;
			Socket socket;
			while (isRunning()) {
				try {
					socket = Server.this.socket.accept();
					client = new Remote(socket);
				} catch (IOException se) {
					throw new RuntimeException(se);
				}
				if (client != null) {
					clients.add(client);
				}
			}
		}
	}

	/*
	 * Runnable object to perform matches
	 * Only one of these should be run at a given time, it counts the number of pairs, and it creates the PairRunnables with the two pairs once there are 2 or more remotes waiting around
	 */
	private class MatchRunnable implements Runnable {
		@Override
		public void run() {
			logger.info("MatchRunnable is running");
			Remote c1, c2;
			int pairs = 0;
			while (isRunning()) {
				if (clients.size() >= 2) {
					c1 = clients.remove();
					c2 = clients.remove();
					logger.info("Paired clients " + c1.toString() + " and " + c2.toString());
					Thread pairThread = new Thread(new PairRunnable(c1, c2), "PairThread#" + pairs++);
					pairThread.start();
				}
			}
		}
	}
	
	/*
	 * Runnable object to do the pairing of two remote clients
	 */
	private class PairRunnable implements Runnable{
		private Remote listener, caller;
		
		private PairRunnable(Remote listener, Remote caller){
			this.listener = listener;
			this.caller = caller;
		}
		
		@Override
		public void run(){
			int port;
			Host host;
			try{
				// send the two remote clients their respective modes
				listener.out.println("LISTEN");
				caller.out.println("CALL");
				// get the port from the listener
				port = Integer.parseInt(listener.in.readLine());
				logger.info("got port " + port + " from listener");
				// place the listeners port and the listeners address into a new host configuration
				host = new Host(listener.getAddress(), port);
				// send the new host configuration to the caller
				caller.out.println(host.toString());
			}
			catch(NumberFormatException nfe){
				// the listener had some sort of major failure
				listener.close();
				clients.add(caller);
			}
			catch(Exception e){
				throw new RuntimeException(e);
			}
			finally{
				caller.close();
				listener.close();
			}
		}
	}

	/*
	 * Initialize new Server with given configuration
	 */
	public static void main(String[] args) {
		try (Server server = new Server(ServerConfiguration.DEFAULT)) {
			server.run();
		}
	}

	private Queue<Remote>				clients	= new ConcurrentLinkedQueue<>();

	/*
	 * The configuration being used by the server
	 */
	public final ServerConfiguration	config;

	/*
	 * The socket the server listens on
	 */
	private ServerSocket				socket;

	private Thread						matchThread, listenThread;

	private volatile boolean			running	= true;

	public final Logger					logger	= new Logger(System.out);

	/*
	 * The tasks runnable from the REPL
	 */
	private Map<String, Task>			tasks	= new HashMap<>();

	/*
	 * instantiate a new Server from a given ServerConfiguration
	 */
	public Server(ServerConfiguration config) {
		this.config = config;
		try {
			socket = new ServerSocket(config.port);
		} catch (IOException ioe) {
			throw new RuntimeException("Failed to open socket", ioe);
		}

		matchThread = new Thread(new MatchRunnable(), "MatchThread");
		listenThread = new Thread(new ListenRunnable(), "ListenThread");
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {
		try {
			socket.close();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public synchronized boolean isRunning() {
		return running;
	}

	/*
	 * Start the Server's threads, and then launch an interactive REPL
	 */
	public void run() {
		// start the threads
		matchThread.start();
		listenThread.start();
		// set thread exception handler
		Thread.currentThread().setUncaughtExceptionHandler((thread, exception) -> {
			exception.printStackTrace(System.err);
			System.out.println(
					"A critical error has occurred in the " + thread.getName() + " thread, check stderr for details");
			stop();
		});
		// the tasks are initialized here
		tasks.put("help",
				new Task("print this information",
						() -> tasks.entrySet().stream()
								.map((entry) -> entry.getKey() + "\t" + entry.getValue().helpString)
								.reduce((a, b) -> a + "\n" + b).get()));
		tasks.put("stop", new Task("stop the server politely", this::stop));
		tasks.put("list", new Task("list the currently waiting clients", () -> clients.stream().map(Remote::toString)
				.reduce((a, b) -> a + "\n" + b).orElse("No clients are connected")));
		try (Scanner scan = new Scanner(System.in)) {
			scan.useDelimiter(System.lineSeparator());
			System.out.println(
					"Starting interactive prompt, type 'help' for a list of options, or 'stop' to kill the server");
			while (isRunning()) {
				String line = scan.nextLine();
				if (line.isEmpty()) {
					continue;
				}
				Task task = tasks.get(line);
				if (task == null) {
					System.out.println("I don't know what that means");
				} else {
					task.run();
				}
			}
		}

	}

	/*
	 * Stop the server politely
	 */
	protected void stop() {
		logger.info("Server shutting down gracefully");
		if (!running)
			throw new IllegalStateException("Server already stopped");
		running = false;
	}
}
