package p2p.client;

import p2p.common.Remote;

public class ClientApp {
	public static void main(String[] args) {
		Remote tracker;
		TrackerQueryFrame tqf;
		Client client;
		ClientFrame cframe;

		tqf = new TrackerQueryFrame();
		tracker = tqf.get();

		client = new Client(tracker);
		// wframe = new WaitingFrame(() -> client.ready(), () ->
		// System.out.println("Cancel"),
		// () -> System.out.println("Finish"));
		client.start();
		// wframe.start();
		cframe = new ClientFrame(client);
		cframe.setVisible(true);
	}
}
