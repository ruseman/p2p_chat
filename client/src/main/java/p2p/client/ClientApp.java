package p2p.client;

import java.awt.EventQueue;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

public class ClientApp {

	private class Message {
		/*
		 * If it's remote or local
		 */
		private boolean	remote;

		private String	txt;

		private Message(String txt, boolean remote) {
			this.txt = txt;
			this.remote = remote;
		}

		@Override
		public String toString() {
			// extra spaces to align
			return (remote ? "[REMOTE] " : "[LOCAL]  ") + txt;
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				ClientApp window = new ClientApp();
				window.frmClient.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private JFrame							frmClient;

	private JTextField						textField;

	private Client							client;

	private ConcurrentLinkedQueue<Message>	history	= new ConcurrentLinkedQueue<>();

	private JTextPane						textPane;

	/**
	 * Create the application.
	 */
	public ClientApp() {
		initialize();
	}

	private void enterText() {
		String text = textField.getText();
		// check if text is empty
		if (text.isEmpty())
			return;
		// send our message through the client
		client.sendMessage(text);
		// add our message to our history
		history.add(new Message(text, false));
		// clear the text field
		textPane.setText("");
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmClient = new JFrame();
		frmClient.setTitle("Client");
		frmClient.setResizable(false);
		frmClient.setBounds(100, 100, 450, 300);
		frmClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmClient.getContentPane().setLayout(null);

		JButton btnEnter = new JButton("Enter");
		btnEnter.addActionListener((event) -> enterText());
		btnEnter.setBounds(364, 238, 72, 25);
		frmClient.getContentPane().add(btnEnter);

		textField = new JTextField();
		textField.addActionListener((event) -> enterText());
		textField.setBounds(12, 240, 340, 22);
		frmClient.getContentPane().add(textField);
		textField.setColumns(10);

		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setBounds(12, 12, 424, 214);
		frmClient.getContentPane().add(textPane);
	}
}
