package p2p.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class ServerInfo {
	private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	public static ServerInfo fromJson(String json) {
		return gson.fromJson(json, ServerInfo.class);
	}

	@Expose
	public final String		host;

	@Expose
	public final Integer	port;

	public ServerInfo(String host, Integer port) {
		super();
		this.host = host;
		this.port = port;
	}

	@Override
	public String toString() {
		return gson.toJson(this);
	}
}
