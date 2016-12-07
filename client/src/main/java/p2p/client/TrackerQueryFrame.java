package p2p.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import p2p.common.Remote;

public class TrackerQueryFrame extends JFrame implements Supplier<Remote> {
	private enum State {
		OKAY, QUIT, WAIT;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -8572297818901461758L;

	public static void main(String[] args) {
		TrackerQueryFrame query = new TrackerQueryFrame();
		System.out.println(query.get());
	}

	private volatile State	state	= State.WAIT;

	private JTextField		fieldAddress, fieldPort;

	private JLabel			labelPrompt;

	public TrackerQueryFrame() {
		JPanel inputPanel, buttonPanel, portPanel, addressPanel;
		JLabel labelPort, labelAddress;
		JButton okayButton, cancelButton;
		Container cont = getContentPane();

		cont.setLayout(new BorderLayout());

		inputPanel = new JPanel(new BorderLayout());
		buttonPanel = new JPanel(new BorderLayout());
		portPanel = new JPanel(new BorderLayout());
		addressPanel = new JPanel(new BorderLayout());

		okayButton = new JButton("Okay");
		cancelButton = new JButton("Cancel");

		fieldAddress = new JTextField(8);
		fieldPort = new JTextField(8);
		labelAddress = new JLabel("Address", SwingConstants.CENTER);
		labelPort = new JLabel("Port", SwingConstants.CENTER);
		labelPrompt = new JLabel("Please enter the tracker", SwingConstants.CENTER);

		portPanel.add(fieldPort, BorderLayout.EAST);
		portPanel.add(labelPort, BorderLayout.WEST);

		addressPanel.add(fieldAddress, BorderLayout.EAST);
		addressPanel.add(labelAddress, BorderLayout.WEST);

		inputPanel.add(portPanel, BorderLayout.SOUTH);
		inputPanel.add(addressPanel, BorderLayout.NORTH);

		buttonPanel.add(okayButton, BorderLayout.EAST);
		buttonPanel.add(cancelButton, BorderLayout.WEST);

		cont.add(inputPanel, BorderLayout.CENTER);
		cont.add(labelPrompt, BorderLayout.NORTH);
		cont.add(buttonPanel, BorderLayout.SOUTH);

		okayButton.addActionListener((ae) -> {
			state = State.OKAY;
		});

		cancelButton.addActionListener((ae) -> {
			state = State.QUIT;
		});

		pack();
		setTitle("Tracker Query");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setResizable(false);
	}

	@Override
	public Remote get() {
		String address;
		int port;
		Remote host;

		setVisible(true);

		while (true) {
			while (state == State.WAIT) {
				;
			}

			if (state == State.QUIT) {
				host = null;
				break;
			}

			try {
				address = fieldAddress.getText();
				port = Integer.parseInt(fieldPort.getText());
				host = new Remote(address, port);
			} catch (RuntimeException re) {
				labelPrompt.setText("Invalid input");
				continue;
			}
			break;
		}
		setVisible(false);
		dispose();
		return host;
	}

}
