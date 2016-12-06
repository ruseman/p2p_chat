package p2p.client;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ClientFrame extends JFrame  {
	private static final long serialVersionUID = -8093297606128388287L;

	private JTextArea	textArea;
	private JTextField	textField;

	private Client		client;
	
	public ClientFrame(Client client) {
		Container cont;
		this.client = client;

		cont = getContentPane();
		cont.setLayout(new BorderLayout());

		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setColumns(40);
		textArea.setRows(10);
		textArea.setText("");
		cont.add(textArea, BorderLayout.CENTER);

		textField = new JTextField();
		textField.setText("");
		textField.setEditable(false);
		cont.add(textField, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		this.setSize(400, 400);
		
		this.setTitle("Client Frame");
		
		textField.addActionListener((event)->{
			String message = textField.getText();
			textField.setText("");
			client.sendMessage(message);
		});
		
		checkConnectThread.start();
		printThread.start();
	}
	
	private Thread printThread = new Thread(){
		public void run(){
			while (true){
				textArea.setText(client.getHist().stream().map((m)->m.text).reduce((a, b)->a + "\n" + b).orElse("Start talking!"));
			}
		}
	};
	
	private Thread checkConnectThread = new Thread(){
		public void run(){
			boolean done = false;
			while(!done){
				if(client.ready()){
					textField.setEditable(true);
					done = true;
				}
			}
		}
	};
}