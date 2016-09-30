package p2p.client.swing;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class ClientFrame extends JFrame {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 3943287640623594790L;
	private JPanel				contentPane;

	/**
	 * Create the frame.
	 */
	public ClientFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
	}

	public void start() {
		EventQueue.invokeLater(() -> {
			try {
				setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

}
