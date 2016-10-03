package p2p.server.task;

/*
 * Task for the REPL, using a given Runnable object
 */
public class SimpleTask extends Task {
	public SimpleTask(String helpString, Runnable run) {
		super(helpString, run);
	}
}