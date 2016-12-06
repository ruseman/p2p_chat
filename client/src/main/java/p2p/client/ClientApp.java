package p2p.client;

import p2p.common.Remote;

public class ClientApp {
	public static void main(String[] args) {
		Remote tracker;
		//TrackerQueryFrame tqf = new TrackerQueryFrame();
		//Remote tracker = tqf.get();
		
		tracker = new Remote("127.0.0.1", 8000);
		Client client;
		ClientFrame cframe;
		
		client = new Client(tracker);
		WaitingFrame wframe = new WaitingFrame(() -> client.ready(), () -> System.out.println("Cancel"),
				() -> System.out.println("Finish"));
		client.start();
		wframe.start();
		cframe = new ClientFrame(client);
		cframe.setVisible(true);
	}
}
