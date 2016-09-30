package p2p.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class Logger {
	public static enum Level {
		INFO, WARN, ERROR;
	}

	private final PrintStream out;

	public Logger(File file) throws FileNotFoundException {
		this(new PrintStream(new FileOutputStream(file)));
	}

	public Logger(OutputStream out) {
		this(new PrintStream(out));
	}

	public Logger(PrintStream out) {
		this.out = out;
	}

	public void error(String msg) {
		log(msg, Level.ERROR);
	}

	public void info(String msg) {
		log(msg, Level.INFO);
	}

	public void log(String msg, Level level) {
		out.println("[" + level.toString() + "][" + Thread.currentThread().getName() + "] " + msg);
	}

	public void warn(String msg) {
		log(msg, Level.WARN);
	}
}
