package p2p.client;

import java.io.IOException;

public class Message {
	public final String text;
	public final Side side;
	public static enum Side{
		REMOTE,
		LOCAL,
		ERROR;
		
		public String toString(){
			switch(this){
			case REMOTE: return "[REMOTE] ";
			case LOCAL:  return "[LOCAL]  ";
			case ERROR:  return "[ERROR]  ";
			}
			throw new RuntimeException();
		}
	}
	
	public Message(String text, Side side){
		this.text = text;
		this.side = side;
	}
	
	public Message(Exception e) {
		this(e.getMessage(), Side.ERROR);
	}

	public String toString(){
		return side + " " + text;
	}
}
