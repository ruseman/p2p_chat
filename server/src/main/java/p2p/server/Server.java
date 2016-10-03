package p2p.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import p2p.common.Logger;
import p2p.common.RemoteClientConfiguration;

public class Server implements AutoCloseable {

	protected class SimpleTask extends Task {
		private Runnable runnable;

		public SimpleTask(String docstring, Runnable run) {
			super(docstring);
			runnable = run;
		}

		@Override
		public void run() {
			runnable.run();
		}
	}

	protected class Task implements Runnable {
		public final String	helpString;

		private Runnable	runnable;

		/**
		 * Construct a new task from a help string and a runnable
		 *
		 * @param helpString
		 *            string of help text
		 * @param runnable
		 *            runnable
		 */
		public Task(String helpString, Runnable runnable) {
			this(helpString);
			this.runnable = runnable;
		}

		/**
		 * Construct a new task from a help string and a supplier The output of
		 * the supplier is printed to System.out
		 *
		 * @param helpString
		 *            help string
		 * @param supplier
		 *            supplier
		 */
		public Task(String helpString, Supplier<String> supplier) {
			this(helpString, () -> {
				System.out.println(supplier.get());
			});
		}

		protected Task(String helpString) {
			this.helpString = helpString;
		}

		@Override
		public void run() {
			runnable.run();
		}
	}

	private class ListenRunnable implements Runnable {
		@Override
		public void run() {
			Client client;
			Socket socket;
			while (isRunning()) {
				try {
					socket = Server.this.socket.accept();
					client = new Client(socket);
				} catch (IOException se) {
					throw new RuntimeException(se);
				}
				if (client != null) {
					clients.add(client);
				}
			}
		}
	}

	private class MatchRunnable implements Runnable {
		@Override
		public void run() {
			Client c1, c2;
			int pairs = 0;
			while (isRunning()) {
				if (clients.size() >= 2) {
					c1 = clients.remove();
					c2 = clients.remove();

					Thread pairThread = new Thread(new PairRunnable(c1, c2), "PairThread#" + pairs);
					pairThread.start();
					pairs++;
				}
			}
		}
	}

	private class PairRunnable implements Runnable {
		private Client server, client;

		private PairRunnable(Client c1, Client c2) {
			server = c1;
			client = c2;
		}

		@Override
		public void run() {
			// The configuration used by the serving-client
			RemoteClientConfiguration serverConfig;
			try {
				int port;
				server.out.println("LISTEN");
				client.out.println("CALL");
				port = Integer.parseInt(server.in.readLine());
				serverConfig = new RemoteClientConfiguration(server.getInetAddress().getHostAddress(), port);
				client.out.println(serverConfig.toString());
			} catch (NumberFormatException nfe) {
				// the server failed at it's job, so we close the server
				// connection, and put the client back in the queue
				server.close();
				clients.add(client);
				throw new RuntimeException(nfe);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				client.close();
				server.close();
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

	private Queue<Client>				clients	= new ConcurrentLinkedQueue<>();

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

	/**
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

	/**
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
		tasks.put("list", new Task("list the currently waiting clients", () -> clients.stream().map(Client::toString)
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
