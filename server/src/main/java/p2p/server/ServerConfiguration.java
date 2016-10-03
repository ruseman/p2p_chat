package p2p.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/*
 * Configuration options for a server
 */
public class ServerConfiguration {
	private static final Gson				gson	= new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	public static final ServerConfiguration	DEFAULT	= new ServerConfiguration(8000);

	public static ServerConfiguration fromJson(String json) {
		return gson.fromJson(json, ServerConfiguration.class);
	}

	@Expose
	public int port;

	private ServerConfiguration(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return gson.toJson(this, ServerConfiguration.class);
	}
}
