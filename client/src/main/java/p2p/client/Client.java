package p2p.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import p2p.common.Host;
import p2p.common.Logger;
import p2p.common.Remote;

public class Client extends Thread implements AutoCloseable {
	protected class ListenThread extends Thread {
		public ListenThread() {
			setName("ListenThread");
		}

		@Override
		public void run() {
			// listen for messages from foreignclient, adding lines as they
			// come to incomingMessages
			while(isRunning()){
				try {
					String line = foreignClient.in.readLine();
					incomingMessages.add(line);
				} catch (IOException e) {
					incomingMessages.add(e.getMessage());
				}				
			}
		}
	}

	protected class SendThread extends Thread {
		public SendThread() {
			setName("SendThread");
		}

		@Override
		public void run() {
			// go through the outgoingmessages sending them to
			// foreighClient as they go
			while(isRunning()){
				while(!outgoingMessages.isEmpty()){
					String line = outgoingMessages.remove();
					foreignClient.out.println(line);
				}
			}
		}
	}

	private class RemoteCaller extends RemoteHandler {

		@Override
		public void close() {
			// stub
		}

		@Override
		public Remote get() {
			// needs to wait for the server to send it a line with the
			// RemoteInfo in it, and then connect to that remote, returning a
			// Remote object from the socket
			try {
				String line = Client.this.server.in.readLine();
				Host host = Host.fromJson(line);
				return new Remote(host);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			catch(RuntimeException re){
				throw re;
			}
		}
	}

	private abstract class RemoteHandler implements Supplier<Remote>, Closeable {
		@Override
		public abstract void close();

		@Override
		public abstract Remote get();
	}

	private class RemoteListener extends RemoteHandler {
		private ServerSocket listen = null;

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

		@Override
		public Remote get() {
			Socket foreignClientPort;
			// open a serversocket, then tell the server the ip it's listening
			// on, then accept the first socket it gets and return a Remote with
			// that socket
			try {
				listen = new ServerSocket(0);
				logger.info("Listening on port #" + listen.getLocalPort());
				logger.info("sending port to server");
				server.out.println(listen.getLocalPort());
				foreignClientPort = listen.accept();
				return new Remote(foreignClientPort);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public final Thread				listenThread		= new ListenThread(), sendThread = new SendThread();

	private final Queue<String>	incomingMessages	= new ConcurrentLinkedQueue<>();

	private final Queue<String>	outgoingMessages	= new ConcurrentLinkedQueue<>();
	
	public void sendMessage(String msg){
		if(msg == null)
			return;
		else
			outgoingMessages.add(msg);
	}
	
	public String getMessage(String msg){
		if(incomingMessages.isEmpty())
			return null;
		else
			return incomingMessages.remove();
	}

	public final Host			serverInfo;

	public final Logger				logger				= new Logger(System.out);

	private boolean					running				= true;

	private Remote					server				= null, foreignClient = null;

	public Client(Host serverInfo, UncaughtExceptionHandler exceptionHandler) {
		this.serverInfo = serverInfo;
		setName("ClientThread");
		setUncaughtExceptionHandler(exceptionHandler);
	}

	@Override
	public void close() {

	}

	public boolean isRunning() {
		return running;
	}

	public synchronized void requestStop() {
		running = false;
	}

	@Override
	public void run() {
		RemoteHandler remoteHandler = null;
		try {
			logger.info("Client is starting...");
			logger.info("connecting to server...");
			server = new Remote(serverInfo);
			logger.info("Server connection established: " + server.toString());
			String mode = server.in.readLine();
			logger.info("Got mode " + mode + " from server");
			switch (mode) {
			case "CALL":
				remoteHandler = new RemoteCaller();
				break;
			case "LISTEN":
				remoteHandler = new RemoteListener();
				break;
			default:
				throw new RuntimeException("Invalid mode string from server");
			}
			foreignClient = remoteHandler.get();
			logger.info("Got foreign client connection" + foreignClient.toString());
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} finally {
			if (remoteHandler != null) {
				remoteHandler.close();
			}
		}
		// start delegate threads TODO
		sendThread.start();
		listenThread.start();
	}

	@Override
	public final void start() {
		super.start();
	}
}
