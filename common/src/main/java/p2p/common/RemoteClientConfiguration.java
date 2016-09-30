package p2p.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class RemoteClientConfiguration {
	public RemoteClientConfiguration(String host, Integer port){
		this.port = port;
		this.host = host;
	}
	
	@Expose
	public String host;
	@Expose
	public Integer port;
	
	private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	
	public String toString(){
		return gson.toJson(this);
	}
	
	public RemoteClientConfiguration parseJson(String json){
		return gson.fromJson(json, RemoteClientConfiguration.class);
	}
}
