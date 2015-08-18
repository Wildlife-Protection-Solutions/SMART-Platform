package org.wcs.smart.connect;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.apache.commons.codec.binary.Base64;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.connect.api.ConnectUserClient;

public class SmartConnect {

	public final static String API_URL = "/api";
	
	private String url;
	private String username;
	private String password;
	
	private ResteasyClient client; 
	
	public SmartConnect(String url, String username, String password){
		this.url = url;
		this.username = username;
		this.password = password;
		
		client = new ResteasyClientBuilder().build();
		client.register(new AddAuthHeadersRequestFilter(username, password));
	}
	
	/**
	 * Validates the user/password/url associated.  Errors are logged
	 * 
	 * @return error message if not valid, otherwise null
	 */
	public String validateUser(){
		try{
			ResteasyWebTarget target = client.target(url + API_URL);
		
			ConnectUserClient simple = target.proxy(ConnectUserClient.class);
			String results = simple.getUser(username);
			System.out.println(results);
			return null;
		}catch (NotFoundException ex){
			String msg = MessageFormat.format("Could not connect to ({0}).", new Object[]{url});
			ConnectPlugIn.log(msg, ex);
			return msg;
		}catch (NotAuthorizedException ex){
			String msg = "Invalid SMART Connect username or password.";
			ConnectPlugIn.log(msg, ex);
			return msg;
		}catch(Throwable t){
			String msg = "Could not connect to server: " + t.getMessage();
			ConnectPlugIn.log(msg, t);
			return msg;
		}
		
	}
	
	
	public static class AddAuthHeadersRequestFilter implements ClientRequestFilter {

	    private final String username;
	    private final String password;

	    public AddAuthHeadersRequestFilter(String username, String password) {
	        this.username = username;
	        this.password = password;
	    }

	    @Override
	    public void filter(ClientRequestContext requestContext) throws IOException {
	        String token = username + ":" + password;
	        String base64Token = Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8));
	        requestContext.getHeaders().add("Authorization", "Basic " + base64Token);
	    }
	}
	
	public static void processException(Throwable ex){
		
	}
}
