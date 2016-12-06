package p2p.client;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ClientFrame extends JFrame  {
	private static final long serialVersionUID = -8093297606128388287L;

	private JTextArea	textArea;
	private JTextField	textField;

	private Client		client;
	
	private JScrollPane scrollPane;
	
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
		scrollPane = new JScrollPane(textArea);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		cont.add(scrollPane, BorderLayout.CENTER);

		textField = new JTextField();
		textField.setText("");
		textField.setEditable(false);
		textField.setColumns(40);
		cont.add(textField, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		this.pack();
		
		this.setTitle("Client Frame");
		
		textField.addActionListener((event)->{
			String message = textField.getText();
			textField.setText("");
			if(!message.isEmpty())
				client.sendMessage(message);
		});
		
		checkConnectThread.start();
		printThread.start();
	}
	
	private Thread printThread = new Thread(){
		public void run(){
			while (true){
				textArea.setText(client.getHist().stream().map(
						(m)->m.text == null ? null :
							(m.side == Message.Side.LOCAL ? "LOCAL:  " + m.text :
							(m.side == Message.Side.REMOTE ? "REMOTE: " + m.text : 
								"ERROR:  " + m.text))).reduce(
								(a, b)->b != null ? a + "\n" + b : a).orElse(ready ? "Start talking!" : "Waiting..."));
			}
		}
	};
	
	private boolean ready = false;
	
	private Thread checkConnectThread = new Thread(){
		public void run(){
			while(!ready){
				if(client.ready()){
					textField.setEditable(true);
					ready = true;
				}
			}
		}
	};
}