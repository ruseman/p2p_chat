package p2p.client;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

public class ClientFrame extends JFrame {
	private class CheckConnectThread extends Thread {
		private CheckConnectThread() {
			super("CheckConnectThread");
		}

		@Override
		public void run() {
			while (!ready) {
				if (client.ready()) {
					textField.setEditable(true);
					ready = true;
				}
			}
		}

	}

	private class PrintThread extends Thread {
		private PrintThread() {
			super("PrintThread");
		}

		@Override
		public void run() {
			String oldtext, newtext;
			while (true) {
				oldtext = textArea.getText();
				newtext = client.getHist().stream()
						.map((m) -> m.text == null ? null
								: m.side == Message.Side.LOCAL ? "LOCAL:  " + m.text
										: m.side == Message.Side.REMOTE ? "REMOTE: " + m.text : "ERROR:  " + m.text)
						.reduce((a, b) -> b != null ? a + "\n" + b : a).orElse(ready ? "Start talking!" : "Waiting...");
				textArea.setText(newtext);
				if (!newtext.equals(oldtext)) {
					textArea.setCaretPosition(textArea.getDocument().getLength());
				}

			}

		}
	}

	private static final long	serialVersionUID	= -8093297606128388287L;

	private JTextArea			textArea;

	private JTextField			textField;

	private Client				client;
	private JScrollPane			scrollPane;

	private Thread				printThread			= new PrintThread();

	private boolean				ready				= false;

	private Thread				checkConnectThread	= new CheckConnectThread();

	public ClientFrame(Client client) {
		Container cont;
		this.client = client;

		cont = getContentPane();
		cont.setLayout(new BorderLayout());

		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setColumns(40);
		textArea.setRows(10);
		textArea.setText("");
		textArea.setLineWrap(true);
		scrollPane = new JScrollPane(textArea);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		cont.add(scrollPane, BorderLayout.CENTER);

		textField = new JTextField();
		textField.setText("");
		textField.setEditable(false);
		textField.setColumns(40);
		cont.add(textField, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		pack();

		setTitle("Client Frame");
		setLocationRelativeTo(null);

		textField.addActionListener((event) -> {
			String message = textField.getText();
			textField.setText("");
			if (!message.isEmpty()) {
				client.sendMessage(message);
			}
		});

		checkConnectThread.start();
		printThread.start();
	}
}