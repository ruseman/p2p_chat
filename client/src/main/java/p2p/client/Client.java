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

import p2p.common.Host;
import p2p.common.Logger;
import p2p.common.Remote;

public class Client extends Thread implements AutoCloseable{
	private interface RemoteHandler extends Supplier<Remote>, Closeable{
		public default void close(){}
	}
	
	private class RemoteListener implements RemoteHandler{
		private ServerSocket listen;
		private Remote server;
		public void close(){
			try{
				if(listen != null)
					listen.close();
			}
			catch(IOException ioe){
				throw new RuntimeException(ioe);
			}
		}
		
		/* 
		 * open a serversocket, tell the tracker the address it's listening on 
		 * then accept the first socket it gets and return a Remote from that socket
		 */
		public Remote get(){
			Socket foreignClient;
			try{
				listen = new ServerSocket(0);
				logger.info("Listening on port #" + listen.getLocalPort());
				logger.info("Sending port # to server");
				server.out.println(listen.getLocalPort());
				foreignClient = listen.accept();
				return new Remote(foreignClient);
			}
			catch(IOException ioe){
				throw new RuntimeException(ioe);
			}
		}
		
		private RemoteListener(Remote server){
			this.server = server;
			listen = null;
		}
	}
	
	private class RemoteCaller implements RemoteHandler{
		private Remote server;

		private RemoteCaller(Remote server){
			this.server = server;
		}
		
		public void close(){
			// stub
		}
		
		/*
		 * wait for the server to send it a line with the Host info, then connect to that remote
		 * returning a remote object from that socket
		 */
		public Remote get(){
			String line;
			Host host;
			try{
				line = server.in.readLine();
				host = Host.fromJson(line);
				return new Remote(host);
			}
			catch(IOException | RuntimeException e){
				throw new RuntimeException(e);
			}
		}
	}
	
	public final Logger logger = new Logger(System.out);
	
	/*
	 * listen for messages from remote copy messages to the hist
	 */
	protected class ListenThread extends Thread {
		public ListenThread() {
			super("ListenThread");
			this.setUncaughtExceptionHandler(handler);
		}

		@Override
		public void run() {
			String line;
			while(running){
				try{
					line = foreignClient.in.readLine();
					hist.add(new Message(line, Message.Side.REMOTE));
				}
				catch(IOException ioe){
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
			this.setUncaughtExceptionHandler(handler);
		}

		@Override
		public void run() {
			String line;
			while(running){
				while(!outgoing.isEmpty()){
					line = outgoing.remove();
					foreignClient.out.println(line);
				}
			}
		}
	}
	
	private Remote server = null;
	private Remote foreignClient = null;
	
	public void run(){
		RemoteHandler remoteHandler = null;
		String mode;
		try{
			logger.info("Client is starting...");
			logger.info("Connecting to server...");
			server = new Remote(tracker);
			logger.info("Server connection established: " + server.toString());
			mode = server.in.readLine();
			logger.info("Got mode " + mode + " from server");
			
			if(mode.equals("CALL"))
				remoteHandler = new RemoteCaller(server);
			else if (mode.equals("LISTEN"))
				remoteHandler = new RemoteListener(server);
			else
				throw new RuntimeException("Invalid mode string from server: " + mode);
			
			foreignClient = remoteHandler.get();
			logger.info("Got foreign client connection: " + foreignClient.toString());
		}
		catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
		finally{
			if(remoteHandler != null)
				remoteHandler.close();
		}
		
		sendThread.start();
		listenThread.start();
	}

	private final Host						tracker;

	private final UncaughtExceptionHandler	handler;

	/*
	 * record of old messages, in order, for printing
	 */
	private List<Message>					hist		= new CopyOnWriteArrayList<>();
	
	private final Thread sendThread, listenThread;
	
	/*
	 * return a copy of the hist list
	 */
	public List<Message> getHist(){
		List<Message> list = new ArrayList<>();
		Collections.copy(list, hist);
		return list;
	}

	/*
	 * queue of messages to send to the remote
	 */
	private Queue<String>					outgoing	= new ConcurrentLinkedQueue<>();

	public Client(Host tracker) {
		this(tracker, null);
	}

	public Client(Host tracker, UncaughtExceptionHandler handler) {
		super("ClientThread");
		this.tracker = tracker;
		this.handler = handler;
		this.sendThread = new SendThread();
		this.listenThread = new ListenThread();
		if (handler != null) {
			setUncaughtExceptionHandler(handler);
			sendThread.setUncaughtExceptionHandler(handler);
			listenThread.setUncaughtExceptionHandler(handler);
		}
	}
	
	public synchronized void requestStop(){
		running = false;
	}
	
	private volatile boolean running = true;
	
	public void close(){
		server.close();
	}

}
