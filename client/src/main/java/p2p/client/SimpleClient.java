package p2p.client;

import p2p.common.Host;

public class SimpleClient {

	public static void main(String[] args) {
		try (Client client = new Client(new Host("localhost", 8000), (thread, ex) -> {
			ex.printStackTrace();
		})) {
			client.start();
		}
	}

}
