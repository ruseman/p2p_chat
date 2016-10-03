package p2p.client;

public class SimpleClient {

	public static void main(String[] args) {
		try (Client client = new Client(new ServerInfo("localhost", 8000), (thread, ex) -> {
			ex.printStackTrace();
			;
		})) {
			client.start();
		}
	}

}
