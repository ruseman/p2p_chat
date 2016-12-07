package p2p.common;

public enum ClientMode {
	CALL, LISTEN;

	public static ClientMode get(String string) {
		switch (string) {
		case "LISTEN":
			return LISTEN;
		case "CALL":
			return CALL;
		}
		throw new RuntimeException();
	}

	@Override
	public String toString() {
		switch (this) {
		case LISTEN:
			return "LISTEN";
		case CALL:
			return "CALL";
		}
		throw new RuntimeException();
	}
}
