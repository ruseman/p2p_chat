package p2p.server.task;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Supplier;

public class Task implements Runnable {
	public final String		helpString;
	private final Runnable	runnable;

	/*
	 * Construct a new task from a help string and a runnable
	 */
	public Task(String helpString, Runnable runnable) {
		this.helpString = helpString;
		this.runnable = runnable;
	}

	public Task(String helpString, Supplier<String> supplier) {
		this(helpString, supplier, System.out);
	}

	public Task(String helpString, Supplier<String> supplier, OutputStream out) {
		this(helpString, supplier, new PrintStream(out));
	}

	/*
	 * Construct a new task from a help string and a supplier The output of the
	 * supplier is printed to System.out
	 */
	public Task(String helpString, Supplier<String> supplier, PrintStream out) {
		this(helpString, () -> {
			out.println(supplier.get());
		});
	}

	@Override
	public final void run() {
		runnable.run();
	}
}