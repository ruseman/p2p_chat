package p2p.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import p2p.common.RemoteConnection;
import p2p.common.Logger;
import p2p.common.Remote;

public class Client extends Thread implements AutoCloseable {
	/*
	 * listen for messages from remote copy messages to the hist
	 */
	protected class ListenThread extends Thread {
		public ListenThread() {
			super("ListenThread");
			setUncaughtExceptionHandler(handler);
		}

		@Override
		public void run() {
			String line;
			while (running) {
				try {
					line = foreignClient.in.readLine();
					hist.add(new Message(line, Message.Side.REMOTE));
				} catch (IOException ioe) {
					hist.add(new Message(ioe));
				}
			}
		}
	}

	/*
	 * Send messages to remote from outgoing copy messages to the hist
	 */
	protected class SendThread extends Thread {
		public SendThread() {
			super("SendThread");
			setUncaughtExceptionHandler(handler);
		}

		@Override
		public void run() {
			String line;
			while (running) {
				while (!outgoing.isEmpty()) {
					line = outgoing.remove();
					foreignClient.out.println(line);
					hist.add(new Message(line, Message.Side.LOCAL));
				}
			}
		}
	}

	private class RemoteCaller implements RemoteHandler {
		private RemoteConnection server;

		private RemoteCaller(RemoteConnection server) {
			this.server = server;
		}

		@Override
		public void close() {
			// stub
		}

		/*
		 * wait for the server to send it a line with the Host info, then
		 * connect to that remote returning a remote object from that socket
		 */
		@Override
		public RemoteConnection get() {
			String line;
			Remote host;
			try {
				line = server.in.readLine();
				host = new Remote(line);
				return new RemoteConnection(host);
			} catch (IOException | RuntimeException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public boolean hasForeignClient(){
		return foreignClient != null;
	}

	private interface RemoteHandler extends Supplier<RemoteConnection>, Closeable {@Override public default void close() {}}
	

	private class RemoteListener implements RemoteHandler {
		private ServerSocket	listen;
		private RemoteConnection			server;

		private RemoteListener(RemoteConnection server) {
			this.server = server;
			listen = null;
		}

		@Override
		public void close() {
			try {
				if (listen != null) {
					listen.close();
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}

		/*
		 * open a serversocket, tell the tracker the address it's listening on
		 * then accept the first socket it gets and return a Remote from that
		 * socket
		 */
		@Override
		public RemoteConnection get() {
			Socket foreignClient;
			try {
				listen = new ServerSocket(0);
				logger.info("Listening on port #" + listen.getLocalPort());
				logger.info("Sending port # to server");
				server.out.println(listen.getLocalPort());
				foreignClient = listen.accept();
				return new RemoteConnection(foreignClient);
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	public final Logger						logger			= new Logger(System.out);

	private volatile boolean				ready			= false;

	private RemoteConnection							server			= null;

	private volatile RemoteConnection							foreignClient	= null;
	private final Remote						tracker;

	private final UncaughtExceptionHandler	handler;

	/*
	 * record of old messages, in order, for printing
	 */
	private List<Message>					hist			= new CopyOnWriteArrayList<>();

	private final Thread					sendThread, listenThread;

	/*
	 * queue of messages to send to the remote
	 */
	private Queue<String>					outgoing		= new ConcurrentLinkedQueue<>();

	private volatile boolean				running			= true;

	public Client(Remote tracker) {
		this(tracker, null);
	}

	public Client(Remote tracker, UncaughtExceptionHandler handler) {
		super("ClientThread");
		this.tracker = tracker;
		this.handler = handler;
		sendThread = new SendThread();
		listenThread = new ListenThread();
		if (handler != null) {
			setUncaughtExceptionHandler(handler);
			sendThread.setUncaughtExceptionHandler(handler);
			listenThread.setUncaughtExceptionHandler(handler);
		}
	}

	@Override
	public void close() {
		server.close();
	}

	/*
	 * return a copy of the hist list
	 */
	public List<Message> getHist() {
		return hist;
	}

	public boolean ready() {
		return ready;
	}

	public synchronized void requestStop() {
		running = false;
	}

	@Override
	public void run() {
		RemoteHandler remoteHandler = null;
		String mode;
		try {
			logger.info("Client is starting...");
			logger.info("Connecting to server...");
			server = new RemoteConnection(tracker);
			logger.info("Server connection established: " + server.toString());
			mode = server.in.readLine();
			logger.info("Got mode " + mode + " from server");

			if (mode.equals("CALL")) {
				remoteHandler = new RemoteCaller(server);
			} else if (mode.equals("LISTEN")) {
				remoteHandler = new RemoteListener(server);
			} else
				throw new RuntimeException("Invalid mode string from server: " + mode);

			foreignClient = remoteHandler.get();
			logger.info("Got foreign client connection: " + foreignClient.toString());
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} finally {
			if (remoteHandler != null) {
				remoteHandler.close();
			}
		}

		sendThread.start();
		listenThread.start();
		ready = true;
	}

	public void sendMessage(String message) {
		outgoing.add(message);
	}

}
