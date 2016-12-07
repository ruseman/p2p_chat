package p2p.server;

import java.util.function.Supplier;

/**
 * This is a Task class for the REPL, essentially a runnable object and a
 * helpstring
 */
public class Task implements Runnable {
	/**
	 * The help string
	 */
	public final String		helpString;

	/**
	 * The runnable object
	 */
	private final Runnable	runnable;

	/**
	 * Construct a new Task object, from a given helpstring and a runnable
	 * object
	 *
	 * @param helpString
	 * @param runnable
	 */
	public Task(String helpString, Runnable runnable) {
		this.helpString = helpString;
		this.runnable = runnable;
	}

	/**
	 * Construct a new Task object, from a given helpstring and a supplier
	 * object
	 *
	 * @param helpString
	 * @param supplier
	 */
	public Task(String helpString, Supplier<String> supplier) {
		this(helpString, () -> {
			System.out.println(supplier.get());
		});
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		runnable.run();
	}
}