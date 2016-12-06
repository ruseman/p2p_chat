package p2p.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class SwingExceptionHandler implements UncaughtExceptionHandler {
	public static class ExceptionFrame extends JFrame {
		public class ExceptionFrameActionListener implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				if (delegate != null) {
					delegate.uncaughtException(thread, throwable);
				}
				dispose();
			}
		}

		/**
		 *
		 */
		private static final long			serialVersionUID	= -6916808081467449748L;
		private JButton						okayButton;
		private UncaughtExceptionHandler	delegate;
		private Throwable					throwable;
		private Thread						thread;

		private JTextArea					text;

		public ExceptionFrame(Thread thread, Throwable throwable, UncaughtExceptionHandler delegate) {
			Container cont;
			String msg;

			this.throwable = throwable;
			this.thread = thread;
			this.delegate = delegate;

			cont = getContentPane();
			cont.setLayout(new BorderLayout());

			msg = "[" + throwable.getClass().getSimpleName() + "]";
			if (throwable.getMessage() != null) {
				msg += " " + throwable.getMessage();
			}
			for (StackTraceElement element : throwable.getStackTrace()) {
				msg += "\n" + element.toString();
			}

			text = new JTextArea(msg);
			text.setEditable(false);
			text.setLineWrap(false);
			text.setSize(80, 20);
			cont.add(text, BorderLayout.CENTER);

			okayButton = new JButton("Okay");
			okayButton.addActionListener(new ExceptionFrameActionListener());
			cont.add(okayButton, BorderLayout.SOUTH);

			setTitle("Error!");
			setAlwaysOnTop(true);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setLocationRelativeTo(null);
			pack();
		}
	}

	public static void main(String[] args) {
		RuntimeException re = new RuntimeException();
		SwingExceptionHandler handler = new SwingExceptionHandler();

		handler.uncaughtException(Thread.currentThread(), re);

		re = new RuntimeException("LOL BIG LONG MSG");
		handler.uncaughtException(Thread.currentThread(), re);

	}

	public SwingExceptionHandler() {

	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		JFrame frame = new ExceptionFrame(t, e, null);
		frame.setVisible(true);
	}
}
