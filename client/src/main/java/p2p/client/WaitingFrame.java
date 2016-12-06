package p2p.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.BooleanSupplier;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A Frame that will show itself, while a given process is running.
 */
public class WaitingFrame extends JFrame implements Runnable, ActionListener {
	/**
	 *
	 */
	private static final long serialVersionUID = 7625914005628395682L;

	public static void main(String[] args) throws InterruptedException {
		WaitingFrame frame;

		frame = new WaitingFrame(() -> false, () -> System.out.println("Cancel"), () -> System.out.println("Finished"));

		frame.start();

		frame.thread.join();
		System.exit(0);
	}

	private Runnable			cancelCallback;
	private BooleanSupplier		waitingCallback;

	private Runnable			finishCallback;

	private JButton				cancelButton;

	private Thread				thread;

	private JLabel				label;
	private volatile boolean	running	= true;

	/**
	 * A WaitingFrame that will wait until a given BooleanSupplier returns false
	 *
	 * @param waitingCallback
	 *            waitingCallback
	 * @param cancelCallback
	 *            cancelCallback
	 * @param finishCallback
	 *            finishCallback
	 */
	public WaitingFrame(BooleanSupplier waitingCallback, Runnable cancelCallback, Runnable finishCallback) {
		Container cont;

		if (waitingCallback == null || waitingCallback == null || finishCallback == null)
			throw new NullPointerException();

		this.waitingCallback = waitingCallback;
		this.cancelCallback = cancelCallback;
		this.finishCallback = finishCallback;

		cont = getContentPane();
		cont.setLayout(new BorderLayout());

		label = new JLabel("Waiting...", SwingConstants.CENTER);
		label.setSize(100, 40);
		cont.add(label, BorderLayout.CENTER);

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cont.add(cancelButton, BorderLayout.SOUTH);

		thread = new Thread(this);

		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Constructs a WaitingFrame that will wait until a given thread is
	 * complete. Once the thread is dead, it'll run the finishCallback, or if
	 * the user chooses to cancel, it'll run the cancelCallback
	 *
	 * @param thread
	 *            the thread to wait for
	 * @param cancelCallback
	 *            the cancel callback
	 * @param finishCallback
	 *            the finish callback
	 */
	public WaitingFrame(Thread thread, Runnable cancelCallback, Runnable finishCallback) {
		this(thread::isAlive, cancelCallback, finishCallback);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		cancel();
	}

	@Override
	public void run() {
		while (running) {
			if (!waitingCallback.getAsBoolean()) {
				finish();
			}
		}
	}

	/**
	 * Start the waitingframe
	 */
	public void start() {
		setVisible(true);
		thread.start();
	}

	private void cancel() {
		setVisible(false);
		running = false;
		cancelCallback.run();
	}

	private void finish() {
		running = false;
		setVisible(false);
		finishCallback.run();
	}
}
