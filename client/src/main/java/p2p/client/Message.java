package p2p.client;

public class Message {
	public static enum Side {
		REMOTE, LOCAL, ERROR;

		@Override
		public String toString() {
			switch (this) {
			case REMOTE:
				return "[REMOTE] ";
			case LOCAL:
				return "[LOCAL]  ";
			case ERROR:
				return "[ERROR]  ";
			}
			throw new RuntimeException();
		}
	}

	public final String	text;
	public final Side	side;

	public Message(Exception e) {
		this(e.getMessage(), Side.ERROR);
	}

	public Message(String text, Side side) {
		this.text = text;
		this.side = side;
	}

	@Override
	public String toString() {
		return side + " " + text;
	}
}
