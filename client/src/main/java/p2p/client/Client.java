package p2p.client;

import java.net.ServerSocket;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import p2p.client.swing.ClientFrame;

public class Client implements AutoCloseable {
	public class MatchServer {
		@Expose
		public String	host;
		@Expose
		public Integer	port;

		public MatchServer(String host, Integer port) {
			this.host = host;
			this.port = port;
		}

		@Override
		public String toString() {
			return gson.toJson(this);
		}
	}

	public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	public static void main(String[] args) {
		try (Client client = new Client()) {
			client.run();
		}
	}

	protected ClientFrame	frame;

	protected ServerSocket	socket;

	public Client() {

	}

	@Override
	public void close() {

	}

	/**
	 * @return the MatchServer or null if the user wants to quit
	 */
	public MatchServer getTrackerDialog() {
		JTextField hostField = new JTextField();
		JTextField portField = new JTextField();
		JComponent[] inputs = new JComponent[] { new JLabel("Host"), hostField, new JLabel("Port"), portField };
		String alert = null;
		Supplier<Integer> confirmDialog = () -> JOptionPane.showConfirmDialog(frame, inputs, "Server configuration",
				JOptionPane.PLAIN_MESSAGE);
		do {
			int result = confirmDialog.get();
			switch (result) {
			case JOptionPane.CLOSED_OPTION:

				return null;
			case JOptionPane.OK_OPTION:
				try {
					return new MatchServer(hostField.getText(), Integer.parseInt(portField.getText()));
				} catch (Exception e) {
					if (hostField.getText().isEmpty() || portField.getText().isEmpty()) {
						alert = "Please provide a matchs erver host and port";
					} else {
						alert = "Malformed input";
					}
				}
				break;
			default:
				alert = "No match server entered";
				break;
			}

			if (alert != null) {
				JOptionPane.showMessageDialog(frame, alert, "Alert", JOptionPane.ERROR_MESSAGE);
			}
		} while (true);
	}

	private void run() {
		System.out.println(getTrackerDialog());
	}
}
