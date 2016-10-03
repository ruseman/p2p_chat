package p2p.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * Container class for a host configuration, using Gson for serialization and deserialization
 */
public final class Host {
	private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	/**
	 * parse a JSON to a host object
	 * @param json the JSON string
	 * @return the Host object
	 */
	public static Host fromJson(String json) {
		return gson.fromJson(json, Host.class);
	}

	/**
	 * The host address
	 */
	@Expose
	public final String		host;

	/**
	 * the hosts port
	 */
	@Expose
	public final Integer	port;

	/**
	 * Construct a host from a given host address and port
	 * @param host
	 * @param port
	 */
	public Host(String host, Integer port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Convert a Host object to a JSON string
	 */
	@Override
	public String toString() {
		return gson.toJson(this);
	}
}
