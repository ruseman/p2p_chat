package p2p.client;

import p2p.common.Host;

import static javax.swing.JOptionPane.showConfirmDialog;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class ClientApp {
	public class ExceptionHandler implements UncaughtExceptionHandler{
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			String msg = "Error in Thread " + t.getName() + ": " + e.getMessage() + "\nConsult log for stacktrace";
			JOptionPane.showMessageDialog(window, msg, "Critical Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void run(){
		client = new Client(getServer(), exceptionHandler);
		// TODO create a gui with an input text field, and a field to display messages
	}
	
	public ClientApp(){
		Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
	}
	
	private UncaughtExceptionHandler exceptionHandler = new ExceptionHandler();
	
	private JFrame window = null;
	private Client client;
	
	private Host server;
	
	public Host getServer(){
		JTextArea hostnameTextArea, portTextArea;
		JPanel portPanel, hostnamePanel, inputPanel;
		int port;
		String address;
		if(server == null){
			hostnameTextArea = new JTextArea();
			portTextArea = new JTextArea();
			
			portPanel = new JPanel(new BorderLayout());
			portPanel.add(new JLabel("Port #: "), BorderLayout.NORTH);
			portPanel.add(portTextArea, BorderLayout.CENTER);
			
			hostnamePanel = new JPanel(new BorderLayout());
			hostnamePanel.add(new JLabel("Hostname: "), BorderLayout.NORTH);
			hostnamePanel.add(hostnameTextArea, BorderLayout.CENTER);
			
			inputPanel = new JPanel(new GridLayout());
			inputPanel.add(hostnamePanel);
			inputPanel.add(portPanel);
			
			JOptionPane.showMessageDialog(window, inputPanel);
			try{
				port = Integer.parseInt(portTextArea.getText());
			}
			catch(NumberFormatException nfe){
				JOptionPane.showMessageDialog(window, "Invalid input for port", "Error!", JOptionPane.ERROR_MESSAGE);
				throw nfe;
			}
			address = hostnameTextArea.getText();
			server = new Host(address, port);
		}
		return server;
	}
	
	public static void main(String[] args){
		ClientApp clientApp = new ClientApp();
		clientApp.run();
	}
}
